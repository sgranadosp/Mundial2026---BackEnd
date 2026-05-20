/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.unbosque.mundial2026.model.Prediction.PredictionStatus;

/**
 * Clase de Transferencia de Datos (DTO) para representar el pronóstico
 * de un usuario sobre un partido dentro de un grupo de polla.
 * <p>
 * Se usa tanto para recibir el pronóstico del cliente (campos {@code matchId},
 * {@code pollGroupId}, {@code predictedHomeScore}, {@code predictedAwayScore})
 * como para devolver el resultado evaluado con el puntaje obtenido
 * ({@code pointsEarned}, {@code status}).
 * </p>
 */
public class PredictionDTO {

    /**
     * Identificador único del pronóstico.
     */
    private Long id;

    /**
     * ID del usuario que realizó el pronóstico.
     */
    private Long userId;

    /**
     * Nombre de usuario del autor del pronóstico (para ranking y vistas de grupo).
     */
    private String username;

    /**
     * ID del partido sobre el que se pronostica.
     */
    private Long matchId;

    /**
     * Nombre del equipo local del partido (para mostrar en la UI sin llamada adicional).
     */
    private String homeTeamName;

    /**
     * Nombre del equipo visitante del partido.
     */
    private String awayTeamName;

    /**
     * ID del grupo de polla al que pertenece el pronóstico.
     */
    private Long pollGroupId;

    /**
     * Nombre del grupo de polla (para mostrar en el historial del usuario).
     */
    private String pollGroupName;

    /**
     * Goles predichos para el equipo local.
     */
    private Integer predictedHomeScore;

    /**
     * Goles predichos para el equipo visitante.
     */
    private Integer predictedAwayScore;

    /**
     * Fecha y hora del último envío o modificación del pronóstico.
     */
    private LocalDateTime submittedAt;

    /**
     * Estado actual del pronóstico: OPEN, LOCKED o EVALUATED.
     */
    private PredictionStatus status;

    /**
     * Puntos obtenidos una vez evaluado el pronóstico.
     * Nulo mientras el partido no haya finalizado.
     */
    private Integer pointsEarned;

    /**
     * Marcador real del partido (para mostrar en el historial junto al pronóstico).
     * Nulo mientras el partido no haya finalizado.
     */
    private Integer actualHomeScore;

    /**
     * Marcador real del equipo visitante.
     * Nulo mientras el partido no haya finalizado.
     */
    private Integer actualAwayScore;

    /**
     * Constructor por defecto de {@code PredictionDTO}.
     */
    public PredictionDTO() {
    }

    /**
     * Constructor con los campos mínimos para crear un pronóstico.
     *
     * @param matchId            ID del partido.
     * @param pollGroupId        ID del grupo de polla.
     * @param predictedHomeScore Goles predichos para el equipo local.
     * @param predictedAwayScore Goles predichos para el equipo visitante.
     */
    public PredictionDTO(Long matchId, Long pollGroupId,
                         Integer predictedHomeScore, Integer predictedAwayScore) {
        this.matchId = matchId;
        this.pollGroupId = pollGroupId;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del pronóstico. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID del usuario. */
    public Long getUserId() { return userId; }

    /** @param userId El nuevo ID de usuario. */
    public void setUserId(Long userId) { this.userId = userId; }

    /** @return El nombre de usuario del autor. */
    public String getUsername() { return username; }

    /** @param username El nuevo nombre de usuario. */
    public void setUsername(String username) { this.username = username; }

    /** @return El ID del partido. */
    public Long getMatchId() { return matchId; }

    /** @param matchId El nuevo ID del partido. */
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    /** @return El nombre del equipo local. */
    public String getHomeTeamName() { return homeTeamName; }

    /** @param homeTeamName El nuevo nombre del equipo local. */
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    /** @return El nombre del equipo visitante. */
    public String getAwayTeamName() { return awayTeamName; }

    /** @param awayTeamName El nuevo nombre del equipo visitante. */
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

    /** @return El ID del grupo de polla. */
    public Long getPollGroupId() { return pollGroupId; }

    /** @param pollGroupId El nuevo ID del grupo. */
    public void setPollGroupId(Long pollGroupId) { this.pollGroupId = pollGroupId; }

    /** @return El nombre del grupo de polla. */
    public String getPollGroupName() { return pollGroupName; }

    /** @param pollGroupName El nuevo nombre del grupo. */
    public void setPollGroupName(String pollGroupName) { this.pollGroupName = pollGroupName; }

    /** @return Los goles predichos para el equipo local. */
    public Integer getPredictedHomeScore() { return predictedHomeScore; }

    /** @param predictedHomeScore Los nuevos goles predichos para el equipo local. */
    public void setPredictedHomeScore(Integer predictedHomeScore) {
        this.predictedHomeScore = predictedHomeScore;
    }

    /** @return Los goles predichos para el equipo visitante. */
    public Integer getPredictedAwayScore() { return predictedAwayScore; }

    /** @param predictedAwayScore Los nuevos goles predichos para el equipo visitante. */
    public void setPredictedAwayScore(Integer predictedAwayScore) {
        this.predictedAwayScore = predictedAwayScore;
    }

    /** @return La fecha y hora de envío del pronóstico. */
    public LocalDateTime getSubmittedAt() { return submittedAt; }

    /** @param submittedAt La nueva fecha de envío. */
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    /** @return El estado del pronóstico. */
    public PredictionStatus getStatus() { return status; }

    /** @param status El nuevo estado. */
    public void setStatus(PredictionStatus status) { this.status = status; }

    /** @return Los puntos obtenidos. */
    public Integer getPointsEarned() { return pointsEarned; }

    /** @param pointsEarned Los nuevos puntos obtenidos. */
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    /** @return El marcador real del equipo local. */
    public Integer getActualHomeScore() { return actualHomeScore; }

    /** @param actualHomeScore El nuevo marcador real del equipo local. */
    public void setActualHomeScore(Integer actualHomeScore) { this.actualHomeScore = actualHomeScore; }

    /** @return El marcador real del equipo visitante. */
    public Integer getActualAwayScore() { return actualAwayScore; }

    /** @param actualAwayScore El nuevo marcador real del equipo visitante. */
    public void setActualAwayScore(Integer actualAwayScore) { this.actualAwayScore = actualAwayScore; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PredictionDTO other = (PredictionDTO) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "PredictionDTO [id=" + id + ", username=" + username
                + ", matchId=" + matchId
                + ", predictedHomeScore=" + predictedHomeScore
                + ", predictedAwayScore=" + predictedAwayScore
                + ", status=" + status + ", pointsEarned=" + pointsEarned + "]";
    }
}