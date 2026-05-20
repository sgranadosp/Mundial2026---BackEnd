/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.MatchDTO;
import co.edu.unbosque.mundial2026.dto.TeamDTO;
import co.edu.unbosque.mundial2026.model.Match.DataStatus;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;

/**
 * Adaptador de servicio para consumir datos de partidos desde la API externa
 * (football-data.org, API-Football o WireMock Cloud según configuración).
 * Sigue el mismo patrón de {@code ExternalHTTPRequestHandler} del proyecto
 * VirusDetected: cliente HTTP singleton con timeout, parseo JSON con Jackson,
 * y manejo explícito de fallos. Cuando la fuente falla o responde lento,
 * el servicio delega a {@link MatchService#markAsPendingUpdate(Long)} para
 * implementar la degradación elegante requerida por el proyecto.
 * La URL base y el API key se inyectan desde {@code application.properties}
 * para facilitar el cambio de proveedor sin reescribir lógica de negocio.
 * En entornos de desarrollo se usa WireMock Cloud con un contrato documentado.
 */
@Service
public class ExternalMatchService {

    /**
     * URL base de la API de datos deportivos. Se configura en application.properties
     * como {@code external.api.football.base-url}.
     */
    @Value("${external.api.football.base-url:https://api.football-data.org/v4}")
    private String baseUrl;

    /**
     * API key del proveedor de datos. Se configura en application.properties
     * como {@code external.api.football.api-key}.
     */
    @Value("${external.api.football.api-key:}")
    private String apiKey;

