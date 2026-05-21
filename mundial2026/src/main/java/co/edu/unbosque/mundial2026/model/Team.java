package co.edu.unbosque.mundial2026.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
 
/**
 * Entidad JPA que representa una selección nacional participante
 * en el Mundial 2026.
 * <p>
 * Almacena los datos básicos del equipo que se consumen desde la
 * API externa (football-data.org o WireMock) y se replican localmente
 * para evitar dependencia continua del proveedor externo.
 * El campo {@code externalId} corresponde al identificador del equipo
 * en la API seleccionada y se usa para sincronización.
 * </p>
 */
@Entity
@Table(name = "teams")
public class Team {
 
    /**
     * Identificador único del equipo generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    /**
     * Identificador del equipo en la API externa de datos deportivos.
     * Se usa para sincronizar actualizaciones desde el proveedor.
     */
    @Column(unique = true)
    private Long externalId;
 
    /**
     * Nombre completo de la selección (ej. "Colombia", "Brasil").
     */
    private String name;
 
    /**
     * Código ISO-3166 alfa-3 del país (ej. "COL", "BRA", "ARG").
     * Se usa como referencia corta en preferencias de usuario.
     */
    @Column(unique = true, length = 3)
    private String isoCode;
 
    /**
     * Nombre corto o abreviado del equipo (ej. "COL", "ENG").
     * Puede coincidir con el código ISO o ser un alias de la API.
     */
    private String shortName;
 
    /**
     * URL del escudo o logotipo del equipo proporcionado por la API externa.
     * Se almacena como referencia para el frontend; no se persiste el binario.
     */
    private String crestUrl;
 
    /**
     * Grupo de la fase de grupos al que pertenece el equipo (ej. "A", "B").
     * Puede ser nulo si el equipo ya avanzó a eliminatorias directas.
     * <p>
     * Se mapea a la columna {@code team_group} porque {@code GROUP} es una
     * palabra reservada en MySQL y causaría error en la creación de la tabla.
     * </p>
     */
    @Column(name = "team_group")
    private String group;
 
    /**
     * Constructor por defecto requerido por JPA.
     */
    public Team() {
    }
 
    /**
     * Constructor con los datos principales del equipo.
     *
     * @param externalId Identificador en la API externa.
     * @param name       Nombre completo de la selección.
     * @param isoCode    Código ISO-3166 alfa-3 del país.
     * @param shortName  Nombre corto o alias del equipo.
     * @param crestUrl   URL del escudo del equipo.
     */
    public Team(Long externalId, String name, String isoCode, String shortName, String crestUrl) {
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
 
    /** @param name El nuevo nombre completo. */
    public void setName(String name) { this.name = name; }
 
    /** @return El código ISO del país. */
    public String getIsoCode() { return isoCode; }
 
    /** @param isoCode El nuevo código ISO. */
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
 
    /** @return El nombre corto del equipo. */
    public String getShortName() { return shortName; }
 
    /** @param shortName El nuevo nombre corto. */
    public void setShortName(String shortName) { this.shortName = shortName; }
 
    /** @return La URL del escudo del equipo. */
    public String getCrestUrl() { return crestUrl; }
 
    /** @param crestUrl La nueva URL del escudo. */
    public void setCrestUrl(String crestUrl) { this.crestUrl = crestUrl; }
 
    /** @return El grupo de fase de grupos del equipo. */
    public String getGroup() { return group; }
 
    /** @param group El nuevo grupo asignado. */
    public void setGroup(String group) { this.group = group; }
 
    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================
 
    /**
     * Compara equipos por ID interno e ID externo.
     *
     * @param obj El objeto a comparar.
     * @return {@code true} si representan el mismo equipo.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Team other = (Team) obj;
        return Objects.equals(id, other.id) && Objects.equals(externalId, other.externalId);
    }
 
    /**
     * Genera el código hash basado en ID interno e ID externo.
     *
     * @return Código hash del objeto.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, externalId);
    }
 
    /**
     * Representación en cadena del equipo.
     *
     * @return Cadena con los atributos principales del equipo.
     */
    @Override
    public String toString() {
        return "Team [id=" + id + ", externalId=" + externalId + ", name=" + name
                + ", isoCode=" + isoCode + ", shortName=" + shortName + ", group=" + group + "]";
    }
}