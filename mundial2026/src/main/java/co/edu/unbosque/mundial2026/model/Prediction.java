/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidad JPA que representa el pronóstico de un usuario sobre un partido
 * dentro de un grupo de polla.
 * <p>
 * Un pronóstico registra el marcador que predice el usuario antes del inicio
 * del partido. El sistema bloquea cambios automáticamente cuando el partido
 * cambia a estado {@code LIVE} o {@code FINISHED}. Una vez concluido el
 * partido, el servicio de puntuación calcula los puntos obtenidos y los
 * almacena en {@code pointsEarned}.
 * </p>
 * <p>
 * La restricción de unicidad garantiza que cada usuario tenga como máximo
 * un pronóstico por partido dentro del mismo grupo.
 * </p>
 */
@Entity
@Table(
    name = "predictions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_prediction_user_match_group",
        columnNames = {"user_id", "match_id", "group_id"}
    )
)
public class Prediction {

    /**
     * Identificador único del pronóstico generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que realizó el pronóstico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Partido al que corresponde el pronóstico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    /**
     * Grupo de polla al que pertenece este pronóstico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private PollGroup pollGroup;

    /**
     * Goles predichos para el equipo local por el usuario.
     */
    private Integer predictedHomeScore;

    /**
     * Goles predichos para el equipo visitante por el usuario.
     */
    private Integer predictedAwayScore;

    /**
     * Fecha y hora en que se registró o modificó el pronóstico por última vez.
     * Se actualiza cada vez que el usuario edita su predicción.
     */
    private LocalDateTime submittedAt;

    /**
     * Estado actual del pronóstico, definido mediante el enum {@link PredictionStatus}.
     */
    @Enumerated(EnumType.STRING)
    private PredictionStatus status;

    /**
     * Puntos obtenidos por este pronóstico una vez calculados por el sistema.
     * Es nulo mientras el partido no haya finalizado.
     * <ul>
     *   <li>3 puntos si el usuario acertó el marcador exacto.</li>
     *   <li>1 punto si el usuario acertó el resultado (ganador o empate).</li>
     *   <li>0 puntos si no acertó ninguna de las dos cosas.</li>
     * </ul>
     */
    private Integer pointsEarned;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el estado como {@code OPEN} y registra la fecha de envío.
     */
    public Prediction() {
        this.status = PredictionStatus.OPEN;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * Constructor con los datos principales del pronóstico.
     *
     * @param user                Usuario que predice.
     * @param match               Partido sobre el que se predice.
     * @param pollGroup           Grupo de polla al que pertenece.
     * @param predictedHomeScore  Goles predichos para el equipo local.
     * @param predictedAwayScore  Goles predichos para el equipo visitante.
     */
    public Prediction(User user, Match match, PollGroup pollGroup,
                      Integer predictedHomeScore, Integer predictedAwayScore) {
        this();
        this.user = user;
        this.match = match;
        this.pollGroup = pollGroup;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
    }

    /**
     * Enumeración que define el estado del pronóstico en su ciclo de vida.
     */
    public enum PredictionStatus {
        /** El pronóstico puede ser modificado por el usuario. */
        OPEN,
        /** El partido inició; el pronóstico está bloqueado para edición. */
        LOCKED,
        /** El partido finalizó y el puntaje fue calculado. */
        EVALUATED
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del pronóstico. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del pronóstico. */
    public void setId(Long id) { this.id = id; }

    /** @return El usuario que realizó el pronóstico. */
    public User getUser() { return user; }

    /** @param user El nuevo usuario. */
    public void setUser(User user) { this.user = user; }

    /** @return El partido asociado al pronóstico. */
    public Match getMatch() { return match; }

    /** @param match El nuevo partido. */
    public void setMatch(Match match) { this.match = match; }

    /** @return El grupo de polla al que pertenece el pronóstico. */
    public PollGroup getPollGroup() { return pollGroup; }

    /** @param pollGroup El nuevo grupo de polla. */
    public void setPollGroup(PollGroup pollGroup) { this.pollGroup = pollGroup; }

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

    /** @param submittedAt La nueva fecha y hora de envío. */
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    /** @return El estado del pronóstico. */
    public PredictionStatus getStatus() { return status; }

    /** @param status El nuevo estado del pronóstico. */
    public void setStatus(PredictionStatus status) { this.status = status; }

    /** @return Los puntos obtenidos por este pronóstico. */
    public Integer getPointsEarned() { return pointsEarned; }

    /** @param pointsEarned Los nuevos puntos obtenidos. */
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    /**
     * Compara pronósticos por ID.
     *
     * @param obj El objeto a comparar.
     * @return {@code true} si representan el mismo pronóstico.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Prediction other = (Prediction) obj;
        return Objects.equals(id, other.id);
    }

    /**
     * Genera el código hash basado en el ID del pronóstico.
     *
     * @return Código hash del objeto.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Representación en cadena del pronóstico.
     *
     * @return Cadena con los atributos principales del pronóstico.
     */
    @Override
    public String toString() {
        return "Prediction [id=" + id
                + ", user=" + (user != null ? user.getUsername() : "null")
                + ", match=" + (match != null ? match.getId() : "null")
                + ", pollGroup=" + (pollGroup != null ? pollGroup.getName() : "null")
                + ", predictedHomeScore=" + predictedHomeScore
                + ", predictedAwayScore=" + predictedAwayScore
                + ", status=" + status
                + ", pointsEarned=" + pointsEarned + "]";
    }
}