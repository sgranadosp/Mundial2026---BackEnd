/**
 * Paquete con los DTOs de respuesta de APIs externas consumidas por el
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de la respuesta del endpoint <code>GET /fixtures</code> de
 * <a href="https://www.api-football.com/documentation-v3">API-Football v3</a>.
 * <p>
 * La respuesta tiene forma {@code {"get":..., "parameters":..., "errors":[],
 * "results":N, "paging":..., "response":[...]}}. Solo nos interesa el array
 * {@code response}, donde cada elemento es un partido (fixture).
 * </p>
 * <p>
 * Usamos {@link JsonIgnoreProperties#ignoreUnknown()} para tolerar campos
 * adicionales que la API agregue en el futuro sin romper el parseo.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFootballFixturesResponse {

    /** Array de fixtures devueltos por la API. */
    private List<Fixture> response;

    /** Constructor por defecto requerido por Jackson. */
    public ApiFootballFixturesResponse() {
    }

    /** @return La lista de fixtures de la respuesta. */
    public List<Fixture> getResponse() { return response; }

    /** @param response Lista de fixtures a almacenar. */
    public void setResponse(List<Fixture> response) { this.response = response; }

    // =========================================================================
    // Estructuras anidadas (solo lo que consumimos)
    // =========================================================================

    /**
     * Un partido completo de API-Football. Combina datos del propio fixture,
     * la liga, los equipos y los goles.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private FixtureInfo fixture;
        private LeagueInfo league;
        private TeamsInfo teams;
        private GoalsInfo goals;

        public Fixture() { }

        public FixtureInfo getFixture() { return fixture; }
        public void setFixture(FixtureInfo fixture) { this.fixture = fixture; }
        public LeagueInfo getLeague() { return league; }
        public void setLeague(LeagueInfo league) { this.league = league; }
        public TeamsInfo getTeams() { return teams; }
        public void setTeams(TeamsInfo teams) { this.teams = teams; }
        public GoalsInfo getGoals() { return goals; }
        public void setGoals(GoalsInfo goals) { this.goals = goals; }
    }

    /**
     * Datos del partido en sí: identificador, fecha en formato ISO-8601 con
     * offset, estado y estadio (venue).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixtureInfo {
        private Long id;
        private String date;       // ej. "2026-06-11T20:00:00+00:00"
        private Status status;
        private Venue venue;

        public FixtureInfo() { }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public Venue getVenue() { return venue; }
        public void setVenue(Venue venue) { this.venue = venue; }
    }

    /**
     * Estado del partido. {@code short} es un código corto (NS, 1H, FT, etc.)
     * usado para mapear al enum interno {@link
     * co.edu.unbosque.mundial2026.model.Match.MatchStatus}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        // "short" es palabra reservada en Java → usamos otro nombre con anotación.
        @com.fasterxml.jackson.annotation.JsonProperty("short")
        private String shortCode;

        @com.fasterxml.jackson.annotation.JsonProperty("long")
        private String longName;

        public Status() { }

        public String getShortCode() { return shortCode; }
        public void setShortCode(String shortCode) { this.shortCode = shortCode; }
        public String getLongName() { return longName; }
        public void setLongName(String longName) { this.longName = longName; }
    }

    /**
     * Estadio donde se juega el partido. API-Football provee {@code name} y
     * {@code city} pero NO coordenadas: las resolvemos server-side con
     * Overpass cuando creamos el estadio en BD.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Venue {
        private Long id;
        private String name;
        private String city;

        public Venue() { }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    /**
     * Información de la liga/competición. Para nosotros solo importa {@code round}
     * (ej. "Group Stage - 1") para extraer la jornada, y {@code group} si la API
     * lo provee.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeagueInfo {
        private Long id;
        private String name;
        private String round;

        public LeagueInfo() { }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRound() { return round; }
        public void setRound(String round) { this.round = round; }
    }

    /** Contenedor de los dos equipos. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamsInfo {
        private TeamRef home;
        private TeamRef away;

        public TeamsInfo() { }

        public TeamRef getHome() { return home; }
        public void setHome(TeamRef home) { this.home = home; }
        public TeamRef getAway() { return away; }
        public void setAway(TeamRef away) { this.away = away; }
    }

    /** Equipo con id externo, nombre y URL del escudo (logo). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamRef {
        private Long id;
        private String name;
        private String logo;
        private Boolean winner;

        public TeamRef() { }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }
        public Boolean getWinner() { return winner; }
        public void setWinner(Boolean winner) { this.winner = winner; }
    }

    /** Goles al final del partido. Cualquiera puede ser {@code null} si no jugó. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoalsInfo {
        private Integer home;
        private Integer away;

        public GoalsInfo() { }

        public Integer getHome() { return home; }
        public void setHome(Integer home) { this.home = home; }
        public Integer getAway() { return away; }
        public void setAway(Integer away) { this.away = away; }
    }
}