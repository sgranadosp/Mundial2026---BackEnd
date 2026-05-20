package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;
 
import jakarta.persistence.Column;
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
 * Entidad JPA que representa un partido del Mundial 2026.
 * <p>
 * Un partido conecta dos selecciones ({@link Team}), un estadio ({@link Stadium}),
 * una fase del torneo y un estado del ciclo de vida. El estado controla
 * cuándo se permite registrar pronósticos, cuándo se calculan puntajes
 * y cuándo se emiten notificaciones.
 * </p>
 * <p>
 * Los datos de marcador y estado son sincronizados desde la API externa
 * (football-data.org, API-Football o WireMock). Si la fuente falla,
 * el campo {@code dataStatus} indica si el dato es confirmado o provisional.
 * </p>
 */
@Entity
@Table(name = "matches")
public class Match {
 
    /**
     * Identificador único del partido generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    /**
     * Identificador del partido en la API externa de datos deportivos.
     * Se usa para sincronizar actualizaciones sin duplicar registros.
     */
    @Column(unique = true)
    private Long externalId;
 
    /**
     * Selección local (home) del partido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;
 
    /**
     * Selección visitante (away) del partido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;
 
    /**
     * Estadio donde se disputará o se disputó el partido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id")
    private Stadium stadium;
 
    /**
     * Fecha y hora programada del partido en UTC.
     * El frontend convierte este valor a la zona horaria local del estadio
     * o del usuario usando {@code Stadium#timezone}.
     */
    private LocalDateTime scheduledAt;
 
    /**
     * Fase del torneo a la que pertenece el partido,
     * definida mediante el enum {@link Phase}.
     */
    @Enumerated(EnumType.STRING)
    private Phase phase;
 
    /**
     * Estado actual del partido en su ciclo de vida,
     * definido mediante el enum {@link MatchStatus}.
     */
    @Enumerated(EnumType.STRING)
    private MatchStatus status;
 
    /**
     * Goles marcados por el equipo local al finalizar el partido.
     * Es nulo mientras el partido no haya terminado.
     */
    private Integer homeScore;
 
    /**
     * Goles marcados por el equipo visitante al finalizar el partido.
     * Es nulo mientras el partido no haya terminado.
     */
    private Integer awayScore;
 
    /**
     * Nombre del grupo en la fase de grupos (ej. "A", "B").
     * Es nulo en fases eliminatorias.
     */
    private String groupName;
 
    /**
     * Número de la jornada dentro de la fase del torneo.
     */
    private Integer matchday;
 
    /**
     * Estado del dato en la plataforma, definido mediante el enum {@link DataStatus}.
     * Indica si la información fue confirmada por la fuente principal o es provisional.
     */
    @Enumerated(EnumType.STRING)
    private DataStatus dataStatus;
 
    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el estado como {@code SCHEDULED} y el estado del dato como {@code CONFIRMED}.
     */
    public Match() {
        this.status = MatchStatus.SCHEDULED;
        this.dataStatus = DataStatus.CONFIRMED;
    }
 
    /**
     * Constructor con los datos principales del partido.
     *
     * @param externalId   Identificador en la API externa.
     * @param homeTeam     Selección local.
     * @param awayTeam     Selección visitante.
     * @param stadium      Estadio del partido.
     * @param scheduledAt  Fecha y hora programada en UTC.
     * @param phase        Fase del torneo.
     */
    public Match(Long externalId, Team homeTeam, Team awayTeam, Stadium stadium,
                 LocalDateTime scheduledAt, Phase phase) {
        this();
        this.externalId = externalId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.stadium = stadium;
        this.scheduledAt = scheduledAt;
        this.phase = phase;
    }
 
    /**
     * Enumeración que define las fases del torneo Mundial 2026.
     */
    public enum Phase {
        /** Fase de grupos. */
        GROUP_STAGE,
        /** Ronda de 32 (nuevo formato 2026). */
        ROUND_OF_32,
        /** Octavos de final. */
        ROUND_OF_16,
        /** Cuartos de final. */
        QUARTER_FINALS,
        /** Semifinales. */
        SEMI_FINALS,
        /** Tercer puesto. */
        THIRD_PLACE,
        /** Final. */
        FINAL
    }
 
