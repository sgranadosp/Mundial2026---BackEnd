/**
 * Paquete que contiene los DTOs específicos del módulo de álbum.
 */
package co.edu.unbosque.mundial2026.dto.album;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO con el resultado de abrir un paquete de láminas.
 * <p>
 * Devuelve las láminas obtenidas (incluyendo metadata útil para el frontend
 * como si era nueva o ya la tenía) y el progreso actualizado del álbum del
 * usuario. 
 * </p>
 */
@Schema(description = "Resultado de abrir un paquete: láminas obtenidas y progreso actualizado.")
public class OpenPackageResponseDTO {

    /**
     * Lista de láminas obtenidas en este paquete (típicamente 5).
     * Cada elemento ya viene con su estado actual (PEGADA si era nueva,
     * REPETIDA si ya la tenía).
     */
    private List<StickerSlotDTO> stickersObtained;

    /**
     * Total de láminas únicas que el usuario tiene en su álbum tras
     * abrir este paquete (ej. 127).
     */
    @Schema(example = "127")
    private long totalCollected;

    /**
     * Total de láminas únicas existentes en el catálogo (siempre 294 en
     * el catálogo actual del Mundial 2026).
     */
    @Schema(example = "294")
    private long totalCatalog;

    /** Constructor por defecto. */
    public OpenPackageResponseDTO() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param stickersObtained Lista de láminas obtenidas.
     * @param totalCollected   Total de láminas únicas que tiene el usuario.
     * @param totalCatalog     Total de láminas en el catálogo.
     */
    public OpenPackageResponseDTO(List<StickerSlotDTO> stickersObtained,
                                   long totalCollected, long totalCatalog) {
        this.stickersObtained = stickersObtained;
        this.totalCollected = totalCollected;
        this.totalCatalog = totalCatalog;
    }

    public List<StickerSlotDTO> getStickersObtained() { return stickersObtained; }
    public void setStickersObtained(List<StickerSlotDTO> stickersObtained) {
        this.stickersObtained = stickersObtained;
    }

    public long getTotalCollected() { return totalCollected; }
    public void setTotalCollected(long totalCollected) { this.totalCollected = totalCollected; }

    public long getTotalCatalog() { return totalCatalog; }
    public void setTotalCatalog(long totalCatalog) { this.totalCatalog = totalCatalog; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OpenPackageResponseDTO other = (OpenPackageResponseDTO) obj;
        return totalCollected == other.totalCollected
                && totalCatalog == other.totalCatalog
                && Objects.equals(stickersObtained, other.stickersObtained);
    }

    @Override
    public int hashCode() { return Objects.hash(stickersObtained, totalCollected, totalCatalog); }

    @Override
    public String toString() {
        return "OpenPackageResponseDTO [stickersObtained="
                + (stickersObtained != null ? stickersObtained.size() : 0)
                + ", totalCollected=" + totalCollected
                + ", totalCatalog=" + totalCatalog + "]";
    }
}