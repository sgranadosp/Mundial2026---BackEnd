/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import co.edu.unbosque.mundial2026.dto.OpenFootballWorldCupResponse;
import co.edu.unbosque.mundial2026.dto.OpenFootballWorldCupResponse.OpenFootballMatch;

/**
 * Cliente HTTP para
 * <a href="https://github.com/openfootball/worldcup.json">openfootball/worldcup.json</a>,
 * el dataset open public domain del calendario completo del Mundial 2026.
 * <p>
 * No requiere API key ni autenticación; el JSON se sirve estáticamente desde
 * GitHub raw. Se usa específicamente para enriquecer la información de
 * estadios (sede de cada partido), dado que football-data.org no provee el
 * campo {@code venue} en su plan gratuito.
 * </p>
 * <p>
 * <b>Nota técnica:</b> GitHub raw sirve el archivo con content-type
 * {@code text/plain}, no {@code application/json}. Por eso configuramos el
 * RestTemplate para que su Jackson converter acepte también {@code text/plain}
 * y {@code application/octet-stream}, y mandamos un header {@code Accept: *\/*}
 * para no pedirle a GitHub que filtre por content-type.
 * </p>
 */
@Component
public class OpenFootballClient {

    private static final Logger log = LoggerFactory.getLogger(OpenFootballClient.class);

    /** URL canónica del JSON del Mundial 2026 en openfootball. */
    private static final String WORLD_CUP_2026_URL =
            "https://raw.githubusercontent.com/openfootball/worldcup.json/master/2026/worldcup.json";

    /** Cliente HTTP reutilizable. {@link RestTemplate} es thread-safe. */
    private final RestTemplate restTemplate;

    /**
     * Constructor por defecto. Crea el RestTemplate y ajusta el Jackson
     * converter para que acepte respuestas con content-type {@code text/plain}
     * (que es lo que devuelve GitHub raw).
     */
    public OpenFootballClient() {
        this.restTemplate = new RestTemplate();

        // Buscamos el Jackson converter y le añadimos los media types que
        // GitHub raw puede devolver, ya que su default solo es application/json.
        for (Object converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jackson =
                        (MappingJackson2HttpMessageConverter) converter;
                jackson.setSupportedMediaTypes(java.util.Arrays.asList(
                        MediaType.APPLICATION_JSON,
                        MediaType.valueOf("text/json"),
                        MediaType.TEXT_PLAIN,
                        MediaType.APPLICATION_OCTET_STREAM,
                        MediaType.ALL
                ));
            }
        }
    }

    /**
     * Descarga el JSON del Mundial 2026 desde openfootball.
     *
     * @return Lista de los 104 partidos del torneo, o lista vacía si el
     *         servidor no respondió correctamente.
     */
    public List<OpenFootballMatch> getWorldCup2026Matches() {
        log.info("[openfootball] GET {}", WORLD_CUP_2026_URL);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<OpenFootballWorldCupResponse> response = restTemplate.exchange(
                    WORLD_CUP_2026_URL, HttpMethod.GET, entity,
                    OpenFootballWorldCupResponse.class);

            log.info("[openfootball] HTTP {} | Content-Type: {}",
                    response.getStatusCode(),
                    response.getHeaders().getContentType());

            OpenFootballWorldCupResponse body = response.getBody();
            if (body == null) {
                log.warn("[openfootball] Respuesta sin body");
                return Collections.emptyList();
            }
            if (body.getMatches() == null) {
                log.warn("[openfootball] Body sin campo 'matches'. Body recibido: name='{}'",
                        body.getName());
                return Collections.emptyList();
            }
            log.info("[openfootball] Recibidos {} partidos del torneo \"{}\"",
                    body.getMatches().size(), body.getName());
            return body.getMatches();
        } catch (RestClientException e) {
            log.error("[openfootball] Error descargando JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}