/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.Team;

/**
 * Interfaz de repositorio para la entidad {@link Team}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar
 * sobre la tabla {@code teams}. Los métodos personalizados permiten sincronizar
 * equipos desde la API externa (por {@code externalId}), buscar por código ISO
 * para las preferencias de usuario, y filtrar por grupo en la fase de grupos.
 */
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Busca un equipo por su identificador en la API externa de datos deportivos.
     * Se usa en la sincronización periódica desde football-data.org o WireMock
     * para actualizar datos sin duplicar registros.
     *
     * @param externalId El identificador del equipo en la API externa.
     * @return Un {@link Optional} con el equipo encontrado, o vacío si no existe.
     */
    Optional<Team> findByExternalId(Long externalId);

    /**
     * Busca un equipo por su código ISO-3166 alfa-3 (ej. "COL", "BRA", "ARG").
     * Se usa para resolver el equipo favorito del usuario a partir de la
     * preferencia almacenada en {@code User#favoriteTeamCode}.
     *
     * @param isoCode El código ISO alfa-3 del país.
     * @return Un {@link Optional} con el equipo encontrado, o vacío si no existe.
     */
    Optional<Team> findByIsoCode(String isoCode);

    /**
     * Busca un equipo por su nombre corto o alias de la API.
     * Se usa como alternativa de búsqueda cuando el código ISO no está disponible.
     *
     * @param shortName El nombre corto del equipo.
     * @return Un {@link Optional} con el equipo encontrado, o vacío si no existe.
     */
    Optional<Team> findByShortName(String shortName);

    /**
     * Obtiene todos los equipos asignados a un grupo específico de la fase de
     * grupos (ej. grupo "A", "B"). Se usa para mostrar la tabla de posiciones
     * del grupo en la interfaz de partidos.
     *
     * @param group El nombre del grupo (ej. "A", "B", "C").
     * @return Lista de equipos que pertenecen al grupo indicado.
     */
    List<Team> findByGroup(String group);

    /**
     * Verifica si ya existe un equipo registrado con el identificador externo dado.
     * Se usa antes de insertar para evitar duplicados durante la sincronización.
     *
     * @param externalId El identificador externo a verificar.
     * @return {@code true} si ya existe un equipo con ese ID externo.
     */
    boolean existsByExternalId(Long externalId);
}