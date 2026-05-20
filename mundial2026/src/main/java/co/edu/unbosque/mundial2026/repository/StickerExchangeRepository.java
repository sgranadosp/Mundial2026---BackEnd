/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.mundial2026.model.StickerExchange;
import co.edu.unbosque.mundial2026.model.StickerExchange.ExchangeStatus;

/**
 * Interfaz de repositorio para la entidad {@link StickerExchange}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code sticker_exchanges}. Los métodos personalizados cubren los
 * flujos de intercambio: ver solicitudes recibidas y enviadas, y limitar
 * el número de intercambios activos por usuario para prevenir abuso
 * (restricción de negocio del proyecto).
 */
public interface StickerExchangeRepository extends JpaRepository<StickerExchange, Long> {

    /**
     * Obtiene todos los intercambios enviados por un usuario (como solicitante),
     * con un estado específico. Se usa para mostrar las solicitudes pendientes
     * enviadas por el usuario.
     *
     * @param requesterId El ID del usuario solicitante.
     * @param status      El estado del intercambio.
     * @return Lista de intercambios enviados con el estado indicado.
     */
    List<StickerExchange> findByRequesterIdAndStatus(Long requesterId, ExchangeStatus status);

    /**
     * Obtiene todos los intercambios recibidos por un usuario (como receptor),
     * con un estado específico. Se usa para mostrar las solicitudes pendientes
     * recibidas que el usuario debe aceptar o rechazar.
     *
     * @param receiverId El ID del usuario receptor.
     * @param status     El estado del intercambio.
     * @return Lista de intercambios recibidos con el estado indicado.
     */
    List<StickerExchange> findByReceiverIdAndStatus(Long receiverId, ExchangeStatus status);

    /**
     * Obtiene el historial completo de intercambios de un usuario, tanto
     * como solicitante como receptor, en todos los estados.
     * Se usa para la vista de historial de intercambios en el perfil.
     *
     * @param userId El ID del usuario.
     * @return Lista de todos los intercambios en los que participó el usuario.
     */
    @Query("SELECT se FROM StickerExchange se WHERE se.requester.id = :userId OR se.receiver.id = :userId")
    List<StickerExchange> findAllByUserId(@Param("userId") Long userId);

    /**
     * Obtiene los intercambios activos (PENDING o ACCEPTED) de un usuario.
     * Se usa para aplicar el límite máximo de intercambios simultáneos
     * por usuario y prevenir abuso o automatización masiva.
     *
     * @param userId El ID del usuario.
     * @return Lista de intercambios activos del usuario.
     */
    @Query("SELECT se FROM StickerExchange se " +
           "WHERE (se.requester.id = :userId OR se.receiver.id = :userId) " +
           "AND se.status IN ('PENDING', 'ACCEPTED')")
    List<StickerExchange> findActiveExchangesByUserId(@Param("userId") Long userId);

    /**
     * Verifica si una lámina específica ya está involucrada en un intercambio
     * activo (estado PENDING o ACCEPTED). Se usa antes de crear un nuevo
     * intercambio para garantizar que la lámina no esté ya comprometida.
     *
     * @param stickerId El ID de la lámina.
     * @return {@code true} si la lámina está en un intercambio activo.
     */
    @Query("SELECT COUNT(se) > 0 FROM StickerExchange se " +
           "WHERE (se.offeredSticker.id = :stickerId OR se.requestedSticker.id = :stickerId) " +
           "AND se.status IN ('PENDING', 'ACCEPTED')")
    boolean isStickerInActiveExchange(@Param("stickerId") Long stickerId);

    /**
     * Cuenta los intercambios activos de un usuario.
     * Se usa junto con el límite de negocio configurado para decidir si el
     * usuario puede iniciar un nuevo intercambio.
     *
     * @param userId El ID del usuario.
     * @return Número de intercambios activos del usuario.
     */
    @Query("SELECT COUNT(se) FROM StickerExchange se " +
           "WHERE (se.requester.id = :userId OR se.receiver.id = :userId) " +
           "AND se.status IN ('PENDING', 'ACCEPTED')")
    long countActiveExchangesByUserId(@Param("userId") Long userId);
}