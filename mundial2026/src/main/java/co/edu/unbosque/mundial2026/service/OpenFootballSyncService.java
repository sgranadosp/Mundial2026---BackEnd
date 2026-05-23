/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.OpenFootballWorldCupResponse.OpenFootballMatch;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Stadium;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.StadiumRepository;

/**
 * Servicio que enriquece la información de estadios y asigna venues a los
 * partidos ya sincronizados desde football-data.org, usando como fuente el
 * dataset público <a href="https://github.com/openfootball/worldcup.json">
 * openfootball/worldcup.json</a>.
 * <p>
 * <b>Por qué existe este servicio:</b> el plan gratuito de football-data.org
 * no incluye el campo {@code venue} en los partidos. openfootball sí provee
 * la ciudad sede de cada partido del Mundial 2026, lo que nos permite
 * inferir el estadio (cada ciudad tiene una única sede del Mundial).
 * </p>
 * <p>
 * <b>Flujo en dos fases:</b>
 * <ol>
 *   <li><b>Fase A:</b> upsert de los 16 estadios oficiales con sus
 *       coordenadas y datos auxiliares (capacidad, timezone, etc.).</li>
 *   <li><b>Fase B:</b> recorre los partidos de openfootball y, para cada uno,
 *       busca el partido equivalente en BD (por nombres de equipos) y le
 *       asigna el {@link Stadium} correspondiente a su ciudad. NO crea
 *       partidos nuevos.</li>
 * </ol>
 * </p>
 * <p>
 * La operación es idempotente: ejecutarla N veces deja el mismo estado en BD.
 * </p>
 */
@Service
public class OpenFootballSyncService {

    private static final Logger log = LoggerFactory.getLogger(OpenFootballSyncService.class);

    @Autowired
    private OpenFootballClient openFootballClient;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private StadiumRepository stadiumRepo;

    /** Constructor por defecto requerido por Spring. */
    public OpenFootballSyncService() {
    }

    // =========================================================================
    // Datos de las 16 sedes oficiales (coordenadas verificadas con OSM + FIFA)
    // =========================================================================
    // Se hardcodean en código por dos razones:
    //   1. Son 16 datos estáticos que nunca cambian.
    //   2. openfootball solo provee la ciudad, no las coordenadas; cualquier
    //      otra API gratuita tampoco las trae completas y consistentes.
    // =========================================================================

    /**
     * Información estática de un estadio. Solo se usa internamente para
     * inicializar la BD desde el array {@link #VENUES}.
     */
    private static final class VenueData {
        final String name;
        final String city;
        final String country;
        final String timezone;
        final double latitude;
        final double longitude;
        final int capacity;

        VenueData(String name, String city, String country, String timezone,
                  double latitude, double longitude, int capacity) {
            this.name = name;
            this.city = city;
            this.country = country;
            this.timezone = timezone;
            this.latitude = latitude;
            this.longitude = longitude;
            this.capacity = capacity;
        }
    }

    /** Las 16 sedes oficiales del Mundial 2026. */
    private static final VenueData[] VENUES = new VenueData[] {
        // USA (11)
        new VenueData("Mercedes-Benz Stadium",   "Atlanta",         "USA",    "America/New_York",     33.7553,  -84.4006, 71000),
        new VenueData("Gillette Stadium",        "Foxborough",      "USA",    "America/New_York",     42.0909,  -71.2643, 65878),
        new VenueData("AT&T Stadium",            "Arlington",       "USA",    "America/Chicago",      32.7473,  -97.0945, 80000),
        new VenueData("NRG Stadium",             "Houston",         "USA",    "America/Chicago",      29.6847,  -95.4107, 72220),
        new VenueData("Arrowhead Stadium",       "Kansas City",     "USA",    "America/Chicago",      39.0489,  -94.4839, 76416),
        new VenueData("SoFi Stadium",            "Inglewood",       "USA",    "America/Los_Angeles",  33.9534, -118.3387, 70240),
        new VenueData("Hard Rock Stadium",       "Miami Gardens",   "USA",    "America/New_York",     25.9580,  -80.2389, 65326),
        new VenueData("MetLife Stadium",         "East Rutherford", "USA",    "America/New_York",     40.8135,  -74.0745, 82500),
        new VenueData("Lincoln Financial Field", "Philadelphia",    "USA",    "America/New_York",     39.9008,  -75.1675, 69596),
        new VenueData("Levi's Stadium",          "Santa Clara",     "USA",    "America/Los_Angeles",  37.4030, -121.9700, 68500),
        new VenueData("Lumen Field",             "Seattle",         "USA",    "America/Los_Angeles",  47.5952, -122.3316, 68740),
        // México (3)
        new VenueData("Estadio Azteca",          "Ciudad de México","Mexico", "America/Mexico_City",  19.3029,  -99.1505, 87523),
        new VenueData("Estadio Akron",           "Guadalajara",     "Mexico", "America/Mexico_City",  20.6819, -103.4625, 49850),
        new VenueData("Estadio BBVA",            "Guadalupe",       "Mexico", "America/Monterrey",    25.6692, -100.2444, 53500),
        // Canadá (2)
        new VenueData("BMO Field",               "Toronto",         "Canada", "America/Toronto",      43.6332,  -79.4185, 30000),
        new VenueData("BC Place",                "Vancouver",       "Canada", "America/Vancouver",    49.2767, -123.1119, 54500),
    };

