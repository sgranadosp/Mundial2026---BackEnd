package co.edu.unbosque.mundial2026.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.mundial2026.model.NotificationInbox;

/**
 * Repositorio JPA para el inbox de notificaciones.
 *
 * <p>Operaciones soportadas:
 * <ul>
 *   <li>Listar las notificaciones de un usuario ordenadas de la más reciente
 *       a la más antigua. Es la operación principal del endpoint
 *       {@code GET /notifications/inbox/{userId}}.</li>
 *   <li>Contar las notificaciones no leídas para badges en la UI.</li>
 *   <li>Bulk-update para marcar todas como leídas de una sola pasada.</li>
 * </ul></p>
 */
@Repository
public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    /**
     * Devuelve las notificaciones de un usuario en orden cronológico inverso.
     */
    @Query("SELECT n FROM NotificationInbox n WHERE n.user.id = :userId ORDER BY n.occurredAt DESC")
    List<NotificationInbox> findByUserIdOrderByOccurredAtDesc(@Param("userId") Long userId);

    /**
     * Cuenta las notificaciones no leídas de un usuario. Útil para badges
     * tipo "tienes 3 mensajes nuevos".
     */
    @Query("SELECT COUNT(n) FROM NotificationInbox n WHERE n.user.id = :userId AND n.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);
}