/**
 * Paquete que contiene los DTOs específicos del módulo de álbum.
 */
package co.edu.unbosque.mundial2026.dto.album;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO que representa una casilla individual del álbum de un usuario:
 * una posición fija dentro de la página de una selección, con su lámina
 * correspondiente y el estado de posesión del usuario.
 * <p>
 * Las láminas faltantes se devuelven con {@code status = FALTANTE} y
 * {@code quantity = 0}, sin {@code imageUrl}, para que el frontend pueda
 * pintar un recuadro vacío sin solicitar la imagen. 
 * </p>
 */
@Schema(description = "Una casilla del álbum con su estado para el usuario actual.")
public class StickerSlotDTO {

    /**
     * Código único de la lámina (ej. {@code argentina_01}).
     */
    @Schema(example = "argentina_01")
    private String code;

    /**
     * Posición de la lámina dentro de la página (1 a 6).
     */
    @Schema(example = "1")
    private int position;

    /**
     * URL del archivo PNG servida desde el backend. Es {@code null} cuando
     * la lámina está faltante, para que el frontend no intente cargarla.
     */
    @Schema(example = "/api/laminas/argentina_01.png", nullable = true)
    private String imageUrl;

    /**
     * Estado de posesión: {@code PEGADA}, {@code REPETIDA} o {@code FALTANTE}.
     */
    @Schema(example = "PEGADA")
    private Status status;

    /**
     * Cantidad de copias que el usuario tiene de esta lámina.
     * {@code 0} si está faltante; {@code 1} si está pegada sin repetir;
     * {@code ≥ 2} si tiene copias para intercambio.
     */
    @Schema(example = "1")
    private int quantity;

    /**
     * Posibles estados de una casilla del álbum.
     */
    public enum Status {
        /** El usuario tiene exactamente una copia de la lámina. */
        PEGADA,
        /** El usuario tiene dos o más copias de la lámina. */
        REPETIDA,
        /** El usuario no tiene la lámina. */
        FALTANTE
    }

    /** Constructor por defecto. */
    public StickerSlotDTO() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param code     Código de la lámina.
     * @param position Posición (1 a 6).
     * @param imageUrl URL de la imagen (o {@code null} si faltante).
     * @param status   Estado de posesión.
     * @param quantity Cantidad de copias.
     */
    public StickerSlotDTO(String code, int position, String imageUrl, Status status, int quantity) {
        this.code = code;
        this.position = position;
        this.imageUrl = imageUrl;
        this.status = status;
        this.quantity = quantity;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StickerSlotDTO other = (StickerSlotDTO) obj;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() { return Objects.hash(code); }

    @Override
    public String toString() {
        return "StickerSlotDTO [code=" + code + ", position=" + position
                + ", status=" + status + ", quantity=" + quantity + "]";
    }
}