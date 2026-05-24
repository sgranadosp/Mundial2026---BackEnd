package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.NotificationDTO;
import co.edu.unbosque.mundial2026.dto.SupportTicketDTO;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventResult;
import co.edu.unbosque.mundial2026.model.SupportTicket;
import co.edu.unbosque.mundial2026.model.SupportTicket.Status;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.SupportTicketRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Servicio de tickets de soporte.
 *
 * <p>Cubre todo el ciclo de vida del ticket:
 * <ul>
 *   <li>USER crea un ticket con un mensaje libre.</li>
 *   <li>USER lista sus propios tickets (historial).</li>
 *   <li>USER elimina (borra de BD) un ticket cerrado que ya leyó.</li>
 *   <li>ADMIN lista los tickets pendientes (status=OPEN).</li>
 *   <li>ADMIN lista todos los tickets (incluyendo cerrados).</li>
 *   <li>ADMIN responde un ticket. Esto marca el ticket como CLOSED,
 *       graba la respuesta, y dispara una notificación push automática
 *       al usuario que lo abrió.</li>
 * </ul></p>
 */
@Service
public class SupportTicketService {

    @Autowired
    private SupportTicketRepository ticketRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditEventService auditService;

    /**
     * Crea un nuevo ticket de soporte para el usuario indicado.
     *
     * @param userId  ID del usuario que abre el ticket.
     * @param message Mensaje del usuario (obligatorio, no vacío).
     * @return 0 si fue creado; 2 si no existe el usuario; 6 si el mensaje
     *         está vacío o excede 2000 caracteres.
     */
    @Transactional
    public int createTicket(Long userId, String message) {
        if (message == null) return 6;
        String trimmed = message.trim();
        if (trimmed.isEmpty() || trimmed.length() > 2000) return 6;

        Optional<User> user = userRepo.findById(userId);
        if (user.isEmpty()) return 2;

        SupportTicket ticket = new SupportTicket(user.get(), trimmed);
        ticketRepo.save(ticket);

        auditService.logJobExecuted(userId, "SUPPORT_TICKET_CREATED",
                "Ticket de soporte creado · id=" + ticket.getId(),
                EventResult.SUCCESS);
        return 0;
    }

    /**
     * Lista los tickets propios del usuario, del más reciente al más antiguo.
     *
     * @param userId ID del usuario titular.
     * @return Lista de DTOs (puede estar vacía).
     */
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getTicketsForUser(Long userId) {
        List<SupportTicket> tickets = ticketRepo.findByUserIdOrderByCreatedAtDesc(userId);
        List<SupportTicketDTO> dtos = new ArrayList<>();
        tickets.forEach(t -> dtos.add(toDTO(t)));
        return dtos;
    }

    /**
     * Lista todos los tickets en estado OPEN. Esta es la vista por defecto
     * del panel admin "Tickets de Soporte": muestra solo lo que está
     * pendiente de respuesta.
     */
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getOpenTickets() {
        List<SupportTicket> tickets = ticketRepo.findByStatusOrderByCreatedAtDesc(Status.OPEN);
        List<SupportTicketDTO> dtos = new ArrayList<>();
        tickets.forEach(t -> dtos.add(toDTO(t)));
        return dtos;
    }

    /**
     * Lista todos los tickets (OPEN + CLOSED). Útil para la vista admin
     * cuando se quiere ver el historial completo.
     */
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets() {
        List<SupportTicket> tickets = ticketRepo.findAllOrderByCreatedAtDesc();
        List<SupportTicketDTO> dtos = new ArrayList<>();
        tickets.forEach(t -> dtos.add(toDTO(t)));
        return dtos;
    }

    /**
     * Responde un ticket por parte del admin. El ticket pasa a CLOSED y
     * se dispara una notificación push automática al usuario que lo abrió.
     *
     * @param ticketId ID del ticket.
     * @param adminId  ID del admin que responde.
     * @param response Texto de la respuesta (obligatorio, no vacío).
     * @return 0 si fue respondido; 2 si no existe el ticket; 5 si ya estaba
     *         cerrado; 6 si la respuesta está vacía o excede 2000 caracteres.
     */
    @Transactional
    public int respondTicket(Long ticketId, Long adminId, String response) {
        if (response == null) return 6;
        String trimmed = response.trim();
        if (trimmed.isEmpty() || trimmed.length() > 2000) return 6;

        Optional<SupportTicket> found = ticketRepo.findById(ticketId);
        if (found.isEmpty()) return 2;

        SupportTicket ticket = found.get();
        if (ticket.getStatus() == Status.CLOSED) return 5;

        ticket.setResponse(trimmed);
        ticket.setStatus(Status.CLOSED);
        ticket.setRespondedByAdminId(adminId);
        ticket.setRespondedAt(LocalDateTime.now());
        ticketRepo.save(ticket);

        /*
         * Notificación push al usuario. Usamos channel "IN_APP" para que
         * NotificationService respete las preferencias del usuario y le
         * envíe por todos los canales que tenga activos (push + email).
         * El inbox se persiste automáticamente sin importar el canal.
         */
        try {
            if (ticket.getUser() != null) {
                NotificationDTO dto = new NotificationDTO(
                        ticket.getUser().getId(),
                        "Respuesta a tu ticket de soporte",
                        "El equipo de soporte respondió tu mensaje. "
                                + "Revisa los detalles en \"Contacto y Soporte\".",
                        "IN_APP",
                        "SUPPORT");
                dto.setResourceId(ticket.getId());
                notificationService.sendToUser(dto);
            }
        } catch (Exception ex) {
            System.err.println("[SupportTicketService] Notif al responder: "
                    + ex.getMessage());
        }

        auditService.logJobExecuted(adminId, "SUPPORT_TICKET_RESPONDED",
                "Ticket #" + ticket.getId() + " respondido al usuario "
                        + ticket.getUser().getId(),
                EventResult.SUCCESS);
        return 0;
    }

    /**
     * Elimina permanentemente un ticket. Solo el dueño puede eliminar y
     * solo si está CLOSED (es decir, ya leyó la respuesta).
     *
     * @param ticketId ID del ticket.
     * @param userId   ID del usuario que intenta eliminar.
     * @return 0 si fue eliminado; 2 si no existe; 4 si no es el dueño o el
     *         ticket aún está abierto.
     */
    @Transactional
    public int deleteTicket(Long ticketId, Long userId) {
        Optional<SupportTicket> found = ticketRepo.findById(ticketId);
        if (found.isEmpty()) return 2;

        SupportTicket ticket = found.get();
        if (ticket.getUser() == null
                || !ticket.getUser().getId().equals(userId)) {
            return 4;
        }
        if (ticket.getStatus() != Status.CLOSED) {
            return 4;
        }

        ticketRepo.delete(ticket);
        return 0;
    }

    // =========================================================================
    // Mapper interno
    // =========================================================================

    /**
     * Mapea la entidad a su DTO incluyendo info denormalizada del usuario
     * (username y name) para no obligar al frontend a hacer un GET adicional
     * por cada ticket.
     */
    private SupportTicketDTO toDTO(SupportTicket t) {
        SupportTicketDTO dto = new SupportTicketDTO();
        dto.setId(t.getId());
        if (t.getUser() != null) {
            dto.setUserId(t.getUser().getId());
            dto.setUsername(t.getUser().getUsername());
            dto.setUserName(t.getUser().getName());
        }
        dto.setMessage(t.getMessage());
        dto.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        dto.setResponse(t.getResponse());
        dto.setRespondedByAdminId(t.getRespondedByAdminId());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setRespondedAt(t.getRespondedAt());
        return dto;
    }
}