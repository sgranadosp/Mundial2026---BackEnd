/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.Objects;

/**
 * Clase de Transferencia de Datos (DTO) para representar la información
 * de un estadio sede del Mundial 2026.
 * Expone los datos del estadio al cliente incluyendo coordenadas geográficas
 * para integración con OpenStreetMap/Nominatim, y la zona horaria IANA
 * para que el frontend pueda convertir horarios UTC a la hora local del estadio.
 */
public class StadiumDTO {

    /**
     * Identificador único del estadio.
     */
    private Long id;

    /**
     * Nombre oficial del estadio (ej. "MetLife Stadium").
     */
    private String name;

    /**
     * Ciudad donde se ubica el estadio.
     */
    private String city;

    /**
     * País anfitrión del estadio ("USA", "Canada" o "Mexico").
     */
    private String country;

    /**
     * Zona horaria IANA del estadio (ej. "America/New_York").
     */
    private String timezone;

    /**
     * Latitud geográfica del estadio en grados decimales.
     */
    private Double latitude;

    /**
     * Longitud geográfica del estadio en grados decimales.
     */
    private Double longitude;

    /**
     * Capacidad máxima de espectadores del estadio.
     */
    private Integer capacity;

    /**
     * URL de una imagen representativa del estadio.
     */
    private String imageUrl;

    /**
     * Constructor por defecto de {@code StadiumDTO}.
     */
    public StadiumDTO() {
    }

    /**
     * Constructor con los campos principales del estadio.
     *
     * @param name      Nombre oficial del estadio.
     * @param city      Ciudad donde se ubica.
     * @param country   País anfitrión.
     * @param timezone  Zona horaria IANA.
     * @param latitude  Latitud geográfica.
     * @param longitude Longitud geográfica.
     * @param capacity  Capacidad máxima.
     */
    public StadiumDTO(String name, String city, String country, String timezone,
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

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El nombre oficial. */
    public String getName() { return name; }

    /** @param name El nuevo nombre. */
    public void setName(String name) { this.name = name; }

    /** @return La ciudad. */
    public String getCity() { return city; }

    /** @param city La nueva ciudad. */
    public void setCity(String city) { this.city = city; }

    /** @return El país anfitrión. */
    public String getCountry() { return country; }

    /** @param country El nuevo país. */
    public void setCountry(String country) { this.country = country; }

    /** @return La zona horaria IANA. */
    public String getTimezone() { return timezone; }

    /** @param timezone La nueva zona horaria. */
    public void setTimezone(String timezone) { this.timezone = timezone; }

    /** @return La latitud geográfica. */
    public Double getLatitude() { return latitude; }

    /** @param latitude La nueva latitud. */
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    /** @return La longitud geográfica. */
    public Double getLongitude() { return longitude; }

    /** @param longitude La nueva longitud. */
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    /** @return La capacidad máxima. */
    public Integer getCapacity() { return capacity; }

    /** @param capacity La nueva capacidad. */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    /** @return La URL de imagen del estadio. */
    public String getImageUrl() { return imageUrl; }

    /** @param imageUrl La nueva URL de imagen. */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StadiumDTO other = (StadiumDTO) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, name); }

    @Override
    public String toString() {
        return "StadiumDTO [id=" + id + ", name=" + name + ", city=" + city
                + ", country=" + country + ", timezone=" + timezone
                + ", capacity=" + capacity + "]";
    }
}