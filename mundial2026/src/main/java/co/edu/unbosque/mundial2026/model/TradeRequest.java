package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa una solicitud de intercambio de láminas
 * creada por un usuario y dirigida a TODOS los usuarios con rol USER
 * (broadcast). Cualquier usuario elegible que cumpla las condiciones
 * puede aceptarla; el primero que lo haga consuma el intercambio y la
 * solicitud queda en estado {@code COMPLETED}.
 *
 * <h3>Reglas de negocio</h3>
 * <ul>
 *   <li>{@code offeredSticker}: lámina que el creador OFRECE. Debe tenerla
 *       como REPETIDA al momento de crear (y al momento de aceptar otro
 *       usuario; si el creador la pierde por otro intercambio, la solicitud
 *       se auto-cancela).</li>
 *   <li>{@code requestedSticker}: lámina que el creador PIDE. Debe estar
 *       como FALTANTE en su álbum al crear. Si el creador la obtiene por
 *       cualquier vía (pack o intercambio), la solicitud se auto-cancela.</li>
 *   <li>Un usuario puede tener máximo 5 solicitudes activas (status PENDING).</li>
 *   <li>Cuando un intercambio se COMPLETED, desaparece de las "solicitudes
 *       recibidas" de todos los demás usuarios (no solo del que aceptó).</li>
 * </ul>
 *
 * <h3>Estados ({@link Status})</h3>
 * <ul>
 *   <li>{@code PENDING}: activa, otros usuarios pueden aceptarla.</li>
 *   <li>{@code COMPLETED}: alguien la aceptó y se transfirieron las láminas.</li>
 *   <li>{@code CANCELLED}: el creador la canceló manualmente o el sistema
 *       la canceló porque las precondiciones dejaron de cumplirse.</li>
 * </ul>
 */
@Entity
@Table(name = "trade_requests",
       indexes = {
           @Index(name = "idx_trade_requests_creator",
                  columnList = "creator_id"),
           @Index(name = "idx_trade_requests_status",
                  columnList = "status"),
           @Index(name = "idx_trade_requests_creator_status",
                  columnList = "creator_id, status")
       })
public class TradeRequest {

    /** Identificador único interno. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que creó la solicitud (ofrece la lámina). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** Lámina que el creador OFRECE (debe tener quantity ≥ 2 al crear). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offered_sticker_id", nullable = false)
    private Sticker offeredSticker;

    /** Lámina que el creador PIDE (debe estar FALTANTE en su álbum). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_sticker_id", nullable = false)
    private Sticker requestedSticker;

    /** Estado actual de la solicitud. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /** Fecha de creación. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha en que la solicitud quedó cerrada (aceptada o cancelada).
     * {@code null} mientras esté PENDING.
     */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * Usuario que aceptó y completó el intercambio. {@code null} si la
     * solicitud no fue aceptada (PENDING o CANCELLED).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_id")
    private User acceptedBy;

    /**
     * Razón de cancelación cuando {@link #status} es CANCELLED.
     * Texto descriptivo, p. ej. "El creador ya no posee la lámina ofrecida".
     */
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    /**
     * Estados posibles de una solicitud de intercambio.
     */
    public enum Status {
        /** Activa: cualquier usuario elegible puede aceptarla. */
        PENDING,
        /** Aceptada y completada por otro usuario. */
        COMPLETED,
        /** Cancelada (por el creador o automáticamente). */
        CANCELLED
    }

    /** Constructor por defecto requerido por JPA. */
    public TradeRequest() {
    }

    /**
     * Construye una nueva solicitud en estado PENDING con fecha de creación
     * {@code LocalDateTime.now()}.
     */
    public TradeRequest(User creator, Sticker offered, Sticker requested) {
        this.creator = creator;
        this.offeredSticker = offered;
        this.requestedSticker = requested;
        this.status = Status.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Marca la solicitud como COMPLETED por el usuario {@code acceptedBy}.
     */
    public void markCompleted(User acceptedBy) {
        this.status = Status.COMPLETED;
        this.acceptedBy = acceptedBy;
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Marca la solicitud como CANCELLED con la razón indicada.
     */
    public void markCancelled(String reason) {
        this.status = Status.CANCELLED;
        this.cancelReason = reason;
        this.closedAt = LocalDateTime.now();
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Sticker getOfferedSticker() { return offeredSticker; }
    public void setOfferedSticker(Sticker offeredSticker) {
        this.offeredSticker = offeredSticker;
    }

    public Sticker getRequestedSticker() { return requestedSticker; }
    public void setRequestedSticker(Sticker requestedSticker) {
        this.requestedSticker = requestedSticker;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public User getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(User acceptedBy) { this.acceptedBy = acceptedBy; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TradeRequest other = (TradeRequest) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "TradeRequest [id=" + id
                + ", creatorId=" + (creator != null ? creator.getId() : null)
                + ", offers=" + (offeredSticker != null ? offeredSticker.getCode() : null)
                + ", wants=" + (requestedSticker != null ? requestedSticker.getCode() : null)
                + ", status=" + status + "]";
    }
}