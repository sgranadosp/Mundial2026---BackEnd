/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.mundial2026.model.PollGroup;

/**
 * Interfaz de repositorio para la entidad {@link PollGroup}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code poll_groups}. Los métodos personalizados cubren los
 * principales flujos del módulo de pollas: unirse por código de invitación,
 * listar grupos del usuario (como creador o como miembro), y filtrar grupos
 * activos para el cálculo de puntajes cuando un partido finaliza.
 */
public interface PollGroupRepository extends JpaRepository<PollGroup, Long> {

    /**
     * Busca un grupo de polla por su código de invitación único.
     * Se usa cuando un usuario ingresa el código para unirse a un grupo
     * existente (flujo de incorporación a la polla).
     *
     * @param inviteCode El código de invitación del grupo.
     * @return Un {@link Optional} con el grupo encontrado, o vacío si el
     *         código no corresponde a ningún grupo activo.
     */
    Optional<PollGroup> findByInviteCode(String inviteCode);

    /**
     * Obtiene todos los grupos de polla creados por un usuario específico.
     * Se usa en el panel del usuario para mostrar los grupos que administra.
     *
     * @param ownerId El ID del usuario creador.
     * @return Lista de grupos de polla cuyo creador es el usuario indicado.
     */
    List<PollGroup> findByOwnerId(Long ownerId);

    /**
     * Obtiene todos los grupos activos creados por un usuario específico.
     * Versión filtrada de {@code findByOwnerId} para mostrar solo los grupos
     * en los que aún se pueden registrar pronósticos.
     *
     * @param ownerId El ID del usuario creador.
     * @param active  {@code true} para grupos activos, {@code false} para inactivos.
     * @return Lista de grupos del usuario con el estado de actividad indicado.
     */
    List<PollGroup> findByOwnerIdAndActive(Long ownerId, boolean active);

    /**
     * Obtiene todos los grupos de polla en los que un usuario es miembro,
     * incluyendo los grupos que él mismo creó. Se usa para mostrar todos los
     * grupos del usuario en su panel de pollas.
     *
     * @param userId El ID del usuario miembro.
     * @return Lista de grupos de polla en los que el usuario participa.
     */
    @Query("SELECT pg FROM PollGroup pg JOIN pg.members m WHERE m.id = :userId")
    List<PollGroup> findGroupsByMemberId(@Param("userId") Long userId);

    /**
     * Obtiene todos los grupos de polla activos en los que un usuario es miembro.
     * Versión filtrada de {@code findGroupsByMemberId} para el panel principal.
     *
     * @param userId El ID del usuario miembro.
     * @return Lista de grupos activos en los que el usuario participa.
     */
    @Query("SELECT pg FROM PollGroup pg JOIN pg.members m WHERE m.id = :userId AND pg.active = true")
    List<PollGroup> findActiveGroupsByMemberId(@Param("userId") Long userId);

    /**
     * Verifica si un usuario ya es miembro de un grupo específico.
     * Se usa para evitar que un usuario se una dos veces al mismo grupo.
     *
     * @param groupId El ID del grupo de polla.
     * @param userId  El ID del usuario a verificar.
     * @return {@code true} si el usuario ya pertenece al grupo.
     */
    @Query("SELECT COUNT(m) > 0 FROM PollGroup pg JOIN pg.members m WHERE pg.id = :groupId AND m.id = :userId")
    boolean isMemberOfGroup(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * Verifica si ya existe un grupo con el código de invitación dado.
     * Se usa al generar un nuevo código para garantizar unicidad.
     *
     * @param inviteCode El código de invitación a verificar.
     * @return {@code true} si el código ya está en uso.
     */
    boolean existsByInviteCode(String inviteCode);
}