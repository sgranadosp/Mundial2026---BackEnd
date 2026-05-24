/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.FootballDataMatchItem;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Match.DataStatus;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.Match.Phase;
import co.edu.unbosque.mundial2026.model.Stadium;
import co.edu.unbosque.mundial2026.model.Team;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.StadiumRepository;
import co.edu.unbosque.mundial2026.repository.TeamRepository;

/**
 * Servicio que sincroniza datos del Mundial 2026 desde
 * <a href="https://docs.football-data.org/general/v4/index.html">football-data.org v4</a>
 * hacia la base de datos local del Mundial 2026 Hub.
 * <p>
 * <b>Política de sincronización:</b>
 * <ul>
 *   <li>Solo se sincronizan partidos de fase de grupos (la API permite
 *       filtrarlos directamente con {@code ?stage=GROUP_STAGE}).</li>
 *   <li>Para cada partido: se buscan/crean los dos {@link Team} y el
 *       {@link Stadium} por su identificador, y luego se hace upsert del
 *       {@link Match}.</li>
 *   <li>Si la API devuelve menos partidos de los esperados (72 para fase
 *       de grupos del Mundial 2026), se sincroniza lo que haya disponible
 *       sin inventar datos.</li>
 *   <li>La operación es idempotente: ejecutarla N veces deja el mismo estado
 *       en BD que ejecutarla una vez.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Diferencias clave con la integración previa (api-football.com):</b>
 * <ul>
 *   <li>El estadio en v4 viene como string plano ({@code venue}), no como
 *       objeto anidado con id. Se usa el nombre como clave de upsert.</li>
 *   <li>La etapa se filtra directamente en la URL, no hay que filtrar en cliente.</li>
 *   <li>Los grupos llegan con formato "GROUP_A", "GROUP_B", ..., "GROUP_L"
 *       (Mundial 2026 tiene 12 grupos).</li>
 * </ul>
 * </p>
 */
@Service
public class FootballApiSyncService {

    private static final Logger log = LoggerFactory.getLogger(FootballApiSyncService.class);

    /** Código de competición del Mundial en football-data.org. */
    private static final String WORLD_CUP_CODE = "WC";

    /** Valor del filtro de etapa para fase de grupos. */
    private static final String GROUP_STAGE_FILTER = "GROUP_STAGE";

    @Autowired
    private FootballApiClient apiClient;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private TeamRepository teamRepo;

    @Autowired
    private StadiumRepository stadiumRepo;

    /** Constructor por defecto requerido por Spring. */
    public FootballApiSyncService() {
    }

