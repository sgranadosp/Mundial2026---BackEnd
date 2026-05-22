/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de la respuesta raíz del endpoint
 * {@code GET /v4/competitions/{code}/matches} de
 * <a href="https://docs.football-data.org/general/v4/match.html">football-data.org v4</a>.
 * <p>
 * Esta clase y {@link FootballDataMatchItem} reflejan literalmente el JSON
 * crudo que devuelve la API externa. NO se exponen en endpoints REST propios;
 * se usan internamente por {@code FootballApiClient} antes de mapear a
 * entidades JPA en {@code FootballApiSyncService}.
 * </p>
 * <p>
 * <b>Formato esperado:</b>
 * <pre>
 * {
 *   "count": 72,
 *   "competition": { "id": 2000, "name": "FIFA World Cup", "code": "WC", ... },
 *   "matches": [ {...}, {...}, ... ]
 * }
 * </pre>
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootballDataMatchesResponse {

    /**
     * Número total de partidos devueltos en {@code matches}.
     */
    private Integer count;

    /**
     * Información de la competición filtrada (Mundial = "WC").
     */
    private CompetitionInfo competition;

    /**
     * Lista de partidos devueltos por la API. Vacía si no hay datos para
     * los filtros indicados.
     */
    private List<FootballDataMatchItem> matches;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public CompetitionInfo getCompetition() { return competition; }
    public void setCompetition(CompetitionInfo competition) { this.competition = competition; }

    public List<FootballDataMatchItem> getMatches() { return matches; }
    public void setMatches(List<FootballDataMatchItem> matches) { this.matches = matches; }

    /**
     * Información mínima de la competición para validar que recibimos
     * lo que esperamos.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompetitionInfo {
        private Integer id;
        private String name;
        private String code;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}