    /**
     * Mapeo de los valores del campo {@code ground} de openfootball a los
     * nombres de estadios en BD.
     * <p>
     * openfootball usa nombres de ciudad como sede, no el nombre del estadio.
     * Algunos valores son ligeramente distintos a los nombres "city" que
     * tenemos guardados (ej. "Mexico City" vs "Ciudad de México") por eso
     * mantenemos un mapeo explícito en lugar de uno automático.
     * </p>
     */
    private static final Map<String, String> GROUND_TO_STADIUM_NAME = new HashMap<>();
    static {
        // USA — los grounds vienen como "Ciudad (Suburbio)" con paréntesis
        GROUND_TO_STADIUM_NAME.put("Atlanta",                              "Mercedes-Benz Stadium");
        GROUND_TO_STADIUM_NAME.put("Boston (Foxborough)",                  "Gillette Stadium");
        GROUND_TO_STADIUM_NAME.put("Dallas (Arlington)",                   "AT&T Stadium");
        GROUND_TO_STADIUM_NAME.put("Houston",                              "NRG Stadium");
        GROUND_TO_STADIUM_NAME.put("Kansas City",                          "Arrowhead Stadium");
        GROUND_TO_STADIUM_NAME.put("Los Angeles (Inglewood)",              "SoFi Stadium");
        GROUND_TO_STADIUM_NAME.put("Miami (Miami Gardens)",                "Hard Rock Stadium");
        GROUND_TO_STADIUM_NAME.put("New York/New Jersey (East Rutherford)","MetLife Stadium");
        GROUND_TO_STADIUM_NAME.put("Philadelphia",                         "Lincoln Financial Field");
        GROUND_TO_STADIUM_NAME.put("San Francisco Bay Area (Santa Clara)", "Levi's Stadium");
        GROUND_TO_STADIUM_NAME.put("Seattle",                              "Lumen Field");
        // México
        GROUND_TO_STADIUM_NAME.put("Mexico City",                          "Estadio Azteca");
        GROUND_TO_STADIUM_NAME.put("Guadalajara (Zapopan)",                "Estadio Akron");
        GROUND_TO_STADIUM_NAME.put("Monterrey (Guadalupe)",                "Estadio BBVA");
        // Canadá
        GROUND_TO_STADIUM_NAME.put("Toronto",                              "BMO Field");
        GROUND_TO_STADIUM_NAME.put("Vancouver",                            "BC Place");
    }

    /**
     * Mapeo de nombres de equipo: openfootball → nombre tal como está en tu BD
     * (que vino de football-data.org).
     * <p>
     * Los nombres confirmados con tu BD (48 equipos):
     * Mexico, South Africa, South Korea, Czechia, Canada, Bosnia-Herzegovina,
     * United States, Paraguay, Qatar, Switzerland, Brazil, Morocco, Haiti,
     * Scotland, Australia, Turkey, Germany, Curaçao, Netherlands, Japan,
     * Ivory Coast, Sweden, Spain, Cape Verde Islands, Belgium, Egypt,
     * Saudi Arabia, Uruguay, Iran, New Zealand, France, Senegal, Iraq, Norway,
     * Argentina, Algeria, Austria, Jordan, Portugal, Congo DR, Uzbekistan,
     * Colombia, England, Croatia, Ghana, Panama, Ecuador, Tunisia.
     * </p>
     */
    private static final Map<String, String> TEAM_ALIASES = new HashMap<>();
    static {
        TEAM_ALIASES.put("Czech Republic",        "Czechia");
        TEAM_ALIASES.put("USA",                   "United States");
        TEAM_ALIASES.put("Bosnia & Herzegovina",  "Bosnia-Herzegovina");
        TEAM_ALIASES.put("Cape Verde",            "Cape Verde Islands");
        TEAM_ALIASES.put("DR Congo",              "Congo DR");
    }

    // =========================================================================
    // Operación principal
    // =========================================================================

