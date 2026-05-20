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

/**
 * Entidad JPA que representa una solicitud de intercambio de láminas entre
 * dos usuarios en la plataforma Mundial 2026 Hub.
 * <p>
 * El intercambio puede ser directo (usuario A le ofrece una lámina a usuario B
 * a cambio de otra específica) o mediado por puntos de intercambio acumulados.
 * El sistema requiere confirmación de ambas partes para completar el intercambio
 * (garantizando no repudio). Las láminas involucradas quedan en estado
 * {@code IN_EXCHANGE} durante el proceso y se transfieren o liberan al resolverse.
 * </p>
 * <p>
 * Se establecen límites de intercambio por usuario para prevenir automatización
 * masiva (restricción de negocio del proyecto).
 * </p>
 */
@Entity
@Table(name = "sticker_exchanges")
public class StickerExchange {

    /**
     * Identificador único del intercambio generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que inicia la solicitud de intercambio (quien ofrece su lámina).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    /**
     * Usuario que recibe la solicitud de intercambio (a quien se le pide la lámina).
     * Puede ser nulo en intercambios abiertos (el sistema asigna al receptor).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    /**
     * Lámina que el usuario solicitante ofrece en el intercambio.
     * Debe tener estado {@code DUPLICATE} al momento de crear la solicitud.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_sticker_id")
    private Sticker offeredSticker;

    /**
     * Lámina que el usuario solicitante desea recibir a cambio.
     * Debe pertenecer al álbum del receptor y tener estado {@code DUPLICATE}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_sticker_id")
    private Sticker requestedSticker;

    /**
     * Estado actual del intercambio, definido mediante el enum {@link ExchangeStatus}.
     */
    @Enumerated(EnumType.STRING)
    private ExchangeStatus status;

    /**
     * Fecha y hora en que se creó la solicitud de intercambio.
     */
    private LocalDateTime createdAt;

    /**
     * Fecha y hora en que el intercambio fue resuelto (completado, rechazado o cancelado).
     * Es nulo mientras el intercambio esté pendiente.
     */
    private LocalDateTime resolvedAt;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el estado como {@code PENDING} y registra la fecha de creación.
     */
    public StickerExchange() {
        this.status = ExchangeStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con los datos principales del intercambio.
     *
     * @param requester       Usuario que inicia el intercambio.
     * @param receiver        Usuario que recibe la solicitud.
     * @param offeredSticker  Lámina ofrecida por el solicitante.
     * @param requestedSticker Lámina solicitada al receptor.
     */
    public StickerExchange(User requester, User receiver,
                           Sticker offeredSticker, Sticker requestedSticker) {
        this();
        this.requester = requester;
        this.receiver = receiver;
        this.offeredSticker = offeredSticker;
        this.requestedSticker = requestedSticker;
    }

    /**
     * Enumeración que define el estado de ciclo de vida de un intercambio.
     */
    public enum ExchangeStatus {
        /** El intercambio fue creado y está esperando confirmación del receptor. */
        PENDING,
        /** El receptor aceptó el intercambio; está en proceso de transferencia. */
        ACCEPTED,
        /** El intercambio fue completado: las láminas fueron transferidas. */
        COMPLETED,
        /** El receptor rechazó la solicitud de intercambio. */
        REJECTED,
        /** El solicitante canceló la solicitud antes de que fuera resuelta. */
        CANCELLED
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del intercambio. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del intercambio. */
    public void setId(Long id) { this.id = id; }

    /** @return El usuario que inició el intercambio. */
    public User getRequester() { return requester; }

    /** @param requester El nuevo usuario solicitante. */
    public void setRequester(User requester) { this.requester = requester; }

    /** @return El usuario receptor de la solicitud. */
    public User getReceiver() { return receiver; }

    /** @param receiver El nuevo usuario receptor. */
    public void setReceiver(User receiver) { this.receiver = receiver; }

    /** @return La lámina ofrecida por el solicitante. */
    public Sticker getOfferedSticker() { return offeredSticker; }

    /** @param offeredSticker La nueva lámina ofrecida. */
    public void setOfferedSticker(Sticker offeredSticker) { this.offeredSticker = offeredSticker; }

    /** @return La lámina solicitada al receptor. */
    public Sticker getRequestedSticker() { return requestedSticker; }

    /** @param requestedSticker La nueva lámina solicitada. */
    public void setRequestedSticker(Sticker requestedSticker) { this.requestedSticker = requestedSticker; }

    /** @return El estado actual del intercambio. */
    public ExchangeStatus getStatus() { return status; }

    /** @param status El nuevo estado del intercambio. */
    public void setStatus(ExchangeStatus status) { this.status = status; }

    /** @return La fecha y hora de creación de la solicitud. */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt La nueva fecha de creación. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return La fecha y hora de resolución del intercambio. */
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    /** @param resolvedAt La nueva fecha de resolución. */
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StickerExchange other = (StickerExchange) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "StickerExchange [id=" + id
                + ", requester=" + (requester != null ? requester.getUsername() : "null")
                + ", receiver=" + (receiver != null ? receiver.getUsername() : "null")
                + ", status=" + status
                + ", createdAt=" + createdAt + ", resolvedAt=" + resolvedAt + "]";
    }
}