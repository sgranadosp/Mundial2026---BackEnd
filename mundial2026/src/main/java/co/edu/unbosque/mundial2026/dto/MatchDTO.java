/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.unbosque.mundial2026.model.Match.DataStatus;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.Match.Phase;

/**
 * Clase de Transferencia de Datos (DTO) para representar la información
 * de un partido del Mundial 2026.
 * En lugar de exponer las entidades {@link co.edu.unbosque.mundial2026.model.Team}
 * y {@link co.edu.unbosque.mundial2026.model.Stadium} completas, este DTO
 * incluye los datos mínimos de cada uno (nombre, código, ciudad) para evitar
 * carga innecesaria. El frontend puede solicitar los detalles completos de
 * equipo o estadio por separado si los necesita.
 * El campo {@code dataStatus} se incluye en la respuesta para que el cliente
 * pueda mostrar un indicador visual cuando el dato es provisional
 * ("actualización pendiente"), implementando el principio de información confiable.
 */
public class MatchDTO {

    /**
     * Identificador único interno del partido.
     */
    private Long id;

    /**
     * Identificador del partido en la API externa.
     */
    private Long externalId;

    /**
     * ID interno del equipo local.
     */
    private Long homeTeamId;

    /**
     * Nombre completo del equipo local.
     */
    private String homeTeamName;

    /**
     * Código ISO del equipo local (ej. "COL").
     */
    private String homeTeamIsoCode;

    /**
     * URL del escudo del equipo local.
     */
    private String homeTeamCrestUrl;

    /**
     * ID interno del equipo visitante.
     */
    private Long awayTeamId;

    /**
     * Nombre completo del equipo visitante.
     */
    private String awayTeamName;

    /**
     * Código ISO del equipo visitante.
     */
    private String awayTeamIsoCode;

    /**
     * URL del escudo del equipo visitante.
     */
    private String awayTeamCrestUrl;

    /**
     * ID interno del estadio.
     */
    private Long stadiumId;

    /**
     * Nombre del estadio.
     */
    private String stadiumName;

    /**
     * Ciudad del estadio.
     */
    private String stadiumCity;

    /**
     * Zona horaria IANA del estadio para conversión de horarios en el cliente.
     */
    private String stadiumTimezone;

    /**
     * Latitud del estadio (WGS84). Usada por el cliente para renderizar el
     * mapa de ubicación en la vista de detalle del partido.
     */
    private Double stadiumLatitude;

    /**
     * Longitud del estadio (WGS84). Usada por el cliente para renderizar el
     * mapa de ubicación en la vista de detalle del partido.
     */
    private Double stadiumLongitude;

    /**
     * Capacidad del estadio. Puede ser {@code null} si no se conoce.
     */
    private Integer stadiumCapacity;

    /**
     * URL de la imagen del estadio. Puede ser {@code null}.
     */
    private String stadiumImageUrl;

    /**
     * Fecha y hora programada del partido en UTC.
     * <p>
     * Se serializa con sufijo {@code 'Z'} explícito (ej.
     * {@code "2026-06-12T02:00:00Z"}) para que el cliente JavaScript la
     * interprete inequívocamente como UTC al hacer {@code new Date(...)}.
     * Sin la {@code Z}, los navegadores interpretan el string como hora
     * local, causando desfases de zona horaria visibles en la UI.
     * </p>
     */
    @com.fasterxml.jackson.annotation.JsonFormat(
            shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
            timezone = "UTC")
    private LocalDateTime scheduledAt;

    /**
     * Fase del torneo del partido.
     */
    private Phase phase;

    /**
     * Estado actual del partido en su ciclo de vida.
     */
    private MatchStatus status;

    /**
     * Goles del equipo local. Nulo si el partido no ha finalizado.
     */
    private Integer homeScore;

    /**
     * Goles del equipo visitante. Nulo si el partido no ha finalizado.
     */
    private Integer awayScore;

    /**
     * Nombre del grupo en fase de grupos. Nulo en eliminatorias.
     */
    private String groupName;

    /**
     * Número de jornada del partido.
     */
    private Integer matchday;

    /**
     * Estado del dato: CONFIRMED, PENDING_UPDATE o CONFLICT.
     * El cliente muestra un indicador visual si no es CONFIRMED.
     */
    private DataStatus dataStatus;