    /**
     * Ejecuta la sincronización completa: fase A (upsert de estadios) +
     * fase B (asignación de venues a partidos existentes).
     *
     * @return Reporte con métricas de la operación.
     */
    @Transactional
    public VenueSyncReport syncVenues() {
        log.info("=== Iniciando sync de venues desde openfootball ===");
        VenueSyncReport report = new VenueSyncReport();

        // ---- FASE A: upsert de los 16 estadios oficiales -------------------
        Map<String, Stadium> stadiumByName = new HashMap<>();
        for (VenueData v : VENUES) {
            Stadium s = stadiumRepo.findByName(v.name).orElseGet(() -> {
                Stadium nuevo = new Stadium();
                nuevo.setName(v.name);
                return nuevo;
            });
            boolean wasNew = (s.getId() == null);

            s.setCity(v.city);
            s.setCountry(v.country);
            s.setTimezone(v.timezone);
            s.setLatitude(v.latitude);
            s.setLongitude(v.longitude);
            s.setCapacity(v.capacity);

            s = stadiumRepo.save(s);
            stadiumByName.put(v.name, s);
            if (wasNew) {
                report.stadiumsCreated++;
                log.info("[venues] Estadio creado: {} ({}, {})",
                        v.name, v.city, v.country);
            } else {
                report.stadiumsUpdated++;
            }
        }

        // ---- FASE B: asignar venue a cada partido -------------------------
        List<OpenFootballMatch> openfootballMatches =
                openFootballClient.getWorldCup2026Matches();
        report.fetchedFromOpenFootball = openfootballMatches.size();

        if (openfootballMatches.isEmpty()) {
            log.warn("openfootball no devolvió partidos. ¿Sin internet?");
            return report;
        }

        List<Match> dbMatches = matchRepo.findAll();
        log.info("[venues] Partidos en BD: {}", dbMatches.size());

        for (OpenFootballMatch ofm : openfootballMatches) {
            // Resolver estadio a partir del ground (ciudad).
            String stadiumName = GROUND_TO_STADIUM_NAME.get(ofm.getGround());
            if (stadiumName == null) {
                log.warn("[venues] Ciudad sin mapeo a estadio: '{}'", ofm.getGround());
                report.unmappedGrounds++;
                continue;
            }
            Stadium stadium = stadiumByName.get(stadiumName);
            if (stadium == null) {
                log.warn("[venues] Estadio mapeado pero no encontrado: '{}'", stadiumName);
                report.unmappedGrounds++;
                continue;
            }

            // Normalizar nombres de equipos para el matching.
            String team1Norm = normalizeTeamName(ofm.getTeam1());
            String team2Norm = normalizeTeamName(ofm.getTeam2());

            // Buscar el partido equivalente en BD por nombres de equipos.
            Optional<Match> matchOpt = findMatchByTeamNames(dbMatches, team1Norm, team2Norm);
            if (matchOpt.isEmpty()) {
                log.debug("[venues] Partido sin equivalente en BD: {} vs {}",
                        ofm.getTeam1(), ofm.getTeam2());
                report.matchesNotInDb++;
                continue;
            }

            Match m = matchOpt.get();
            // Solo guardamos si cambió el estadio (evita updates innecesarios).
            if (m.getStadium() == null || !stadium.getId().equals(m.getStadium().getId())) {
                m.setStadium(stadium);
                matchRepo.save(m);
                report.matchesAssigned++;
            } else {
                report.matchesAlreadyAssigned++;
            }
        }

        log.info("=== Sync de venues terminado: {} ===", report);
        return report;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Normaliza un nombre de equipo aplicando los aliases conocidos.
     */
    private String normalizeTeamName(String original) {
        if (original == null) return null;
        return TEAM_ALIASES.getOrDefault(original, original);
    }

    /**
     * Busca un partido en la lista de BD que coincida por nombres de equipos
     * (independiente del orden home/away por seguridad, aunque normalmente
     * coincide en orden).
     */
    private Optional<Match> findMatchByTeamNames(List<Match> matches,
                                                  String name1, String name2) {
        for (Match m : matches) {
            String home = m.getHomeTeam() != null ? m.getHomeTeam().getName() : null;
            String away = m.getAwayTeam() != null ? m.getAwayTeam().getName() : null;
            if (home == null || away == null) continue;

            if ((home.equalsIgnoreCase(name1) && away.equalsIgnoreCase(name2))
                    || (home.equalsIgnoreCase(name2) && away.equalsIgnoreCase(name1))) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    // =========================================================================
    // Reporte de sync
    // =========================================================================

    /** Reporte con métricas de la sincronización de venues. */
    public static class VenueSyncReport {
        public int stadiumsCreated;
        public int stadiumsUpdated;
        public int fetchedFromOpenFootball;
        public int matchesAssigned;
        public int matchesAlreadyAssigned;
        public int matchesNotInDb;
        public int unmappedGrounds;

        @Override
        public String toString() {
            return "VenueSyncReport[stadiumsCreated=" + stadiumsCreated
                    + ", stadiumsUpdated=" + stadiumsUpdated
                    + ", fetchedFromOpenFootball=" + fetchedFromOpenFootball
                    + ", matchesAssigned=" + matchesAssigned
                    + ", matchesAlreadyAssigned=" + matchesAlreadyAssigned
                    + ", matchesNotInDb=" + matchesNotInDb
                    + ", unmappedGrounds=" + unmappedGrounds + "]";
        }
    }
}