package co.edu.unbosque.mundial2026.dto.pack;

import java.time.LocalDateTime;

import co.edu.unbosque.mundial2026.model.StickerPack.Origin;

/**
 * DTO de una fila del historial de packs (GET /packs/me/history).
 *
 * Representa un pack que el sistema otorgó al usuario, indicando cuándo,
 * por qué (origen) y si ya fue abierto.
 */
public class PackHistoryEntryDTO {

    /** ID interno del pack (para referencia). */
    private Long id;

    /** Origen del pack: WELCOME / DAILY / POLL. */
    private Origin origin;

    /** Fecha en que el sistema otorgó el pack. */
    private LocalDateTime grantedAt;

    /** {@code true} si el usuario ya lo abrió. */
    private boolean opened;

    /** Fecha en que se abrió (null si todavía está pendiente). */
    private LocalDateTime openedAt;

    public PackHistoryEntryDTO() {}

    public PackHistoryEntryDTO(Long id, Origin origin, LocalDateTime grantedAt,
                                boolean opened, LocalDateTime openedAt) {
        this.id = id;
        this.origin = origin;
        this.grantedAt = grantedAt;
        this.opened = opened;
        this.openedAt = openedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public boolean isOpened() { return opened; }
    public void setOpened(boolean opened) { this.opened = opened; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
}