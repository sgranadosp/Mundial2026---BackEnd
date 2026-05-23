/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente server-side de la <a href="https://overpass-api.de/">Overpass API</a>
 * de OpenStreetMap. Se usa durante la sincronización de partidos para
 * resolver las coordenadas geográficas (lat/lon) de un estadio a partir de
 * su nombre y ciudad, dado que API-Football provee ambos campos pero no
 * las coordenadas en el endpoint {@code /fixtures}.
 * <p>
 * La estrategia es defensiva: si Overpass no encuentra el estadio o falla
 * la consulta, el método devuelve {@code null} y el estadio se persiste sin
 * coordenadas (el frontend lo manejará mostrando un mensaje en el mapa).
 * Nunca se lanza una excepción que rompa la sincronización completa.
 * </p>
 * <p>
 * El frontend también consulta Overpass (para obtener el polígono del
 * estadio); ambas consultas son independientes pero usan la misma fuente
 * de datos.
 * </p>
 */
@Component
public class OverpassClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

    /** Endpoint público de Overpass. */
    private static final String OVERPASS_ENDPOINT = "https://overpass-api.de/api/interpreter";

    /** Cliente HTTP reutilizable. */
    private final RestTemplate restTemplate;

    /** Constructor por defecto. */
    public OverpassClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Resuelve las coordenadas del centroide de un estadio buscando en OSM
     * por nombre y opcionalmente filtrando por ciudad para desambiguar.
     * <p>
     * Estrategia: una primera consulta a nivel mundial filtrando por
     * {@code leisure=stadium} y nombre. Si hay varios resultados, devuelve
     * el primero (Overpass los ordena por relevancia interna). Si no hay
     * resultados, devuelve {@code null}.
     * </p>
     *
     * @param stadiumName Nombre oficial del estadio (ej. "Estadio Azteca").
     * @param city        Ciudad donde está el estadio (puede ser {@code null}).
     *                    Se ignora en esta versión y queda disponible para
     *                    una segunda búsqueda más restrictiva si en el futuro
     *                    aparecen ambigüedades.
     * @return Arreglo {@code [latitud, longitud]} o {@code null} si Overpass
     *         no devolvió coincidencias o falló la petición.
     */
    public double[] findStadiumCoordinates(String stadiumName, String city) {
        if (stadiumName == null || stadiumName.isBlank()) return null;

        // Escapamos comillas en el nombre para no romper la sintaxis de Overpass QL.
        String safeName = stadiumName.replace("\"", "\\\"");

        // Buscamos ways y relations (los estadios suelen modelarse como ways
        // poligonales). `out center;` devuelve un punto representativo del
        // centroide en lugar de la geometría completa, suficiente para
        // poblar lat/lon en BD.
        String query = "[out:json][timeout:25];"
                + "(way[\"leisure\"=\"stadium\"][\"name\"=\"" + safeName + "\"];"
                +  "relation[\"leisure\"=\"stadium\"][\"name\"=\"" + safeName + "\"];);"
                + "out center;";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("data", query);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    OVERPASS_ENDPOINT, HttpMethod.POST, entity, Map.class);

            Map<?, ?> body = response.getBody();
            if (body == null) return null;

            Object elementsObj = body.get("elements");
            if (!(elementsObj instanceof List<?>)) return null;
            List<?> elements = (List<?>) elementsObj;
            if (elements.isEmpty()) {
                log.debug("[overpass] sin resultados para '{}' (ciudad={})", stadiumName, city);
                return null;
            }

            // Primer elemento: cada way con `out center` tiene un objeto
            // {center: {lat, lon}}; las relations también.
            Object first = elements.get(0);
            if (!(first instanceof Map<?, ?>)) return null;
            Map<?, ?> el = (Map<?, ?>) first;

            Object centerObj = el.get("center");
            if (centerObj instanceof Map<?, ?>) {
                Map<?, ?> center = (Map<?, ?>) centerObj;
                Object lat = center.get("lat");
                Object lon = center.get("lon");
                if (lat instanceof Number && lon instanceof Number) {
                    return new double[] {
                            ((Number) lat).doubleValue(),
                            ((Number) lon).doubleValue()
                    };
                }
            }
            return null;
        } catch (RestClientException e) {
            log.warn("[overpass] Error consultando '{}': {}", stadiumName, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("[overpass] Error inesperado parseando respuesta para '{}': {}",
                    stadiumName, e.getMessage());
            return null;
        }
    }
}