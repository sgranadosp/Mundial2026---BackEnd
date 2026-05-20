/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.Objects;

/**
 * Clase de Transferencia de Datos (DTO) para representar una entrada en el
 * ranking de usuarios dentro de un grupo de polla o en la clasificación global.
 * El ranking se calcula y publica automáticamente cuando un partido finaliza.
 * Este DTO encapsula la posición del usuario, sus puntos acumulados y
 * estadísticas de rendimiento (total de pronósticos, aciertos de resultado,
 * aciertos de marcador exacto) para mostrar en el panel de la polla y en el
 * perfil personal del usuario.
 */
public class RankingDTO {

    /**
     * Posición del usuario en el ranking (1 = primer lugar).
     */
    private Integer position;

    /**
     * ID del usuario en la posición correspondiente.
     */
    private Long userId;

    /**
     * Nombre de usuario para mostrar en el ranking.
     */
    private String username;

    /**
     * ID del grupo de polla al que corresponde este ranking.
     * Nulo si es el ranking global de la plataforma.
     */
    private Long pollGroupId;

    /**
     * Nombre del grupo de polla. Nulo si es el ranking global.
     */
    private String pollGroupName;

    /**
     * Puntos totales acumulados por el usuario.
     */
    private Integer totalPoints;

    /**
     * Número total de pronósticos realizados por el usuario.
     */
    private Integer totalPredictions;

    /**
     * Número de pronósticos en que acertó al menos el resultado
     * (ganador o empate).
     */
    private Integer correctResults;

    /**
     * Número de pronósticos en que acertó el marcador exacto.
     */
    private Integer exactScores;

    /**
     * Constructor por defecto de {@code RankingDTO}.
     */
    public RankingDTO() {
    }

    /**
     * Constructor con los campos principales del ranking.
     *
     * @param position     Posición en el ranking.
     * @param userId       ID del usuario.
     * @param username     Nombre de usuario.
     * @param totalPoints  Puntos acumulados.
     */
    public RankingDTO(Integer position, Long userId, String username, Integer totalPoints) {
        this.position = position;
        this.userId = userId;
        this.username = username;
        this.totalPoints = totalPoints;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return La posición en el ranking. */
    public Integer getPosition() { return position; }

    /** @param position La nueva posición. */
    public void setPosition(Integer position) { this.position = position; }

    /** @return El ID del usuario. */
    public Long getUserId() { return userId; }

    /** @param userId El nuevo ID. */
    public void setUserId(Long userId) { this.userId = userId; }

    /** @return El nombre de usuario. */
    public String getUsername() { return username; }

    /** @param username El nuevo nombre de usuario. */
    public void setUsername(String username) { this.username = username; }

    /** @return El ID del grupo de polla. */
    public Long getPollGroupId() { return pollGroupId; }

    /** @param pollGroupId El nuevo ID del grupo. */
    public void setPollGroupId(Long pollGroupId) { this.pollGroupId = pollGroupId; }

    /** @return El nombre del grupo de polla. */
    public String getPollGroupName() { return pollGroupName; }

    /** @param pollGroupName El nuevo nombre del grupo. */
    public void setPollGroupName(String pollGroupName) { this.pollGroupName = pollGroupName; }

    /** @return Los puntos totales acumulados. */
    public Integer getTotalPoints() { return totalPoints; }

    /** @param totalPoints Los nuevos puntos totales. */
    public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }

    /** @return El total de pronósticos realizados. */
    public Integer getTotalPredictions() { return totalPredictions; }

    /** @param totalPredictions El nuevo total de pronósticos. */
    public void setTotalPredictions(Integer totalPredictions) {
        this.totalPredictions = totalPredictions;
    }

    /** @return El número de resultados correctos (ganador o empate). */
    public Integer getCorrectResults() { return correctResults; }

    /** @param correctResults El nuevo número de resultados correctos. */
    public void setCorrectResults(Integer correctResults) { this.correctResults = correctResults; }

    /** @return El número de marcadores exactos acertados. */
    public Integer getExactScores() { return exactScores; }

    /** @param exactScores El nuevo número de marcadores exactos. */
    public void setExactScores(Integer exactScores) { this.exactScores = exactScores; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RankingDTO other = (RankingDTO) obj;
        return Objects.equals(userId, other.userId) && Objects.equals(pollGroupId, other.pollGroupId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, pollGroupId); }

    @Override
    public String toString() {
        return "RankingDTO [position=" + position + ", username=" + username
                + ", pollGroupName=" + pollGroupName
                + ", totalPoints=" + totalPoints
                + ", correctResults=" + correctResults
                + ", exactScores=" + exactScores + "]";
    }
}