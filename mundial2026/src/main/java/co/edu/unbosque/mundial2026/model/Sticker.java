/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.model;

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
 * Entidad JPA que representa una lámina digital (sticker) dentro del álbum
 * de un usuario en la plataforma Mundial 2026 Hub.
 * <p>
 * Cada lámina tiene metadatos (categoría, rareza, equipo o estadio asociado)
 * y un estado que indica si está pegada en el álbum o disponible para intercambio.
 * No se usan imágenes de personas reales; se usan avatares y gráficos temáticos
 * (restricción de negocio del proyecto).
 * </p>
 */
@Entity
@Table(name = "stickers")
public class Sticker {

    /**
     * Identificador único de la lámina en el álbum del usuario,
     * generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Álbum al que pertenece esta lámina.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    /**
     * Código único de la lámina en el catálogo del álbum oficial (ej. "COL-01", "EST-05").
     * Dos láminas del mismo usuario con el mismo {@code stickerCode} son repetidas.
     */
    private String stickerCode;

    /**
     * Nombre descriptivo de la lámina (ej. "Estadio MetLife", "Avatar Colombia").
     */
    private String displayName;

    /**
     * Categoría temática de la lámina, definida mediante el enum {@link StickerCategory}.
     */
    @Enumerated(EnumType.STRING)
    private StickerCategory category;

    /**
     * Rareza de la lámina, que determina su probabilidad de aparición en paquetes.
     * Las láminas más raras tienen mayor valor en intercambios.
     */
    @Enumerated(EnumType.STRING)
    private StickerRarity rarity;

    /**
     * Estado de la lámina en el álbum, definido mediante el enum {@link StickerStatus}.
     */
    @Enumerated(EnumType.STRING)
    private StickerStatus status;

    /**
     * URL del avatar o gráfico temático de la lámina.
     * No contiene imágenes de personas reales (restricción del proyecto).
     */
    private String imageUrl;

    /**
     * Código ISO del equipo relacionado con la lámina, si aplica.
     * Puede ser nulo para láminas de estadios u otras categorías.
     */
    private String teamIsoCode;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa la lámina como pegada en el álbum.
     */
    public Sticker() {
        this.status = StickerStatus.PLACED;
    }

    /**
     * Constructor con los datos principales de la lámina.
     *
     * @param album       Álbum propietario de la lámina.
     * @param stickerCode Código único de la lámina en el catálogo.
     * @param displayName Nombre descriptivo de la lámina.
     * @param category    Categoría temática.
     * @param rarity      Rareza de la lámina.
     */
    public Sticker(Album album, String stickerCode, String displayName,
                   StickerCategory category, StickerRarity rarity) {
        this();
        this.album = album;
        this.stickerCode = stickerCode;
        this.displayName = displayName;
        this.category = category;
        this.rarity = rarity;
    }

    /**
     * Enumeración que define las categorías temáticas de las láminas del álbum.
     */
    public enum StickerCategory {
        /** Láminas de selecciones nacionales (avatar del equipo). */
        NATIONAL_TEAM,
        /** Láminas de estadios sede del Mundial. */
        STADIUM,
        /** Láminas de trofeos o momentos icónicos. */
        TROPHY,
        /** Láminas especiales o de edición limitada. */
        SPECIAL
    }

    /**
     * Enumeración que define la rareza de una lámina y su probabilidad de aparición.
     */
    public enum StickerRarity {
        /** Lámina común, aparece frecuentemente en paquetes. */
        COMMON,
        /** Lámina poco común. */
        UNCOMMON,
        /** Lámina rara, baja probabilidad de aparición. */
        RARE,
        /** Lámina legendaria, muy baja probabilidad de aparición. */
        LEGENDARY
    }

    /**
     * Enumeración que define el estado de una lámina dentro del álbum del usuario.
     */
    public enum StickerStatus {
        /** La lámina está pegada en el álbum (es única para el usuario). */
        PLACED,
        /** La lámina es una repetida y está disponible para intercambio. */
        DUPLICATE,
        /** La lámina está en proceso de intercambio (bloqueada temporalmente). */
        IN_EXCHANGE
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID de la lámina. */
    public Long getId() { return id; }

    /** @param id El nuevo ID de la lámina. */
    public void setId(Long id) { this.id = id; }

    /** @return El álbum propietario de la lámina. */
    public Album getAlbum() { return album; }

    /** @param album El nuevo álbum propietario. */
    public void setAlbum(Album album) { this.album = album; }

    /** @return El código único de la lámina en el catálogo. */
    public String getStickerCode() { return stickerCode; }

    /** @param stickerCode El nuevo código de lámina. */
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    /** @return El nombre descriptivo de la lámina. */
    public String getDisplayName() { return displayName; }

    /** @param displayName El nuevo nombre descriptivo. */
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /** @return La categoría temática de la lámina. */
    public StickerCategory getCategory() { return category; }

    /** @param category La nueva categoría. */
    public void setCategory(StickerCategory category) { this.category = category; }

    /** @return La rareza de la lámina. */
    public StickerRarity getRarity() { return rarity; }

    /** @param rarity La nueva rareza. */
    public void setRarity(StickerRarity rarity) { this.rarity = rarity; }

    /** @return El estado actual de la lámina. */
    public StickerStatus getStatus() { return status; }

    /** @param status El nuevo estado de la lámina. */
    public void setStatus(StickerStatus status) { this.status = status; }

    /** @return La URL del avatar o gráfico de la lámina. */
    public String getImageUrl() { return imageUrl; }

    /** @param imageUrl La nueva URL de imagen. */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /** @return El código ISO del equipo relacionado con la lámina. */
    public String getTeamIsoCode() { return teamIsoCode; }

    /** @param teamIsoCode El nuevo código ISO del equipo. */
    public void setTeamIsoCode(String teamIsoCode) { this.teamIsoCode = teamIsoCode; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Sticker other = (Sticker) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Sticker [id=" + id + ", stickerCode=" + stickerCode
                + ", displayName=" + displayName + ", category=" + category
                + ", rarity=" + rarity + ", status=" + status + "]";
    }
}