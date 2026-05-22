/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO que representa un partido individual dentro de la respuesta de
 * football-data.org v4.
 * <p>
 * El JSON real tiene muchos más campos (alineaciones, eventos, árbitros,
 * apuestas, etc.), pero solo deserializamos los que necesitamos para nuestro
 * caso de uso: datos del partido (id, fecha, estado, sede, etapa, grupo),
 * los dos equipos y el marcador.
 * </p>
 * <p>
 * La anotación {@code @JsonIgnoreProperties(ignoreUnknown = true)} hace que
 * Jackson descarte los campos extras sin fallar.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootballDataMatchItem {

    /**
     * ID del partido en la API externa (lo usamos como {@code externalId} en BD).
     */
    private Long id;

    /**
     * Fecha y hora del partido en formato ISO-8601 UTC (ej. "2026-06-11T20:00:00Z").
     */
    private String utcDate;

    /**
     * Estado actual del partido.
     * Posibles valores: SCHEDULED, TIMED, IN_PLAY, PAUSED, FINISHED, SUSPENDED,
     * POSTPONED, CANCELLED, AWARDED.
     */
    private String status;

    /**
     * Minuto actual del partido (solo durante en vivo).
     */
    private Integer minute;

    /**
     * Nombre del estadio (en v4 viene como string plano, no como objeto anidado).
     */
    private String venue;

    /**
     * Número de jornada.
     */
    private Integer matchday;

    /**
     * Etapa del torneo. Para fase de grupos = "GROUP_STAGE".
     * Otros valores: LAST_16, QUARTER_FINALS, SEMI_FINALS, FINAL, etc.
     */
    private String stage;

    /**
     * Grupo al que pertenece (solo en fase de grupos).
     * Valores: GROUP_A, GROUP_B, ..., GROUP_L (12 grupos en Mundial 2026).
     */
    private String group;

    /**
     * Equipo local.
     */
    private TeamRef homeTeam;

    /**
     * Equipo visitante.
     */
    private TeamRef awayTeam;

    /**
     * Marcador completo del partido.
     */
    private ScoreInfo score;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUtcDate() { return utcDate; }
    public void setUtcDate(String utcDate) { this.utcDate = utcDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public Integer getMatchday() { return matchday; }
    public void setMatchday(Integer matchday) { this.matchday = matchday; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public TeamRef getHomeTeam() { return homeTeam; }
    public void setHomeTeam(TeamRef homeTeam) { this.homeTeam = homeTeam; }

    public TeamRef getAwayTeam() { return awayTeam; }
    public void setAwayTeam(TeamRef awayTeam) { this.awayTeam = awayTeam; }

    public ScoreInfo getScore() { return score; }
    public void setScore(ScoreInfo score) { this.score = score; }

    // =========================================================================
    // Clases anidadas
    // =========================================================================

    /**
     * Referencia a un equipo dentro de un partido.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamRef {
        /** ID del equipo en la API externa. */
        private Long id;
        /** Nombre completo del equipo (ej. "Brazil"). */
        private String name;
        /** Nombre corto del equipo (ej. "Brazil"). */
        private String shortName;
        /** Sigla de 3 letras (ej. "BRA"). */
        private String tla;
        /** URL del escudo (SVG o PNG). */
        private String crest;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }

        public String getTla() { return tla; }
        public void setTla(String tla) { this.tla = tla; }

        public String getCrest() { return crest; }
        public void setCrest(String crest) { this.crest = crest; }
    }

    /**
     * Marcador completo. Contiene resultado del tiempo regular, medio tiempo
     * y final (que puede incluir tiempo extra y penaltis).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreInfo {
        /** "HOME_TEAM", "AWAY_TEAM", "DRAW" o null si aún no termina. */
        private String winner;
        /** "REGULAR", "EXTRA_TIME" o "PENALTY_SHOOTOUT". */
        private String duration;
        /** Marcador al finalizar el partido (90 min + tiempo extra si hubo). */
        private GoalsInfo fullTime;
        /** Marcador al medio tiempo. */
        private GoalsInfo halfTime;

        public String getWinner() { return winner; }
        public void setWinner(String winner) { this.winner = winner; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public GoalsInfo getFullTime() { return fullTime; }
        public void setFullTime(GoalsInfo fullTime) { this.fullTime = fullTime; }

        public GoalsInfo getHalfTime() { return halfTime; }
        public void setHalfTime(GoalsInfo halfTime) { this.halfTime = halfTime; }
    }

    /**
     * Goles por equipo en un momento dado.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoalsInfo {
        /** Goles del equipo local (puede ser null si no aplica). */
        private Integer home;
        /** Goles del equipo visitante (puede ser null si no aplica). */
        private Integer away;

        public Integer getHome() { return home; }
        public void setHome(Integer home) { this.home = home; }

        public Integer getAway() { return away; }
        public void setAway(Integer away) { this.away = away; }
    }
}