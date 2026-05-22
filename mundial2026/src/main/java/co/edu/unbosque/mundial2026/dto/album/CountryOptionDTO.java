/**
 * Paquete que contiene los DTOs específicos del módulo de álbum.
 */
package co.edu.unbosque.mundial2026.dto.album;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO ligero para poblar el autocomplete de selecciones en el frontend.
 * Cada opción consta del código ASCII (usado en queries) y el nombre
 * legible (mostrado al usuario).  
 */
@Schema(description = "Opción del filtro de selección para el autocomplete del álbum.")
public class CountryOptionDTO {

    /**
     * Código ASCII de la selección (ej. {@code arabia_saudi}, {@code especial}).
     */
    @Schema(example = "arabia_saudi")
    private String code;

    /**
     * Nombre legible (ej. "Arabia Saudí", "Especiales").
     */
    @Schema(example = "Arabia Saudí")
    private String name;

    /** Constructor por defecto. */
    public CountryOptionDTO() {
    }

    /**
     * Constructor con código y nombre.
     *
     * @param code Código ASCII.
     * @param name Nombre legible.
     */
    public CountryOptionDTO(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CountryOptionDTO other = (CountryOptionDTO) obj;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() { return Objects.hash(code); }

    @Override
    public String toString() {
        return "CountryOptionDTO [code=" + code + ", name=" + name + "]";
    }
}