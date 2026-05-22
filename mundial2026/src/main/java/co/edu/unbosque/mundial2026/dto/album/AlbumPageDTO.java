/**
 * Paquete que contiene los DTOs específicos del módulo de álbum.
 */
package co.edu.unbosque.mundial2026.dto.album;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO que representa una página completa del álbum para una selección
 * (o la página de Especiales).
 * <p>
 * Una página siempre tiene 6 casillas correspondientes a las 6 láminas
 * de esa selección. Las casillas se devuelven ya ordenadas por posición y
 * con el estado de posesión del usuario, listas para que el frontend las
 * pinte directamente. 
 * </p>
 */
@Schema(description = "Página del álbum: datos de la selección + 6 casillas con su estado.")
public class AlbumPageDTO {

    /**
     * Código ASCII de la selección (ej. {@code argentina}, {@code especial}).
     */
    @Schema(example = "argentina")
    private String countryCode;

    /**
     * Nombre de la selección listo para mostrar (ej. "Argentina").
     */
    @Schema(example = "Argentina")
    private String countryName;

    /**
     * Las 6 casillas de esta página, ordenadas por posición (1 a 6).
     * Si se aplica un filtro (PEGADA / REPETIDA / FALTANTE), la lista
     * solo contiene las casillas que cumplen el filtro y puede tener
     * menos de 6 elementos.
     */
    private List<StickerSlotDTO> slots;

    /** Constructor por defecto. */
    public AlbumPageDTO() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param countryCode Código ASCII de la selección.
     * @param countryName Nombre de la selección.
     * @param slots       Lista de casillas.
     */
    public AlbumPageDTO(String countryCode, String countryName, List<StickerSlotDTO> slots) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.slots = slots;
    }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }

    public List<StickerSlotDTO> getSlots() { return slots; }
    public void setSlots(List<StickerSlotDTO> slots) { this.slots = slots; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AlbumPageDTO other = (AlbumPageDTO) obj;
        return Objects.equals(countryCode, other.countryCode);
    }

    @Override
    public int hashCode() { return Objects.hash(countryCode); }

    @Override
    public String toString() {
        return "AlbumPageDTO [countryCode=" + countryCode + ", countryName=" + countryName
                + ", slots=" + (slots != null ? slots.size() : 0) + "]";
    }
}