    /**
     * Constructor por defecto de {@code MatchDTO}.
     */
    public MatchDTO() {
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID interno del partido. */
    public Long getId() { return id; }

    /** @param id El nuevo ID interno. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID en la API externa. */
    public Long getExternalId() { return externalId; }

    /** @param externalId El nuevo ID externo. */
    public void setExternalId(Long externalId) { this.externalId = externalId; }

    /** @return El ID del equipo local. */
    public Long getHomeTeamId() { return homeTeamId; }

    /** @param homeTeamId El nuevo ID del equipo local. */
    public void setHomeTeamId(Long homeTeamId) { this.homeTeamId = homeTeamId; }

    /** @return El nombre del equipo local. */
    public String getHomeTeamName() { return homeTeamName; }

    /** @param homeTeamName El nuevo nombre del equipo local. */
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    /** @return El código ISO del equipo local. */
    public String getHomeTeamIsoCode() { return homeTeamIsoCode; }

    /** @param homeTeamIsoCode El nuevo código ISO del equipo local. */
    public void setHomeTeamIsoCode(String homeTeamIsoCode) { this.homeTeamIsoCode = homeTeamIsoCode; }

    /** @return La URL del escudo del equipo local. */
    public String getHomeTeamCrestUrl() { return homeTeamCrestUrl; }

    /** @param homeTeamCrestUrl La nueva URL del escudo del equipo local. */
    public void setHomeTeamCrestUrl(String homeTeamCrestUrl) { this.homeTeamCrestUrl = homeTeamCrestUrl; }

    /** @return El ID del equipo visitante. */
    public Long getAwayTeamId() { return awayTeamId; }

    /** @param awayTeamId El nuevo ID del equipo visitante. */
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    /** @return El nombre del equipo visitante. */
    public String getAwayTeamName() { return awayTeamName; }

    /** @param awayTeamName El nuevo nombre del equipo visitante. */
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

    /** @return El código ISO del equipo visitante. */
    public String getAwayTeamIsoCode() { return awayTeamIsoCode; }

    /** @param awayTeamIsoCode El nuevo código ISO del equipo visitante. */
    public void setAwayTeamIsoCode(String awayTeamIsoCode) { this.awayTeamIsoCode = awayTeamIsoCode; }

    /** @return La URL del escudo del equipo visitante. */
    public String getAwayTeamCrestUrl() { return awayTeamCrestUrl; }

    /** @param awayTeamCrestUrl La nueva URL del escudo del equipo visitante. */
    public void setAwayTeamCrestUrl(String awayTeamCrestUrl) { this.awayTeamCrestUrl = awayTeamCrestUrl; }

    /** @return El ID del estadio. */
    public Long getStadiumId() { return stadiumId; }

    /** @param stadiumId El nuevo ID del estadio. */
    public void setStadiumId(Long stadiumId) { this.stadiumId = stadiumId; }

    /** @return El nombre del estadio. */
    public String getStadiumName() { return stadiumName; }

    /** @param stadiumName El nuevo nombre del estadio. */
    public void setStadiumName(String stadiumName) { this.stadiumName = stadiumName; }

    /** @return La ciudad del estadio. */
    public String getStadiumCity() { return stadiumCity; }

    /** @param stadiumCity La nueva ciudad del estadio. */
    public void setStadiumCity(String stadiumCity) { this.stadiumCity = stadiumCity; }

    /** @return La zona horaria IANA del estadio. */
    public String getStadiumTimezone() { return stadiumTimezone; }

    /** @param stadiumTimezone La nueva zona horaria. */
    public void setStadiumTimezone(String stadiumTimezone) { this.stadiumTimezone = stadiumTimezone; }

    /** @return La latitud del estadio (WGS84). */
    public Double getStadiumLatitude() { return stadiumLatitude; }

    /** @param stadiumLatitude La nueva latitud del estadio. */
    public void setStadiumLatitude(Double stadiumLatitude) { this.stadiumLatitude = stadiumLatitude; }

    /** @return La longitud del estadio (WGS84). */
    public Double getStadiumLongitude() { return stadiumLongitude; }

    /** @param stadiumLongitude La nueva longitud del estadio. */
    public void setStadiumLongitude(Double stadiumLongitude) { this.stadiumLongitude = stadiumLongitude; }

    /** @return La capacidad del estadio o {@code null} si no se conoce. */
    public Integer getStadiumCapacity() { return stadiumCapacity; }

    /** @param stadiumCapacity La nueva capacidad del estadio. */
    public void setStadiumCapacity(Integer stadiumCapacity) { this.stadiumCapacity = stadiumCapacity; }

    /** @return La URL de la imagen del estadio o {@code null}. */
    public String getStadiumImageUrl() { return stadiumImageUrl; }

    /** @param stadiumImageUrl La nueva URL de imagen del estadio. */
    public void setStadiumImageUrl(String stadiumImageUrl) { this.stadiumImageUrl = stadiumImageUrl; }

    /** @return La fecha y hora programada en UTC. */
    public LocalDateTime getScheduledAt() { return scheduledAt; }

    /** @param scheduledAt La nueva fecha y hora programada. */
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    /** @return La fase del torneo. */
    public Phase getPhase() { return phase; }

    /** @param phase La nueva fase. */
    public void setPhase(Phase phase) { this.phase = phase; }

    /** @return El estado actual del partido. */
    public MatchStatus getStatus() { return status; }

    /** @param status El nuevo estado. */
    public void setStatus(MatchStatus status) { this.status = status; }

    /** @return Los goles del equipo local. */
    public Integer getHomeScore() { return homeScore; }

    /** @param homeScore Los nuevos goles del equipo local. */
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    /** @return Los goles del equipo visitante. */
    public Integer getAwayScore() { return awayScore; }

    /** @param awayScore Los nuevos goles del equipo visitante. */
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    /** @return El nombre del grupo. */
    public String getGroupName() { return groupName; }

    /** @param groupName El nuevo nombre del grupo. */
    public void setGroupName(String groupName) { this.groupName = groupName; }

    /** @return El número de jornada. */
    public Integer getMatchday() { return matchday; }

    /** @param matchday El nuevo número de jornada. */
    public void setMatchday(Integer matchday) { this.matchday = matchday; }

    /** @return El estado del dato en la plataforma. */
    public DataStatus getDataStatus() { return dataStatus; }

    /** @param dataStatus El nuevo estado del dato. */
    public void setDataStatus(DataStatus dataStatus) { this.dataStatus = dataStatus; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MatchDTO other = (MatchDTO) obj;
        return Objects.equals(id, other.id) && Objects.equals(externalId, other.externalId);
    }

    @Override
    public int hashCode() { return Objects.hash(id, externalId); }

    @Override
    public String toString() {
        return "MatchDTO [id=" + id + ", homeTeamName=" + homeTeamName
                + ", awayTeamName=" + awayTeamName
                + ", scheduledAt=" + scheduledAt + ", phase=" + phase
                + ", status=" + status + ", homeScore=" + homeScore
                + ", awayScore=" + awayScore + ", dataStatus=" + dataStatus + "]";
    }
}