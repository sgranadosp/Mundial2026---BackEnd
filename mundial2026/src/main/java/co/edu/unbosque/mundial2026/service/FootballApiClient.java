/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import co.edu.unbosque.mundial2026.dto.FootballDataMatchItem;
import co.edu.unbosque.mundial2026.dto.FootballDataMatchesResponse;

/**
 * Cliente HTTP para consumir la API externa de
 * <a href="https://docs.football-data.org/general/v4/index.html">football-data.org v4</a>.
 * <p>
 * Encapsula los detalles de autenticación (header {@code X-Auth-Token}) y
 * la construcción de URLs. No mantiene estado entre llamadas.
 * </p>
 * <p>
 * <b>Cuotas:</b> el plan Free de football-data.org tiene un límite de
 * <b>10 requests por minuto</b>. Como nuestras sincronizaciones son
 * manuales y traen todos los partidos del Mundial en una sola request,
 * la cuota es suficiente.
 * </p>
 * <p>
 * <b>Endpoint base:</b> {@code https://api.football-data.org/v4}
 * </p>
 */
@Component
public class FootballApiClient {

    private static final Logger log = LoggerFactory.getLogger(FootballApiClient.class);

    /** URL base de football-data.org v4. */
    private static final String BASE_URL = "https://api.football-data.org/v4";

    /** Header esperado por football-data.org para autenticación. */
    private static final String API_TOKEN_HEADER = "X-Auth-Token";

    /** Clave de API, inyectada desde {@code application.properties}. */
    @Value("${apifootball.key}")
    private String apiKey;

    /** Cliente HTTP reutilizable. {@link RestTemplate} es thread-safe. */
    private final RestTemplate restTemplate;

    /** Constructor por defecto. Crea el RestTemplate interno. */
    public FootballApiClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Trae todos los partidos de una competición filtrados por etapa.
     * <p>
     * Para el Mundial 2026 fase de grupos: {@code competitionCode = "WC"},
     * {@code stage = "GROUP_STAGE"}.
     * </p>
     *
     * @param competitionCode Código de la competición (ej. "WC" para Mundial,
     *                        "CL" para Champions, "PL" para Premier League).
     * @param stage           Etapa a filtrar. Si es {@code null} o vacío, no
     *                        se filtra y devuelve todos. Para fase de grupos
     *                        usar "GROUP_STAGE".
     * @return Lista de partidos, o lista vacía si la API no respondió con datos.
     */
    public List<FootballDataMatchItem> getMatchesByCompetition(String competitionCode, String stage) {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("/competitions/")
                .append(competitionCode)
                .append("/matches");
        if (stage != null && !stage.isBlank()) {
            url.append("?stage=").append(stage);
        }
        log.info("[football-data.org] GET {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(API_TOKEN_HEADER, apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<FootballDataMatchesResponse> response = restTemplate.exchange(
                    url.toString(), HttpMethod.GET, entity, FootballDataMatchesResponse.class);

            FootballDataMatchesResponse body = response.getBody();
            if (body == null) {
                log.warn("[football-data.org] Respuesta sin body");
                return Collections.emptyList();
            }

            List<FootballDataMatchItem> matches = body.getMatches();
            if (matches == null) {
                log.warn("[football-data.org] matches es null. count={}", body.getCount());
                return Collections.emptyList();
            }

            String compName = body.getCompetition() != null
                    ? body.getCompetition().getName() : "?";
            log.info("[football-data.org] Recibidos {} partidos de la competición '{}'",
                    matches.size(), compName);
            return matches;

        } catch (RestClientException e) {
            log.error("[football-data.org] Error consultando partidos: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}