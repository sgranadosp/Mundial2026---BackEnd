/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.unbosque.mundial2026.model.StickerExchange.ExchangeStatus;

/**
 * Clase de Transferencia de Datos (DTO) para representar una solicitud de
 * intercambio de láminas entre usuarios en la plataforma Mundial 2026 Hub.
 * El solicitante envía el {@code offeredStickerId} (lámina que ofrece) y el
 * {@code requestedStickerId} (lámina que quiere recibir). Ambas láminas deben
 * ser repetidas (estado {@code DUPLICATE}). El receptor confirma o rechaza.
 * El DTO incluye los datos mínimos de las láminas involucradas para que la UI
 * pueda mostrar el resumen del intercambio sin llamadas adicionales.
 */
public class StickerExchangeDTO {

    /**
     * Identificador único del intercambio.
     */
    private Long id;

    /**
     * ID del usuario que inicia el intercambio.
     */
    private Long requesterId;

    /**
     * Nombre de usuario del solicitante.
     */
    private String requesterUsername;

    /**
     * ID del usuario receptor de la solicitud.
     */
    private Long receiverId;

    /**
     * Nombre de usuario del receptor.
     */
    private String receiverUsername;

    /**
     * ID de la lámina que el solicitante ofrece.
     */
    private Long offeredStickerId;

    /**
     * Código de catálogo de la lámina ofrecida (para mostrar en la UI).
     */
    private String offeredStickerCode;

    /**
     * Nombre de la lámina ofrecida.
     */
    private String offeredStickerName;

    /**
     * ID de la lámina que el solicitante quiere recibir.
     */
    private Long requestedStickerId;

    /**
     * Código de catálogo de la lámina solicitada.
     */
    private String requestedStickerCode;

    /**
     * Nombre de la lámina solicitada.
     */
    private String requestedStickerName;

    /**
     * Estado actual del intercambio: PENDING, ACCEPTED, COMPLETED, REJECTED o CANCELLED.
     */
    private ExchangeStatus status;

    /**
     * Fecha y hora de creación de la solicitud.
     */
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de resolución del intercambio. Nulo si está pendiente.
     */
    private LocalDateTime resolvedAt;

    /**
     * Constructor por defecto de {@code StickerExchangeDTO}.
     */
    public StickerExchangeDTO() {
    }

    /**
     * Constructor con los campos mínimos para crear una solicitud.
     *
     * @param requesterId      ID del solicitante.
     * @param receiverId       ID del receptor.
     * @param offeredStickerId ID de la lámina ofrecida.
     * @param requestedStickerId ID de la lámina solicitada.
     */
    public StickerExchangeDTO(Long requesterId, Long receiverId,
                               Long offeredStickerId, Long requestedStickerId) {
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.offeredStickerId = offeredStickerId;
        this.requestedStickerId = requestedStickerId;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del intercambio. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID del solicitante. */
    public Long getRequesterId() { return requesterId; }

    /** @param requesterId El nuevo ID del solicitante. */
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    /** @return El nombre de usuario del solicitante. */
    public String getRequesterUsername() { return requesterUsername; }

    /** @param requesterUsername El nuevo nombre de usuario del solicitante. */
    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    /** @return El ID del receptor. */
    public Long getReceiverId() { return receiverId; }

    /** @param receiverId El nuevo ID del receptor. */
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    /** @return El nombre de usuario del receptor. */
    public String getReceiverUsername() { return receiverUsername; }

    /** @param receiverUsername El nuevo nombre de usuario del receptor. */
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }

    /** @return El ID de la lámina ofrecida. */
    public Long getOfferedStickerId() { return offeredStickerId; }

    /** @param offeredStickerId El nuevo ID de la lámina ofrecida. */
    public void setOfferedStickerId(Long offeredStickerId) { this.offeredStickerId = offeredStickerId; }

    /** @return El código de la lámina ofrecida. */
    public String getOfferedStickerCode() { return offeredStickerCode; }

    /** @param offeredStickerCode El nuevo código. */
    public void setOfferedStickerCode(String offeredStickerCode) {
        this.offeredStickerCode = offeredStickerCode;
    }

    /** @return El nombre de la lámina ofrecida. */
    public String getOfferedStickerName() { return offeredStickerName; }

    /** @param offeredStickerName El nuevo nombre. */
    public void setOfferedStickerName(String offeredStickerName) {
        this.offeredStickerName = offeredStickerName;
    }

    /** @return El ID de la lámina solicitada. */
    public Long getRequestedStickerId() { return requestedStickerId; }

    /** @param requestedStickerId El nuevo ID de la lámina solicitada. */
    public void setRequestedStickerId(Long requestedStickerId) {
        this.requestedStickerId = requestedStickerId;
    }

    /** @return El código de la lámina solicitada. */
    public String getRequestedStickerCode() { return requestedStickerCode; }

    /** @param requestedStickerCode El nuevo código. */
    public void setRequestedStickerCode(String requestedStickerCode) {
        this.requestedStickerCode = requestedStickerCode;
    }

    /** @return El nombre de la lámina solicitada. */
    public String getRequestedStickerName() { return requestedStickerName; }

    /** @param requestedStickerName El nuevo nombre. */
    public void setRequestedStickerName(String requestedStickerName) {
        this.requestedStickerName = requestedStickerName;
    }

    /** @return El estado del intercambio. */
    public ExchangeStatus getStatus() { return status; }

    /** @param status El nuevo estado. */
    public void setStatus(ExchangeStatus status) { this.status = status; }

    /** @return La fecha de creación. */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt La nueva fecha de creación. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return La fecha de resolución. */
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
        StickerExchangeDTO other = (StickerExchangeDTO) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "StickerExchangeDTO [id=" + id
                + ", requesterUsername=" + requesterUsername
                + ", receiverUsername=" + receiverUsername
                + ", offeredStickerCode=" + offeredStickerCode
                + ", requestedStickerCode=" + requestedStickerCode
                + ", status=" + status + "]";
    }
}