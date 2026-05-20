/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.TicketDTO;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Ticket;
import co.edu.unbosque.mundial2026.model.Ticket.TicketCategory;
import co.edu.unbosque.mundial2026.model.Ticket.TicketStatus;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.TicketRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Servicio encargado del ciclo de vida de entradas digitales en la plataforma
 * Mundial 2026 Hub.
 * El ciclo es: {@code RESERVED → PAID → (TRANSFERRED | REFUNDED | EXPIRED)}.
 * Cada transición deja trazabilidad completa vía {@code correlationId}.
 * Las reservas tienen un TTL configurable ({@value #RESERVATION_TTL_MINUTES} minutos);
 * si no se paga dentro de la ventana, el job {@code expireReservations()} libera el cupo.
 * Los pagos se procesan en modo sandbox (Stripe test o WireMock).
 * Se aplican límites antifraude: máximo {@value #MAX_ACTIVE_TICKETS} entradas
 * activas (RESERVED o PAID) por usuario a la vez.
 */
@Service
public class TicketService {

    /**
     * Tiempo máximo en minutos que una reserva puede estar sin pagar
     * antes de expirar automáticamente.
     */
    private static final int RESERVATION_TTL_MINUTES = 15;

    /**
     * Número máximo de entradas activas (RESERVED o PAID) por usuario.
     * Superar este límite activa una alerta antifraude.
     */
    private static final int MAX_ACTIVE_TICKETS = 4;

    @Autowired
    private TicketRepository ticketRepo;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuditEventService auditService;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public TicketService() {
    }

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    /**
     * Crea una reserva de entrada para un partido. Genera un {@code correlationId}
     * único para toda la trazabilidad de la operación. Verifica los límites
     * antifraude antes de proceder.
     *
     * @param matchId  El ID del partido.
     * @param userId   El ID del usuario que reserva.
     * @param category La categoría de la entrada.
     * @param price    El precio simulado en USD.
     * @return El {@link TicketDTO} de la reserva creada; {@code null} si el partido
     *         o usuario no existen o el partido no está en estado {@code SCHEDULED}.
     *         Retorna un DTO con {@code status = null} si se supera el límite antifraude.
     */
    public TicketDTO reserveTicket(Long matchId, Long userId, TicketCategory category, Double price) {
        Optional<Match> match = matchRepo.findById(matchId);
        Optional<User> user = userRepo.findById(userId);

        if (match.isEmpty() || user.isEmpty()) return null;
        if (match.get().getStatus() != co.edu.unbosque.mundial2026.model.Match.MatchStatus.SCHEDULED) {
            return null;
        }

        long activeTickets = ticketRepo.countActiveTicketsByOriginalBuyerId(userId);
        if (activeTickets >= MAX_ACTIVE_TICKETS) {
            auditService.logFraudPattern(userId, "Superó límite de " + MAX_ACTIVE_TICKETS + " entradas activas");
            return null;
        }

        String correlationId = generateUniqueCorrelationId();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES);

        Ticket ticket = new Ticket(correlationId, match.get(), user.get(), user.get(),
                category, price, expiresAt);
        ticketRepo.save(ticket);

        auditService.logTicketReserved(userId, correlationId, matchId);
        return toDTO(ticket);
    }

    /**
     * Confirma el pago de una entrada reservada. Actualiza el estado a {@code PAID}
     * y registra el ID de la transacción sandbox (Stripe o WireMock).
     *
     * @param ticketId             El ID de la entrada.
     * @param userId               El ID del usuario titular (validación).
     * @param paymentTransactionId El ID de la transacción en el sistema de pagos.
     * @return 0 si fue confirmado; 2 si no existe; 4 si la reserva expiró o el
     *         usuario no es el titular.
     */
    public int confirmPayment(Long ticketId, Long userId, String paymentTransactionId) {
        Optional<Ticket> found = ticketRepo.findById(ticketId);
        if (found.isEmpty()) return 2;

        Ticket ticket = found.get();
        if (!ticket.getHolder().getId().equals(userId)) return 4;
        if (ticket.getStatus() != TicketStatus.RESERVED) return 4;
        if (LocalDateTime.now().isAfter(ticket.getReservationExpiresAt())) {
            ticket.setStatus(TicketStatus.EXPIRED);
            ticketRepo.save(ticket);
            return 4;
        }

        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(LocalDateTime.now());
        ticket.setPaymentTransactionId(paymentTransactionId);
        ticketRepo.save(ticket);

        auditService.logTicketPaid(userId, ticket.getCorrelationId());
        return 0;
    }

    /**
     * Transfiere una entrada pagada a otro usuario. Registra quién transfiere,
     * quién recibe y cuándo (no repudio). Solo se permiten entradas en estado
     * {@code PAID}.
     *
     * @param ticketId    El ID de la entrada.
     * @param fromUserId  El ID del usuario que transfiere (debe ser el titular).
     * @param toUserId    El ID del usuario que recibe.
     * @return 0 si fue transferida; 2 si alguna entidad no existe; 4 si el estado
     *         no permite transferencia o el solicitante no es el titular.
     */
    public int transferTicket(Long ticketId, Long fromUserId, Long toUserId) {
        Optional<Ticket> found = ticketRepo.findById(ticketId);
        Optional<User> toUser = userRepo.findById(toUserId);

        if (found.isEmpty() || toUser.isEmpty()) return 2;

        Ticket ticket = found.get();
        if (!ticket.getHolder().getId().equals(fromUserId)) return 4;
        if (ticket.getStatus() != TicketStatus.PAID) return 4;

        ticket.setHolder(toUser.get());
        ticket.setStatus(TicketStatus.TRANSFERRED);
        ticketRepo.save(ticket);

        auditService.logTicketTransferred(fromUserId, toUserId, ticket.getCorrelationId());
        return 0;
    }

    /**
     * Procesa el reembolso de una entrada. Aplica las reglas de negocio de
     * elegibilidad (solo entradas {@code PAID}) y registra la operación
     * con el ID de transacción del reembolso sandbox.
     *
     * @param ticketId             El ID de la entrada.
     * @param userId               El ID del usuario titular.
     * @param refundTransactionId  El ID de la transacción de reembolso sandbox.
     * @return 0 si fue procesado; 2 si no existe; 4 si no es elegible para
     *         reembolso o el usuario no es el titular.
     */
    public int refundTicket(Long ticketId, Long userId, String refundTransactionId) {
        Optional<Ticket> found = ticketRepo.findById(ticketId);
        if (found.isEmpty()) return 2;

        Ticket ticket = found.get();
        if (!ticket.getHolder().getId().equals(userId)) return 4;
        if (ticket.getStatus() != TicketStatus.PAID) return 4;

        ticket.setStatus(TicketStatus.REFUNDED);
        ticket.setPaymentTransactionId(refundTransactionId);
        ticketRepo.save(ticket);

        auditService.logTicketRefunded(userId, ticket.getCorrelationId());
        return 0;
    }

    /**
     * Job de expiración de reservas. Busca todas las entradas {@code RESERVED}
     * cuyo TTL ya venció y las marca como {@code EXPIRED}, liberando el cupo.
     * Debe invocarse periódicamente desde un {@code @Scheduled} en el controlador
     * o en una clase de configuración.
     *
     * @return Número de reservas que fueron expiradas.
     */
    public int expireReservations() {
        List<Ticket> expired = ticketRepo.findByStatusAndReservationExpiresAtBefore(
                TicketStatus.RESERVED, LocalDateTime.now());
        expired.forEach(ticket -> {
            ticket.setStatus(TicketStatus.EXPIRED);
            ticketRepo.save(ticket);
            auditService.logTicketExpired(ticket.getHolder().getId(), ticket.getCorrelationId());
        });
        return expired.size();
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    /**
     * Obtiene las entradas activas (PAID) de un usuario.
     *
     * @param userId El ID del usuario.
     * @return Lista de {@link TicketDTO} de entradas pagadas del usuario.
     */
    public List<TicketDTO> getPaidTicketsByUser(Long userId) {
        List<Ticket> tickets = ticketRepo.findByHolderIdAndStatus(userId, TicketStatus.PAID);
        List<TicketDTO> dtoList = new ArrayList<>();
        tickets.forEach(t -> dtoList.add(toDTO(t)));
        return dtoList;
    }

    /**
     * Obtiene el historial completo de entradas de un usuario (todos los estados).
     *
     * @param userId El ID del usuario.
     * @return Lista de {@link TicketDTO} del historial del usuario.
     */
    public List<TicketDTO> getTicketHistoryByUser(Long userId) {
        List<Ticket> tickets = ticketRepo.findByOriginalBuyerId(userId);
        List<TicketDTO> dtoList = new ArrayList<>();
        tickets.forEach(t -> dtoList.add(toDTO(t)));
        return dtoList;
    }

    /**
     * Busca una entrada por su {@code correlationId} para soporte y auditoría.
     *
     * @param correlationId El ID de correlación de la entrada.
     * @return El {@link TicketDTO} correspondiente, o {@code null} si no existe.
     */
    public TicketDTO getByCorrelationId(String correlationId) {
        Optional<Ticket> found = ticketRepo.findByCorrelationId(correlationId);
        return found.map(this::toDTO).orElse(null);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Genera un ID de correlación UUID único garantizando que no exista en BD.
     *
     * @return UUID único como string.
     */
    private String generateUniqueCorrelationId() {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (ticketRepo.existsByCorrelationId(id));
        return id;
    }

    /**
     * Convierte una entidad {@link Ticket} a {@link TicketDTO} con datos del
     * partido, estadio y usuarios aplanados.
     *
     * @param ticket La entidad a convertir.
     * @return El DTO de la entrada.
     */
    private TicketDTO toDTO(Ticket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setCorrelationId(ticket.getCorrelationId());
        dto.setStatus(ticket.getStatus());
        dto.setCategory(ticket.getCategory());
        dto.setPrice(ticket.getPrice());
        dto.setReservedAt(ticket.getReservedAt());
        dto.setReservationExpiresAt(ticket.getReservationExpiresAt());
        dto.setPaidAt(ticket.getPaidAt());
        dto.setPaymentTransactionId(ticket.getPaymentTransactionId());

        if (ticket.getHolder() != null) {
            dto.setHolderId(ticket.getHolder().getId());
            dto.setHolderUsername(ticket.getHolder().getUsername());
        }
        if (ticket.getOriginalBuyer() != null) {
            dto.setOriginalBuyerId(ticket.getOriginalBuyer().getId());
            dto.setOriginalBuyerUsername(ticket.getOriginalBuyer().getUsername());
        }
        if (ticket.getMatch() != null) {
            dto.setMatchId(ticket.getMatch().getId());
            dto.setMatchScheduledAt(ticket.getMatch().getScheduledAt());
            if (ticket.getMatch().getHomeTeam() != null) {
                dto.setHomeTeamName(ticket.getMatch().getHomeTeam().getName());
            }
            if (ticket.getMatch().getAwayTeam() != null) {
                dto.setAwayTeamName(ticket.getMatch().getAwayTeam().getName());
            }
            if (ticket.getMatch().getStadium() != null) {
                dto.setStadiumName(ticket.getMatch().getStadium().getName());
                dto.setStadiumCity(ticket.getMatch().getStadium().getCity());
            }
        }
        return dto;
    }
}