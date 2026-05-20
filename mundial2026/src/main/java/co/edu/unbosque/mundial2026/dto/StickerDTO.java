/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.Objects;

import co.edu.unbosque.mundial2026.model.Sticker.StickerCategory;
import co.edu.unbosque.mundial2026.model.Sticker.StickerRarity;
import co.edu.unbosque.mundial2026.model.Sticker.StickerStatus;

/**
 * Clase de Transferencia de Datos (DTO) para representar una lámina digital
 * dentro del álbum de un usuario en la plataforma Mundial 2026 Hub.
 * Se usa para listar el contenido del álbum, mostrar las láminas disponibles
 * para intercambio y para la respuesta de apertura de paquetes.
 * No se incluyen imágenes de personas reales; solo avatares y gráficos temáticos.
 */
public class StickerDTO {

    /**
     * Identificador único de la lámina en el álbum del usuario.
     */
    private Long id;

    /**
     * ID del álbum al que pertenece esta lámina.
     */
    private Long albumId;

    /**
     * Código único de la lámina en el catálogo (ej. "COL-01", "EST-05").
     */
    private String stickerCode;

    /**
     * Nombre descriptivo de la lámina.
     */
    private String displayName;

    /**
     * Categoría temática de la lámina: NATIONAL_TEAM, STADIUM, TROPHY o SPECIAL.
     */
    private StickerCategory category;

    /**
     * Rareza de la lámina: COMMON, UNCOMMON, RARE o LEGENDARY.
     */
    private StickerRarity rarity;

    /**
     * Estado de la lámina: PLACED, DUPLICATE o IN_EXCHANGE.
     */
    private StickerStatus status;

    /**
     * URL del avatar o gráfico temático de la lámina.
     */
    private String imageUrl;

    /**
     * Código ISO del equipo relacionado, si aplica.
     */
    private String teamIsoCode;

    /**
     * Constructor por defecto de {@code StickerDTO}.
     */
    public StickerDTO() {
    }

    /**
     * Constructor con los campos principales de la lámina.
     *
     * @param stickerCode Código de catálogo de la lámina.
     * @param displayName Nombre descriptivo.
     * @param category    Categoría temática.
     * @param rarity      Rareza.
     * @param status      Estado actual.
     */
    public StickerDTO(String stickerCode, String displayName,
                      StickerCategory category, StickerRarity rarity, StickerStatus status) {
        this.stickerCode = stickerCode;
        this.displayName = displayName;
        this.category = category;
        this.rarity = rarity;
        this.status = status;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID de la lámina. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID del álbum propietario. */
    public Long getAlbumId() { return albumId; }

    /** @param albumId El nuevo ID del álbum. */
    public void setAlbumId(Long albumId) { this.albumId = albumId; }

    /** @return El código de catálogo de la lámina. */
    public String getStickerCode() { return stickerCode; }

    /** @param stickerCode El nuevo código. */
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    /** @return El nombre descriptivo. */
    public String getDisplayName() { return displayName; }

    /** @param displayName El nuevo nombre. */
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /** @return La categoría temática. */
    public StickerCategory getCategory() { return category; }

    /** @param category La nueva categoría. */
    public void setCategory(StickerCategory category) { this.category = category; }

    /** @return La rareza de la lámina. */
    public StickerRarity getRarity() { return rarity; }

    /** @param rarity La nueva rareza. */
    public void setRarity(StickerRarity rarity) { this.rarity = rarity; }

    /** @return El estado actual de la lámina. */
    public StickerStatus getStatus() { return status; }

    /** @param status El nuevo estado. */
    public void setStatus(StickerStatus status) { this.status = status; }

    /** @return La URL del avatar o gráfico. */
    public String getImageUrl() { return imageUrl; }

    /** @param imageUrl La nueva URL. */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /** @return El código ISO del equipo relacionado. */
    public String getTeamIsoCode() { return teamIsoCode; }

    /** @param teamIsoCode El nuevo código ISO. */
    public void setTeamIsoCode(String teamIsoCode) { this.teamIsoCode = teamIsoCode; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StickerDTO other = (StickerDTO) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "StickerDTO [id=" + id + ", stickerCode=" + stickerCode
                + ", displayName=" + displayName + ", category=" + category
                + ", rarity=" + rarity + ", status=" + status + "]";
    }
}