/**
 * Paquete con los DTOs de respuesta de APIs externas consumidas por el
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de la respuesta de
 * <a href="https://github.com/openfootball/worldcup.json">openfootball/worldcup.json</a>
 * para el Mundial 2026.
 * <p>
 * Es un dataset open public domain servido como JSON estático por GitHub.
 * No requiere API key ni tiene límites de cuota. La URL canónica es:
 * <pre>
 * https://raw.githubusercontent.com/openfootball/worldcup.json/master/2026/worldcup.json
 * </pre>
 * </p>
 * <p>
 * Estructura simplificada (solo los campos que consumimos):
 * <pre>
 * {
 *   "name": "World Cup 2026",
 *   "matches": [
 *     { "team1": "Mexico",      "team2": "South Africa", "ground": "Mexico City",            ... },
 *     { "team1": "South Korea", "team2": "Czech Republic","ground": "Guadalajara (Zapopan)", ... },
 *     ...
 *   ]
 * }
 * </pre>
 * </p>
 * <p>
 * Nota importante sobre el campo {@code ground}: openfootball devuelve la
 * <b>ciudad</b> donde se juega, no el nombre del estadio. Como cada ciudad
 * sede del Mundial 2026 tiene un solo estadio, el mapeo ciudad → estadio es
 * 1:1 y se resuelve en {@code OpenFootballSyncService}.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFootballWorldCupResponse {

    /** Nombre del torneo (ej. "World Cup 2026"). */
    private String name;

    /** Lista de los 104 partidos del torneo. */
    private List<OpenFootballMatch> matches;

    /** Constructor por defecto requerido por Jackson. */
    public OpenFootballWorldCupResponse() {
    }

    /** @return Nombre del torneo. */
    public String getName() { return name; }

    /** @param name Nombre del torneo. */
    public void setName(String name) { this.name = name; }

    /** @return Lista de partidos. */
    public List<OpenFootballMatch> getMatches() { return matches; }

    /** @param matches Lista de partidos. */
    public void setMatches(List<OpenFootballMatch> matches) { this.matches = matches; }

    // =========================================================================
    // Partido individual
    // =========================================================================

    /**
     * Un partido tal como lo devuelve openfootball. Aplanado, sin estructuras
     * anidadas, lo que hace el parseo trivial.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenFootballMatch {

        /** Ronda del torneo (ej. "Matchday 1", "Round of 32", "Final"). */
        private String round;

        /** Fecha del partido en formato ISO local (ej. "2026-06-11"). */
        private String date;

        /** Hora del partido en formato libre (ej. "13:00 UTC-6"). */
        private String time;

        /** Nombre del equipo local. */
        private String team1;

        /** Nombre del equipo visitante. */
        private String team2;

        /** Grupo de la fase de grupos (ej. "Group A"). Null en eliminatorias. */
        private String group;

        /** Ciudad/sede donde se juega (ej. "Mexico City", "Atlanta"). */
        private String ground;

        public OpenFootballMatch() { }

        public String getRound() { return round; }
        public void setRound(String round) { this.round = round; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public String getTeam1() { return team1; }
        public void setTeam1(String team1) { this.team1 = team1; }

        public String getTeam2() { return team2; }
        public void setTeam2(String team2) { this.team2 = team2; }

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }

        public String getGround() { return ground; }
        public void setGround(String ground) { this.ground = ground; }
    }
}