package co.edu.unbosque.mundial2026.dto.trade;

import java.time.LocalDateTime;

import co.edu.unbosque.mundial2026.model.TradeRequest.Status;

/**
 * DTO de salida que representa una solicitud de intercambio con TODOS
 * los datos legibles que el front necesita para renderizar.
 *
 * <p>Incluye nombres y URLs de imágenes en lugar de solo IDs, para que el
 * front pueda mostrar mensajes amigables como
 * <em>"sgran te ofrece Argentina 03 a cambio de Colombia 01"</em>
 * sin tener que hacer llamadas adicionales para resolver los IDs de
 * usuario o lámina.</p>
 */
public class TradeRequestDTO {

    /** ID interno de la solicitud. */
    private Long id;

    /** ID del usuario creador. */
    private Long creatorId;

    /** Username del creador (para mostrar en la UI). */
    private String creatorUsername;

    // ─── Lámina ofrecida ───
    private Long offeredStickerId;
    private String offeredStickerCode;
    private String offeredStickerCountryName;
    private int offeredStickerPosition;
    private String offeredStickerImageUrl;

    // ─── Lámina pedida ───
    private Long requestedStickerId;
    private String requestedStickerCode;
    private String requestedStickerCountryName;
    private int requestedStickerPosition;
    private String requestedStickerImageUrl;

    /** Estado actual de la solicitud. */
    private Status status;

    /** Fecha de creación. */
    private LocalDateTime createdAt;

    public TradeRequestDTO() {}

    // ─── Getters / Setters ───
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public Long getOfferedStickerId() { return offeredStickerId; }
    public void setOfferedStickerId(Long offeredStickerId) {
        this.offeredStickerId = offeredStickerId;
    }

    public String getOfferedStickerCode() { return offeredStickerCode; }
    public void setOfferedStickerCode(String offeredStickerCode) {
        this.offeredStickerCode = offeredStickerCode;
    }

    public String getOfferedStickerCountryName() { return offeredStickerCountryName; }
    public void setOfferedStickerCountryName(String offeredStickerCountryName) {
        this.offeredStickerCountryName = offeredStickerCountryName;
    }

    public int getOfferedStickerPosition() { return offeredStickerPosition; }
    public void setOfferedStickerPosition(int offeredStickerPosition) {
        this.offeredStickerPosition = offeredStickerPosition;
    }

    public String getOfferedStickerImageUrl() { return offeredStickerImageUrl; }
    public void setOfferedStickerImageUrl(String offeredStickerImageUrl) {
        this.offeredStickerImageUrl = offeredStickerImageUrl;
    }

    public Long getRequestedStickerId() { return requestedStickerId; }
    public void setRequestedStickerId(Long requestedStickerId) {
        this.requestedStickerId = requestedStickerId;
    }

    public String getRequestedStickerCode() { return requestedStickerCode; }
    public void setRequestedStickerCode(String requestedStickerCode) {
        this.requestedStickerCode = requestedStickerCode;
    }

    public String getRequestedStickerCountryName() { return requestedStickerCountryName; }
    public void setRequestedStickerCountryName(String requestedStickerCountryName) {
        this.requestedStickerCountryName = requestedStickerCountryName;
    }

    public int getRequestedStickerPosition() { return requestedStickerPosition; }
    public void setRequestedStickerPosition(int requestedStickerPosition) {
        this.requestedStickerPosition = requestedStickerPosition;
    }

    public String getRequestedStickerImageUrl() { return requestedStickerImageUrl; }
    public void setRequestedStickerImageUrl(String requestedStickerImageUrl) {
        this.requestedStickerImageUrl = requestedStickerImageUrl;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}