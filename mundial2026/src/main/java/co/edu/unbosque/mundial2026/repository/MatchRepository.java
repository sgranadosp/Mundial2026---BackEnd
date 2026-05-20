/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.Match.Phase;

/**
 * Interfaz de repositorio para la entidad {@link Match}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code matches}. Los métodos personalizados cubren los principales
 * casos de uso del módulo de partidos: listado de partidos programados,
 * filtros por equipo / estadio / fase / estado, búsqueda para sincronización
 * con la API externa, y consultas para el módulo de pollas (pronósticos solo
 * se permiten en partidos con estado {@code SCHEDULED}).
 */
public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Busca un partido por su identificador en la API externa de datos deportivos.
     * Se usa en la sincronización periódica para actualizar marcadores y estados
     * sin duplicar registros.
     *
     * @param externalId El identificador del partido en la API externa.
     * @return Un {@link Optional} con el partido encontrado, o vacío si no existe.
     */
    Optional<Match> findByExternalId(Long externalId);

    /**
     * Obtiene todos los partidos con un estado específico del ciclo de vida.
     * El caso más frecuente es buscar partidos con estado {@code SCHEDULED}
     * para mostrar la agenda y permitir pronósticos, y partidos {@code FINISHED}
     * para calcular puntajes de pollas.
     *
     * @param status El estado del partido a filtrar.
     * @return Lista de partidos con el estado indicado.
     */
    List<Match> findByStatus(MatchStatus status);

    /**
     * Obtiene todos los partidos de una fase específica del torneo.
     * Se usa para mostrar el bracket del torneo y para filtrar partidos
     * en la agenda del usuario (HU06 — Ver lista de partidos).
     *
     * @param phase La fase del torneo a filtrar.
     * @return Lista de partidos de la fase indicada.
     */
    List<Match> findByPhase(Phase phase);

    /**
     * Obtiene todos los partidos de un estadio específico.
     * Se usa en el filtro de partidos por sede en la agenda personalizada.
     *
     * @param stadiumId El ID interno del estadio.
     * @return Lista de partidos programados en ese estadio.
     */
    List<Match> findByStadiumId(Long stadiumId);

    /**
     * Obtiene todos los partidos en los que participa un equipo específico,
     * ya sea como local o visitante. Se usa para la agenda personalizada del
     * usuario según su equipo favorito.
     *
     * @param homeTeamId El ID del equipo local.
     * @param awayTeamId El ID del equipo visitante.
     * @return Lista de partidos en los que participa el equipo.
     */
    @Query("SELECT m FROM Match m WHERE m.homeTeam.id = :homeTeamId OR m.awayTeam.id = :awayTeamId")
    List<Match> findByTeam(@Param("homeTeamId") Long homeTeamId, @Param("awayTeamId") Long awayTeamId);

    /**
     * Obtiene todos los partidos programados dentro de un rango de fechas.
     * Se usa en el filtro por fecha de la interfaz (HU09 — Filtrar partidos
     * por fecha) y para generar recordatorios contextuales al aficionado viajero.
     *
     * @param start Fecha y hora de inicio del rango (inclusive), en UTC.
     * @param end   Fecha y hora de fin del rango (inclusive), en UTC.
     * @return Lista de partidos dentro del rango indicado.
     */
    List<Match> findByScheduledAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Obtiene todos los partidos de un estadio específico con un estado dado.
     * Combinación útil para mostrar la agenda de una sede filtrando solo los
     * partidos pendientes o solo los finalizados.
     *
     * @param stadiumId El ID del estadio.
     * @param status    El estado del partido.
     * @return Lista de partidos del estadio con el estado indicado.
     */
    List<Match> findByStadiumIdAndStatus(Long stadiumId, MatchStatus status);

    /**
     * Obtiene todos los partidos de un grupo de la fase de grupos, ordenados
     * por fecha ascendente. Se usa para construir la tabla del grupo.
     *
     * @param groupName El nombre del grupo (ej. "A", "B").
     * @return Lista de partidos del grupo, ordenados por fecha.
     */
    List<Match> findByGroupNameOrderByScheduledAtAsc(String groupName);

    /**
     * Obtiene los próximos partidos con estado {@code SCHEDULED} cuya fecha
     * sea anterior a un umbral dado, con soporte de paginación.
     * Se usa para enviar recordatorios antes de que comience un partido y para
     * calcular cuándo bloquear los pronósticos de la polla.
     *
     * @param status      El estado del partido (normalmente {@code SCHEDULED}).
     * @param threshold   La fecha y hora límite.
     * @param pageable    Parámetros de paginación.
     * @return Página de partidos con estado y fecha dentro del umbral.
     */
    Page<Match> findByStatusAndScheduledAtBefore(MatchStatus status, LocalDateTime threshold, Pageable pageable);

    /**
     * Verifica si ya existe un partido registrado con el identificador externo dado.
     * Se usa antes de insertar para evitar duplicados en la sincronización.
     *
     * @param externalId El identificador externo a verificar.
     * @return {@code true} si ya existe un partido con ese ID externo.
     */
    boolean existsByExternalId(Long externalId);
}