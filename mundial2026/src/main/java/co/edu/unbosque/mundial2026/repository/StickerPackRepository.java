package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.mundial2026.model.StickerPack;
import co.edu.unbosque.mundial2026.model.StickerPack.Origin;

/**
 * Repositorio JPA para la entidad {@link StickerPack}.
 *
 * Expone consultas para obtener el inventario de packs pendientes de un
 * usuario por origen, el historial completo (incluyendo abiertos), y
 * helpers para detectar si el usuario ya recibió packs de bienvenida.
 */
@Repository
public interface StickerPackRepository extends JpaRepository<StickerPack, Long> {

    /**
     * Cuenta cuántos packs PENDIENTES (no abiertos) tiene el usuario de un
     * origen dado.
     */
    long countByUserIdAndOriginAndOpenedFalse(Long userId, Origin origin);

    /**
     * Cuenta cuántos packs ha recibido el usuario de un origen, incluyendo
     * abiertos y pendientes. Útil para validar que el welcome solo se
     * otorgue una vez (sin contar nunca >3).
     */
    long countByUserIdAndOrigin(Long userId, Origin origin);

    /**
     * Lista todos los packs del usuario ordenados por fecha de otorgamiento
     * descendente (los más recientes primero). Útil para el historial.
     */
    List<StickerPack> findByUserIdOrderByGrantedAtDesc(Long userId);

    /**
     * Obtiene el primer pack PENDIENTE (no abierto) de un usuario y origen
     * específico, eligiendo el más antiguo para ser justo (FIFO).
     */
    Optional<StickerPack> findFirstByUserIdAndOriginAndOpenedFalseOrderByGrantedAtAsc(
            Long userId, Origin origin);

    /**
     * Cuenta cuántos pronósticos POLL packs ha recibido el usuario.
     * Equivale a {@code totalPronosticos / 6} en el momento del último
     * otorgamiento. Sirve para calcular cuántos packs por pronósticos
     * faltan otorgar al detectar nuevos múltiplos de 6.
     */
    @Query("SELECT COUNT(sp) FROM StickerPack sp " +
           "WHERE sp.user.id = :userId AND sp.origin = " +
           "co.edu.unbosque.mundial2026.model.StickerPack$Origin.POLL")
    long countPollPacksByUser(@Param("userId") Long userId);
}