package co.edu.unbosque.mundial2026.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.mundial2026.model.SupportTicket;
import co.edu.unbosque.mundial2026.model.SupportTicket.Status;

/**
 * Repositorio JPA para los tickets de soporte.
 *
 * <p>Operaciones:
 * <ul>
 *   <li>Listar los tickets de un usuario (vista USER) ordenados de más
 *       reciente a más antiguo.</li>
 *   <li>Listar todos los tickets abiertos (vista ADMIN) para que el equipo
 *       sepa qué hay pendiente de responder.</li>
 *   <li>Listar todos los tickets con paginación implícita por orden cronológico
 *       (vista ADMIN cuando se quiere ver histórico completo).</li>
 * </ul></p>
 */
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    /**
     * Tickets de un usuario, ordenados del más reciente al más antiguo.
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Tickets en un estado específico, ordenados por fecha de creación
     * descendente. La vista admin de "Tickets abiertos" usa {@code OPEN}.
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(@Param("status") Status status);

    /**
     * Todos los tickets ordenados del más reciente al más antiguo. Para
     * la vista admin cuando se quiere ver el histórico.
     */
    @Query("SELECT t FROM SupportTicket t ORDER BY t.createdAt DESC")
    List<SupportTicket> findAllOrderByCreatedAtDesc();
}