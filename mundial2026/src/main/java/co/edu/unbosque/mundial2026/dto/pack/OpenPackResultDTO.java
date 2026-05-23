package co.edu.unbosque.mundial2026.dto.pack;

import java.util.List;

import co.edu.unbosque.mundial2026.dto.album.StickerSlotDTO;
import co.edu.unbosque.mundial2026.model.StickerPack.Origin;

/**
 * DTO de respuesta del endpoint POST /packs/me/open?origin=...
 *
 * Devuelve las 5 láminas obtenidas (con su estado actualizado tras la
 * apertura, ya con quantity ≥ 2 si era duplicada) más el inventario
 * de packs pendientes refrescado para que el front no tenga que
 * hacer una segunda llamada.
 */
public class OpenPackResultDTO {

    /** Origen del pack que se acaba de abrir (WELCOME/DAILY/POLL). */
    private Origin origin;

    /** Las 5 láminas obtenidas en este pack. */
    private List<StickerSlotDTO> stickersObtained;

    /** Inventario de packs pendientes después de la apertura. */
    private PackInventoryDTO inventory;

    public OpenPackResultDTO() {}

    public OpenPackResultDTO(Origin origin, List<StickerSlotDTO> stickersObtained,
                              PackInventoryDTO inventory) {
        this.origin = origin;
        this.stickersObtained = stickersObtained;
        this.inventory = inventory;
    }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }

    public List<StickerSlotDTO> getStickersObtained() { return stickersObtained; }
    public void setStickersObtained(List<StickerSlotDTO> stickersObtained) {
        this.stickersObtained = stickersObtained;
    }

    public PackInventoryDTO getInventory() { return inventory; }
    public void setInventory(PackInventoryDTO inventory) { this.inventory = inventory; }
}