    /**
     * Cliente HTTP singleton configurado con HTTP/2 y timeout de 10 segundos.
     * Mismo patrón que el proyecto VirusDetected.
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Mapper para parsear respuestas JSON desde la API externa.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MatchService matchService;

    @Autowired
    private AuditEventService auditService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public ExternalMatchService() {
    }

    // =========================================================================
    // Métodos de sincronización
    // =========================================================================

    /**
     * Sincroniza todos los partidos del Mundial 2026 desde la API externa.
     * Obtiene el fixture completo del torneo (competition code: WC) y persiste
     * o actualiza cada partido. Si la API falla, registra el evento en auditoría
     * y retorna sin interrumpir la operación del sistema.
     *
     * @return Número de partidos sincronizados exitosamente; -1 si hubo fallo total.
     */
    public int syncAllMatches() {
        String url = baseUrl + "/competitions/WC/matches";
        try {
            String responseBody = doGet(url);
            if (responseBody == null) return -1;

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode matches = root.get("matches");
            if (matches == null || !matches.isArray()) return -1;

            int synced = 0;
            for (JsonNode matchNode : matches) {
                MatchDTO dto = parseMatchNode(matchNode);
                if (dto != null) {
                    matchService.create(dto);
                    synced++;
                }
            }
            return synced;

        } catch (IOException e) {
            auditService.logNotificationFailed(null, "Fallo al sincronizar partidos: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Obtiene el resultado actualizado de un partido específico por su ID externo.
     * Usado para actualizar marcadores en tiempo real durante partidos en curso.
     * Si la API no responde, marca el partido como {@code PENDING_UPDATE}.
     *
     * @param externalMatchId El ID del partido en la API externa.
     * @return El {@link MatchDTO} actualizado, o {@code null} si falla la consulta.
     */
    public MatchDTO fetchMatchById(Long externalMatchId) {
        String url = baseUrl + "/matches/" + externalMatchId;
        try {
            String responseBody = doGet(url);
            if (responseBody == null) {
                matchService.markAsPendingUpdate(externalMatchId);
                return null;
            }
            JsonNode root = objectMapper.readTree(responseBody);
            return parseMatchNode(root);

        } catch (IOException e) {
            matchService.markAsPendingUpdate(externalMatchId);
            return null;
        }
    }

    /**
     * Obtiene información de un equipo por su ID externo.
     * Se usa durante la sincronización inicial para poblar la tabla de equipos.
     *
     * @param externalTeamId El ID del equipo en la API externa.
     * @return El {@link TeamDTO} con los datos del equipo, o {@code null} si falla.
     */
    public TeamDTO fetchTeamById(Long externalTeamId) {
        String url = baseUrl + "/teams/" + externalTeamId;
        try {
            String responseBody = doGet(url);
            if (responseBody == null) return null;

            JsonNode root = objectMapper.readTree(responseBody);
            return parseTeamNode(root);

        } catch (IOException e) {
            return null;
        }
    }

    // =========================================================================
    // Helpers de HTTP y parseo
    // =========================================================================

    /**
     * Realiza una petición GET autenticada a la URL indicada.
     * Retorna el cuerpo de la respuesta como String, o {@code null} si falla.
     *
     * @param url La URL completa del endpoint.
     * @return El cuerpo de la respuesta, o {@code null} en caso de error.
     */
    private String doGet(String url) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("X-Auth-Token", apiKey);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response;

        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        if (response.statusCode() != 200) {
            System.out.println("ExternalMatchService: respuesta " + response.statusCode() + " desde " + url);
            return null;
        }
        return response.body();
    }

    /**
     * Parsea un nodo JSON de partido (formato football-data.org) a {@link MatchDTO}.
     * Los campos faltantes se ignoran para tolerar variaciones entre proveedores.
     *
     * @param node El nodo JSON del partido.
     * @return El {@link MatchDTO} parseado, o {@code null} si falta el ID externo.
     */
    private MatchDTO parseMatchNode(JsonNode node) {
        if (node == null || node.get("id") == null) return null;

        MatchDTO dto = new MatchDTO();
        dto.setExternalId(node.get("id").asLong());

        JsonNode utcDate = node.get("utcDate");
        if (utcDate != null) {
            try {
                dto.setScheduledAt(LocalDateTime.parse(
                        utcDate.asText().replace("Z", ""),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (Exception ignored) {
            }
        }

        JsonNode statusNode = node.get("status");
        if (statusNode != null) {
            dto.setStatus(parseMatchStatus(statusNode.asText()));
        }

        JsonNode score = node.get("score");
        if (score != null) {
            JsonNode fullTime = score.get("fullTime");
            if (fullTime != null) {
                JsonNode home = fullTime.get("home");
                JsonNode away = fullTime.get("away");
                if (home != null && !home.isNull()) dto.setHomeScore(home.asInt());
                if (away != null && !away.isNull()) dto.setAwayScore(away.asInt());
            }
        }

        JsonNode homeTeam = node.get("homeTeam");
        if (homeTeam != null && homeTeam.get("id") != null) {
            dto.setHomeTeamId(homeTeam.get("id").asLong());
            if (homeTeam.get("name") != null) dto.setHomeTeamName(homeTeam.get("name").asText());
            if (homeTeam.get("shortName") != null) {
                dto.setHomeTeamIsoCode(homeTeam.get("shortName").asText());
            }
            if (homeTeam.get("crest") != null) dto.setHomeTeamCrestUrl(homeTeam.get("crest").asText());
        }

        JsonNode awayTeam = node.get("awayTeam");
        if (awayTeam != null && awayTeam.get("id") != null) {
            dto.setAwayTeamId(awayTeam.get("id").asLong());
            if (awayTeam.get("name") != null) dto.setAwayTeamName(awayTeam.get("name").asText());
            if (awayTeam.get("shortName") != null) {
                dto.setAwayTeamIsoCode(awayTeam.get("shortName").asText());
            }
            if (awayTeam.get("crest") != null) dto.setAwayTeamCrestUrl(awayTeam.get("crest").asText());
        }

        JsonNode group = node.get("group");
        if (group != null && !group.isNull()) dto.setGroupName(group.asText());

        JsonNode matchday = node.get("matchday");
        if (matchday != null && !matchday.isNull()) dto.setMatchday(matchday.asInt());

        dto.setDataStatus(DataStatus.CONFIRMED);
        return dto;
    }

    /**
     * Parsea un nodo JSON de equipo (formato football-data.org) a {@link TeamDTO}.
     *
     * @param node El nodo JSON del equipo.
     * @return El {@link TeamDTO} parseado, o {@code null} si falta el ID externo.
     */
    private TeamDTO parseTeamNode(JsonNode node) {
        if (node == null || node.get("id") == null) return null;

        TeamDTO dto = new TeamDTO();
        dto.setExternalId(node.get("id").asLong());
        if (node.get("name") != null) dto.setName(node.get("name").asText());
        if (node.get("shortName") != null) dto.setShortName(node.get("shortName").asText());
        if (node.get("tla") != null) dto.setIsoCode(node.get("tla").asText());
        if (node.get("crest") != null) dto.setCrestUrl(node.get("crest").asText());
        return dto;
    }

    /**
     * Convierte el estado textual de la API externa al enum {@link MatchStatus}.
     * Maneja los estados de football-data.org: TIMED, SCHEDULED, IN_PLAY,
     * PAUSED, FINISHED, SUSPENDED, POSTPONED, CANCELLED.
     *
     * @param status El estado como String desde la API.
     * @return El {@link MatchStatus} correspondiente; {@code SCHEDULED} si no se reconoce.
     */
    private MatchStatus parseMatchStatus(String status) {
        return switch (status.toUpperCase()) {
            case "IN_PLAY", "PAUSED" -> MatchStatus.LIVE;
            case "FINISHED" -> MatchStatus.FINISHED;
            case "POSTPONED" -> MatchStatus.POSTPONED;
            case "CANCELLED", "SUSPENDED" -> MatchStatus.CANCELLED;
            default -> MatchStatus.SCHEDULED;
        };
    }
}