    /**
     * Sincroniza los partidos de fase de grupos del Mundial 2026.
     * <p>
     * Llama a la API filtrando por fase de grupos, mapea cada partido a
     * entidades JPA (Team, Stadium, Match) y los upsertea en BD. Devuelve
     * un reporte con cuántos se procesaron y cuántos quedaron en BD.
     * </p>
     *
     * @return Reporte con métricas del sync.
     */
    @Transactional
    public SyncReport syncGroupStageFixtures() {
        log.info("=== Iniciando sync de fase de grupos del Mundial 2026 ===");
        SyncReport report = new SyncReport();

        List<FootballDataMatchItem> matches = apiClient.getMatchesByCompetition(
                WORLD_CUP_CODE, GROUP_STAGE_FILTER);
        report.fetchedFromApi = matches.size();

        if (matches.isEmpty()) {
            log.warn("La API no devolvió ningún partido. ¿API key correcta? ¿Hay datos publicados?");
            return report;
        }

        // Caches in-memory por id externo para no martillar a la BD en cada partido.
        Map<Long, Team> teamsCache = new HashMap<>();
        Map<String, Stadium> stadiumsCache = new HashMap<>();

        for (FootballDataMatchItem item : matches) {
            // Filtro: equipos con ID válido (la API a veces manda placeholders)
            if (item.getHomeTeam() == null
                    || item.getAwayTeam() == null
                    || item.getHomeTeam().getId() == null
                    || item.getAwayTeam().getId() == null) {
                report.skippedNoTeams++;
                continue;
            }

            try {
                Team home = resolveTeam(item.getHomeTeam(), teamsCache);
                Team away = resolveTeam(item.getAwayTeam(), teamsCache);
                Stadium stadium = resolveStadium(item.getVenue(), stadiumsCache);

                upsertMatch(item, home, away, stadium);
                report.synced++;
            } catch (Exception e) {
                log.error("Error procesando partido id={}: {}",
                        item.getId(), e.getMessage());
                report.errors++;
            }
        }

        report.totalInDb = (int) matchRepo.count();
        log.info("=== Sync terminado: {} ===", report);
        return report;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Busca un equipo por su id externo o lo crea si no existe.
     * Si ya existe, actualiza nombre y escudo si cambiaron.
     */
    private Team resolveTeam(FootballDataMatchItem.TeamRef ref, Map<Long, Team> cache) {
        if (cache.containsKey(ref.getId())) {
            return cache.get(ref.getId());
        }
        Team team = teamRepo.findByExternalId(ref.getId())
                .orElseGet(() -> {
                    Team t = new Team();
                    t.setExternalId(ref.getId());
                    t.setName(ref.getName());
                    t.setShortName(ref.getShortName() != null ? ref.getShortName() : ref.getName());
                    t.setCrestUrl(ref.getCrest());
                    return teamRepo.save(t);
                });

        // Actualizamos campos por si cambiaron
        boolean dirty = false;
        if (ref.getName() != null && !ref.getName().equals(team.getName())) {
            team.setName(ref.getName());
            dirty = true;
        }
        if (ref.getCrest() != null && !ref.getCrest().equals(team.getCrestUrl())) {
            team.setCrestUrl(ref.getCrest());
            dirty = true;
        }
        if (dirty) {
            team = teamRepo.save(team);
        }
        cache.put(ref.getId(), team);
        return team;
    }

    /**
     * Busca un estadio por su nombre o lo crea si no existe.
     * <p>
     * En football-data.org v4, el venue viene como string plano (no como
     * objeto con id), así que usamos el nombre como clave de upsert.
     * Como no tenemos ciudad por separado, queda en null.
     * </p>
     */
    /**
     * Resuelve un estadio a partir del nombre de la venue que viene en el
     * payload de football-data.org. Estrategia:
     * <ul>
     *   <li>Si no hay nombre → null (el partido conservará el estadio
     *       previamente asignado por {@code OpenFootballSyncService}, si lo
     *       tiene).</li>
     *   <li>Si el nombre matchea un estadio existente → lo devuelve sin
     *       tocarlo (preserva latitud/longitud sincronizados por openfootball).</li>
     *   <li>Si NO matchea ningún estadio existente → devuelve null. NO crea
     *       estadios fantasma sin coordenadas; en su lugar deja el campo
     *       intacto en {@link #upsertMatch} para que el partido siga
     *       apuntando al estadio correcto si ya lo tenía. Si el partido es
     *       totalmente nuevo y no tiene estadio, el operador deberá correr
     *       el sync de venues para completarlo.</li>
     * </ul>
     */
    private Stadium resolveStadium(String venueName, Map<String, Stadium> cache) {
        if (venueName == null || venueName.isBlank()) return null;
        if (cache.containsKey(venueName)) {
            return cache.get(venueName);
        }
        Stadium stadium = stadiumRepo.findByName(venueName).orElse(null);
        if (stadium != null) {
            cache.put(venueName, stadium);
        } else {
            log.debug("[sync/fixtures] Venue '{}' no matchea ningún Stadium "
                    + "en BD; se conserva el estadio actual del partido. "
                    + "Ejecuta /admin/sync/venues si quieres completarlo.",
                    venueName);
        }
        return stadium;
    }

    /**
     * Hace upsert de un partido: crea si no existe, actualiza si ya está.
     */
    private void upsertMatch(FootballDataMatchItem item, Team home, Team away, Stadium stadium) {
        Long externalId = item.getId();
        Match match = matchRepo.findByExternalId(externalId).orElseGet(Match::new);

        match.setExternalId(externalId);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        // Solo sobrescribir el estadio si recibimos uno válido. Si stadium
        // viene null (football-data no envió venue para este partido o el
        // nombre no matcheó con ninguno en BD) preservamos el estadio
        // existente que probablemente fue asignado por OpenFootballSyncService
        // y tiene latitud/longitud completas.
        if (stadium != null) {
            match.setStadium(stadium);
        }
        match.setScheduledAt(parseUtcDate(item.getUtcDate()));
        match.setPhase(Phase.GROUP_STAGE);
        match.setStatus(mapStatus(item.getStatus()));
        match.setHomeScore(extractHomeScore(item));
        match.setAwayScore(extractAwayScore(item));
        match.setGroupName(extractGroupLetter(item.getGroup()));
        match.setMatchday(item.getMatchday());
        match.setDataStatus(DataStatus.CONFIRMED);

        matchRepo.save(match);
    }

    /**
     * Parsea una fecha ISO-8601 UTC (ej. "2026-06-11T20:00:00Z") a
     * LocalDateTime en UTC.
     */
    private LocalDateTime parseUtcDate(String iso) {
        if (iso == null) return null;
        try {
            return OffsetDateTime.parse(iso).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            log.warn("No se pudo parsear fecha '{}': {}", iso, e.getMessage());
            return null;
        }
    }

    /**
     * Mapea el estado de football-data.org al enum interno {@link MatchStatus}.
     * <p>
     * Estados de football-data.org v4: SCHEDULED, TIMED, IN_PLAY, PAUSED,
     * FINISHED, SUSPENDED, POSTPONED, CANCELLED, AWARDED.
     * </p>
     */
    private MatchStatus mapStatus(String status) {
        if (status == null) return MatchStatus.SCHEDULED;
        switch (status) {
            case "FINISHED":
            case "AWARDED":
                return MatchStatus.FINISHED;
            case "IN_PLAY":
            case "PAUSED":
                return MatchStatus.LIVE;
            case "POSTPONED":
                return MatchStatus.POSTPONED;
            case "SUSPENDED":
            case "CANCELLED":
                return MatchStatus.CANCELLED;
            case "SCHEDULED":
            case "TIMED":
            default:
                return MatchStatus.SCHEDULED;
        }
    }

    /**
     * Extrae los goles del equipo local del score completo del partido.
     * Devuelve null si el partido no inició aún.
     */
    private Integer extractHomeScore(FootballDataMatchItem item) {
        if (item.getScore() == null || item.getScore().getFullTime() == null) return null;
        return item.getScore().getFullTime().getHome();
    }

    /**
     * Extrae los goles del equipo visitante del score completo del partido.
     */
    private Integer extractAwayScore(FootballDataMatchItem item) {
        if (item.getScore() == null || item.getScore().getFullTime() == null) return null;
        return item.getScore().getFullTime().getAway();
    }

    /**
     * Extrae la letra del grupo desde el enum de la API.
     * Ej. "GROUP_A" → "A", "GROUP_L" → "L".
     */
    private String extractGroupLetter(String group) {
        if (group == null || !group.startsWith("GROUP_")) return null;
        return group.substring("GROUP_".length());
    }

    // =========================================================================
    // Reporte de sync (DTO simple para el controller)
    // =========================================================================

    /**
     * Reporte con métricas de una operación de sincronización.
     */
    public static class SyncReport {
        public int fetchedFromApi;
        public int synced;
        public int skippedNoTeams;
        public int errors;
        public int totalInDb;

        @Override
        public String toString() {
            return "SyncReport[fetched=" + fetchedFromApi
                    + ", synced=" + synced
                    + ", skippedNoTeams=" + skippedNoTeams
                    + ", errors=" + errors
                    + ", totalInDb=" + totalInDb + "]";
        }
    }
}