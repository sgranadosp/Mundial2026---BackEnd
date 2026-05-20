/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.Objects;

/**
 * Clase de Transferencia de Datos (DTO) para representar la información
 * de una selección nacional participante en el Mundial 2026.
 * Se usa tanto para recibir datos desde la API externa de partidos
 * (football-data.org, API-Football o WireMock) como para exponer
 * la información de equipos al cliente a través de los endpoints REST.
 */
public class TeamDTO {

    /**
     * Identificador único interno del equipo.
     */
    private Long id;

    /**
     * Identificador del equipo en la API externa de datos deportivos.
     */
    private Long externalId;

    /**
     * Nombre completo de la selección (ej. "Colombia", "Brasil").
     */
    private String name;

    /**
     * Código ISO-3166 alfa-3 del país (ej. "COL", "BRA", "ARG").
     */
    private String isoCode;

    /**
     * Nombre corto o abreviado del equipo.
     */
    private String shortName;

    /**
     * URL del escudo o logotipo del equipo.
     */
    private String crestUrl;

    /**
     * Grupo de la fase de grupos al que pertenece el equipo (ej. "A", "B").
     * Nulo en fases eliminatorias.
     */
    private String group;

    /**
     * Constructor por defecto de {@code TeamDTO}.
     */
    public TeamDTO() {
    }

    /**
     * Constructor con los campos principales del equipo.
     *
     * @param externalId Identificador en la API externa.
     * @param name       Nombre completo de la selección.
     * @param isoCode    Código ISO-3166 alfa-3.
     * @param shortName  Nombre corto o alias.
     * @param crestUrl   URL del escudo.
     */
    public TeamDTO(Long externalId, String name, String isoCode, String shortName, String crestUrl) {
        this.externalId = externalId;
        this.name = name;
        this.isoCode = isoCode;
        this.shortName = shortName;
        this.crestUrl = crestUrl;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID interno del equipo. */
    public Long getId() { return id; }

    /** @param id El nuevo ID interno. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID del equipo en la API externa. */
    public Long getExternalId() { return externalId; }

    /** @param externalId El nuevo ID externo. */
    public void setExternalId(Long externalId) { this.externalId = externalId; }

    /** @return El nombre completo de la selección. */
    public String getName() { return name; }

    /** @param name El nuevo nombre. */
    public void setName(String name) { this.name = name; }

    /** @return El código ISO del país. */
    public String getIsoCode() { return isoCode; }

    /** @param isoCode El nuevo código ISO. */
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }

    /** @return El nombre corto del equipo. */
    public String getShortName() { return shortName; }

    /** @param shortName El nuevo nombre corto. */
    public void setShortName(String shortName) { this.shortName = shortName; }

    /** @return La URL del escudo. */
    public String getCrestUrl() { return crestUrl; }

    /** @param crestUrl La nueva URL del escudo. */
    public void setCrestUrl(String crestUrl) { this.crestUrl = crestUrl; }

    /** @return El grupo de fase de grupos. */
    public String getGroup() { return group; }

    /** @param group El nuevo grupo. */
    public void setGroup(String group) { this.group = group; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TeamDTO other = (TeamDTO) obj;
        return Objects.equals(id, other.id) && Objects.equals(externalId, other.externalId);
    }

    @Override
    public int hashCode() { return Objects.hash(id, externalId); }

    @Override
    public String toString() {
        return "TeamDTO [id=" + id + ", externalId=" + externalId
                + ", name=" + name + ", isoCode=" + isoCode
                + ", shortName=" + shortName + ", group=" + group + "]";
    }
}