package co.edu.unbosque.mundial2026.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa una lámina del catálogo maestro del álbum
 * Mundial 2026 Hub.
 * <p>
 * El catálogo está formado por 294 láminas: 6 láminas por cada una de las
 * 48 selecciones nacionales clasificadas al Mundial (288 láminas) más una
 * página final de 6 láminas especiales (294 en total). Esta tabla es
 * fija y es la misma para todos los usuarios; lo que cambia por usuario es
 * cuáles posee, lo cual vive en {@link UserSticker}.
 * </p>
 * <p>
 * Cada lámina se identifica por un código único en formato
 * {@code <country_code>_NN} (por ejemplo {@code argentina_01},
 * {@code especial_06}), que también corresponde al nombre del archivo PNG
 * servido desde {@code /api/laminas/{code}.png}. 
 * </p>
 */
@Entity
@Table(name = "stickers",
       indexes = { @Index(name = "idx_sticker_country", columnList = "country_code") })
public class Sticker {

    /**
     * Identificador único interno de la lámina.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único de la lámina con formato {@code <country_code>_NN}.
     * Coincide con el nombre del archivo PNG (sin extensión).
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Código de la selección a la que pertenece la lámina, en formato
     * ASCII minúsculas con guiones bajos (ej. {@code argentina},
     * {@code arabia_saudi}, {@code especial}).
     */
    @Column(name = "country_code", nullable = false, length = 30)
    private String countryCode;

    /**
     * Nombre de la selección listo para mostrar al usuario, con tildes y
     * caracteres especiales (ej. "Argentina", "España", "Especiales").
     */
    @Column(name = "country_name", nullable = false, length = 50)
    private String countryName;

    /**
     * Posición de la lámina dentro de la página de su selección (1 a 6).
     */
    @Column(nullable = false)
    private int position;

    /**
     * Tipo de la lámina: escudo de la selección, jugador, o lámina especial.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StickerType type;

    /**
     * Tipos posibles de lámina en el álbum.
     */
    public enum StickerType {
        /** Escudo o bandera de la selección (típicamente la primera de la página). */
        CREST,
        /** Lámina de un jugador de la selección. */
        PLAYER,
        /** Lámina especial (página final de 6 láminas comunes a todos). */
        SPECIAL
    }

    /** Constructor por defecto requerido por JPA. */
    public Sticker() {
    }

    /**
     * Constructor con todos los campos funcionales.
     *
     * @param code        Código único {@code <country>_NN}.
     * @param countryCode Código de la selección en ASCII.
     * @param countryName Nombre de la selección listo para mostrar.
     * @param position    Posición de la lámina (1 a 6).
     * @param type        Tipo de la lámina.
     */
    public Sticker(String code, String countryCode, String countryName, int position, StickerType type) {
        this.code = code;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.position = position;
        this.type = type;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID interno. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El código único de la lámina. */
    public String getCode() { return code; }

    /** @param code El nuevo código. */
    public void setCode(String code) { this.code = code; }

    /** @return El código ASCII de la selección. */
    public String getCountryCode() { return countryCode; }

    /** @param countryCode El nuevo código de selección. */
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    /** @return El nombre de la selección para mostrar. */
    public String getCountryName() { return countryName; }

    /** @param countryName El nuevo nombre de selección. */
    public void setCountryName(String countryName) { this.countryName = countryName; }

    /** @return La posición de la lámina en su página. */
    public int getPosition() { return position; }

    /** @param position La nueva posición. */
    public void setPosition(int position) { this.position = position; }

    /** @return El tipo de lámina. */
    public StickerType getType() { return type; }

    /** @param type El nuevo tipo. */
    public void setType(StickerType type) { this.type = type; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Sticker other = (Sticker) obj;
        return Objects.equals(id, other.id) && Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() { return Objects.hash(id, code); }

    @Override
    public String toString() {
        return "Sticker [id=" + id + ", code=" + code + ", countryName=" + countryName
                + ", position=" + position + ", type=" + type + "]";
    }
}