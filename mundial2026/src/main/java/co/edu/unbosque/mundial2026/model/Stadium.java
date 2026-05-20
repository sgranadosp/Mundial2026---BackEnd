/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa un estadio sede del Mundial 2026.
 * El Mundial 2026 se distribuye entre tres países anfitriones (EE. UU., Canadá
 * y México) con múltiples ciudades sede. Esta entidad almacena los datos de
 * cada estadio para que el usuario pueda filtrar partidos por sede, calcular
 * rutas y recibir notificaciones contextuales (zona horaria local).
 * Las coordenadas geográficas se utilizan junto con OpenStreetMap/Nominatim
 * para mostrar mapas en la agenda personalizada.
 */
@Entity
@Table(name = "stadiums")
public class Stadium {

    /**
     * Identificador único del estadio generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre oficial del estadio (ej. "MetLife Stadium").
     */
    @Column(unique = true)
    private String name;

    /**
     * Ciudad donde se ubica el estadio (ej. "East Rutherford", "Vancouver").
     */
    private String city;

    /**
     * País sede del estadio. Uno de los tres países anfitriones:
     * "USA", "Canada" o "Mexico".
     */
    private String country;

    /**
     * Zona horaria IANA del estadio (ej. "America/New_York", "America/Mexico_City").
     * Se usa para convertir horarios UTC a la hora local del estadio.
     */
    private String timezone;

    /**
     * Latitud geográfica del estadio en grados decimales.
     * Se usa para integración con OpenStreetMap.
     */
    private Double latitude;

    /**
     * Longitud geográfica del estadio en grados decimales.
     * Se usa para integración con OpenStreetMap.
     */
    private Double longitude;

    /**
     * Capacidad máxima de espectadores del estadio.
     */
    private Integer capacity;

    /**
     * URL de una imagen representativa del estadio para mostrar en la interfaz.
     * No se persiste el binario; se almacena solo la referencia.
     */
    private String imageUrl;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public Stadium() {
    }

    /**
     * Constructor con los datos principales del estadio.
     *
     * @param name      Nombre oficial del estadio.
     * @param city      Ciudad donde se ubica.
     * @param country   País anfitrión.
     * @param timezone  Zona horaria IANA.
     * @param latitude  Latitud geográfica.
     * @param longitude Longitud geográfica.
     * @param capacity  Capacidad máxima de espectadores.
     */
    public Stadium(String name, String city, String country, String timezone,
                   Double latitude, Double longitude, Integer capacity) {
        this.name = name;
        this.city = city;
        this.country = country;
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del estadio. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del estadio. */
    public void setId(Long id) { this.id = id; }

    /** @return El nombre oficial del estadio. */
    public String getName() { return name; }

    /** @param name El nuevo nombre del estadio. */
    public void setName(String name) { this.name = name; }

    /** @return La ciudad del estadio. */
    public String getCity() { return city; }

    /** @param city La nueva ciudad. */
    public void setCity(String city) { this.city = city; }

    /** @return El país anfitrión del estadio. */
    public String getCountry() { return country; }

    /** @param country El nuevo país. */
    public void setCountry(String country) { this.country = country; }

    /** @return La zona horaria IANA del estadio. */
    public String getTimezone() { return timezone; }

    /** @param timezone La nueva zona horaria IANA. */
    public void setTimezone(String timezone) { this.timezone = timezone; }

    /** @return La latitud geográfica del estadio. */
    public Double getLatitude() { return latitude; }

    /** @param latitude La nueva latitud. */
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    /** @return La longitud geográfica del estadio. */
    public Double getLongitude() { return longitude; }

    /** @param longitude La nueva longitud. */
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    /** @return La capacidad máxima de espectadores. */
    public Integer getCapacity() { return capacity; }

    /** @param capacity La nueva capacidad. */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    /** @return La URL de la imagen del estadio. */
    public String getImageUrl() { return imageUrl; }

    /** @param imageUrl La nueva URL de imagen. */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    /**
     * Compara estadios por ID y nombre.
     *
     * @param obj El objeto a comparar.
     * @return {@code true} si representan el mismo estadio.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Stadium other = (Stadium) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }

    /**
     * Genera el código hash basado en ID y nombre.
     *
     * @return Código hash del objeto.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    /**
     * Representación en cadena del estadio.
     *
     * @return Cadena con los atributos principales del estadio.
     */
    @Override
    public String toString() {
        return "Stadium [id=" + id + ", name=" + name + ", city=" + city
                + ", country=" + country + ", timezone=" + timezone
                + ", capacity=" + capacity + "]";
    }
}