    /**
     * Enumeración que define el estado de ciclo de vida de un partido.
     */
    public enum MatchStatus {
        /** El partido está programado pero no ha comenzado. */
        SCHEDULED,
        /** El partido está en curso. */
        LIVE,
        /** El partido ha concluido. */
        FINISHED,
        /** El partido fue pospuesto. */
        POSTPONED,
        /** El partido fue cancelado. */
        CANCELLED
    }
 
    /**
     * Enumeración que define el estado de confiabilidad del dato en la plataforma.
     * Implementa el principio de "información confiable" requerido por el proyecto.
     */
    public enum DataStatus {
        /** El dato fue confirmado por la fuente principal. */
        CONFIRMED,
        /** El dato es provisional porque la fuente no respondió o respondió con error. */
        PENDING_UPDATE,
        /** Existe discrepancia entre dos fuentes; se usa la fuente principal. */
        CONFLICT
    }
 
    // =========================================================================
    // Getters y Setters
    // =========================================================================
 
    /** @return El ID del partido. */
    public Long getId() { return id; }
 
    /** @param id El nuevo ID del partido. */
    public void setId(Long id) { this.id = id; }
 
    /** @return El ID del partido en la API externa. */
    public Long getExternalId() { return externalId; }
 
    /** @param externalId El nuevo ID externo. */
    public void setExternalId(Long externalId) { this.externalId = externalId; }
 
    /** @return La selección local del partido. */
    public Team getHomeTeam() { return homeTeam; }
 
    /** @param homeTeam La nueva selección local. */
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }
 
    /** @return La selección visitante del partido. */
    public Team getAwayTeam() { return awayTeam; }
 
    /** @param awayTeam La nueva selección visitante. */
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }
 
    /** @return El estadio del partido. */
    public Stadium getStadium() { return stadium; }
 
    /** @param stadium El nuevo estadio. */
    public void setStadium(Stadium stadium) { this.stadium = stadium; }
 
    /** @return La fecha y hora programada del partido en UTC. */
    public LocalDateTime getScheduledAt() { return scheduledAt; }
 
    /** @param scheduledAt La nueva fecha y hora programada. */
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
 
    /** @return La fase del torneo. */
    public Phase getPhase() { return phase; }
 
    /** @param phase La nueva fase del torneo. */
    public void setPhase(Phase phase) { this.phase = phase; }
 
    /** @return El estado actual del partido. */
    public MatchStatus getStatus() { return status; }
 
    /** @param status El nuevo estado del partido. */
    public void setStatus(MatchStatus status) { this.status = status; }
 
    /** @return Los goles del equipo local. */
    public Integer getHomeScore() { return homeScore; }
 
    /** @param homeScore Los nuevos goles del equipo local. */
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }
 
    /** @return Los goles del equipo visitante. */
    public Integer getAwayScore() { return awayScore; }
 
    /** @param awayScore Los nuevos goles del equipo visitante. */
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }
 
    /** @return El nombre del grupo en fase de grupos. */
    public String getGroupName() { return groupName; }
 
    /** @param groupName El nuevo nombre de grupo. */
    public void setGroupName(String groupName) { this.groupName = groupName; }
 
    /** @return El número de jornada del partido. */
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
 
    /**
     * Compara partidos por ID interno e ID externo.
     *
     * @param obj El objeto a comparar.
     * @return {@code true} si representan el mismo partido.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Match other = (Match) obj;
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
     * Representación en cadena del partido.
     *
     * @return Cadena con los atributos principales del partido.
     */
    @Override
    public String toString() {
        return "Match [id=" + id + ", externalId=" + externalId
                + ", homeTeam=" + (homeTeam != null ? homeTeam.getShortName() : "null")
                + ", awayTeam=" + (awayTeam != null ? awayTeam.getShortName() : "null")
                + ", scheduledAt=" + scheduledAt
                + ", phase=" + phase + ", status=" + status
                + ", homeScore=" + homeScore + ", awayScore=" + awayScore
                + ", dataStatus=" + dataStatus + "]";
    }
}