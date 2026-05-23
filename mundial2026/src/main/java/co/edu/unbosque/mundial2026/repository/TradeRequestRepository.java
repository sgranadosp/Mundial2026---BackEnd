package co.edu.unbosque.mundial2026.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.mundial2026.model.TradeRequest;
import co.edu.unbosque.mundial2026.model.TradeRequest.Status;

/**
 * Repositorio JPA para {@link TradeRequest}.
 */
@Repository
public interface TradeRequestRepository extends JpaRepository<TradeRequest, Long> {

    /**
     * Lista las solicitudes activas (PENDING) creadas por un usuario.
     * Útil para mostrar al usuario sus propias solicitudes y para validar
     * el límite de 5 activas simultáneas.
     */
    List<TradeRequest> findByCreatorIdAndStatus(Long creatorId, Status status);

    /**
     * Cuenta cuántas solicitudes activas tiene un usuario, para validar
     * el límite máximo de 5 simultáneas.
     */
    long countByCreatorIdAndStatus(Long creatorId, Status status);

    /**
     * Listado de solicitudes que un usuario PUEDE VER en su bandeja de
     * "Solicitudes recibidas":
     *   - Estado PENDING (activas).
     *   - El creador NO es él mismo (uno no se ve sus propias solicitudes
     *     en "recibidas", sino en "mías").
     *   - NO existe una fila en {@code TradeRejection} con su userId y la
     *     trade_request_id (no la ha rechazado previamente).
     *
     * Ordenadas por fecha de creación descendente (más recientes primero).
     */
    @Query("SELECT tr FROM TradeRequest tr " +
           "WHERE tr.status = co.edu.unbosque.mundial2026.model.TradeRequest$Status.PENDING " +
           "  AND tr.creator.id <> :userId " +
           "  AND tr.id NOT IN (" +
           "      SELECT rej.tradeRequest.id FROM TradeRejection rej " +
           "      WHERE rej.user.id = :userId) " +
           "ORDER BY tr.createdAt DESC")
    List<TradeRequest> findReceivedForUser(@Param("userId") Long userId);

    /**
     * Lista las solicitudes activas del creador que ofrecen una lámina
     * específica. Útil para el hook de auto-cancelación cuando el creador
     * pierde su única repetida.
     */
    @Query("SELECT tr FROM TradeRequest tr " +
           "WHERE tr.creator.id = :creatorId " +
           "  AND tr.offeredSticker.id = :stickerId " +
           "  AND tr.status = co.edu.unbosque.mundial2026.model.TradeRequest$Status.PENDING")
    List<TradeRequest> findActiveByCreatorAndOfferedSticker(
            @Param("creatorId") Long creatorId,
            @Param("stickerId") Long stickerId);

    /**
     * Lista las solicitudes activas del creador que piden una lámina
     * específica. Útil para auto-cancelar cuando el creador obtiene
     * lo que pedía por otra vía.
     */
    @Query("SELECT tr FROM TradeRequest tr " +
           "WHERE tr.creator.id = :creatorId " +
           "  AND tr.requestedSticker.id = :stickerId " +
           "  AND tr.status = co.edu.unbosque.mundial2026.model.TradeRequest$Status.PENDING")
    List<TradeRequest> findActiveByCreatorAndRequestedSticker(
            @Param("creatorId") Long creatorId,
            @Param("stickerId") Long stickerId);
}