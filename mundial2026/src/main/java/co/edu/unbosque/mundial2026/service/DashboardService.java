/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.DashboardSummaryDTO;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventType;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.Ticket.TicketStatus;
import co.edu.unbosque.mundial2026.repository.AuditEventRepository;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.PollGroupRepository;
import co.edu.unbosque.mundial2026.repository.PredictionRepository;
import co.edu.unbosque.mundial2026.repository.TicketRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Servicio que computa las métricas agregadas del Dashboard administrativo
 * (HU16/HU20 — Resumen general del sistema).
 * <p>
 * Cada llamada a {@link #buildSummary()} ejecuta entre 8 y 10 consultas de
 * conteo sobre las tablas {@code users}, {@code matches}, {@code poll_groups},
 * {@code predictions}, {@code tickets} y {@code audit_events}. Todas son
 * {@code COUNT} con índices ya existentes, así que el costo es bajo y se
 * puede invocar por polling cada 15 segundos sin saturar la BD.
 * </p>
 */
@Service
public class DashboardService {

    /**
     * Ventana para considerar un usuario como "nuevo esta semana" (7 días).
     */
    private static final int NEW_USERS_WINDOW_DAYS = 7;

    /**
     * Ventana para considerar una reserva como "expira pronto" (15 minutos).
     */
    private static final int EXPIRING_SOON_WINDOW_MINUTES = 15;

    /**
     * Repositorio de usuarios para el total.
     */
    @Autowired
    private UserRepository userRepo;

    /**
     * Repositorio de auditoría para contar registros nuevos esta semana.
     */
    @Autowired
    private AuditEventRepository auditRepo;

    /**
     * Repositorio de partidos para totales y conteo en vivo.
     */
    @Autowired
    private MatchRepository matchRepo;

    /**
     * Repositorio de grupos de polla para el total.
     */
    @Autowired
    private PollGroupRepository pollGroupRepo;

    /**
     * Repositorio de pronósticos para el total.
     */
    @Autowired
    private PredictionRepository predictionRepo;

    /**
     * Repositorio de entradas para activas y expirando pronto.
     */
    @Autowired
    private TicketRepository ticketRepo;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public DashboardService() {
    }

    /**
     * Construye el resumen del dashboard ejecutando todas las consultas de
     * conteo necesarias.
     *
     * @return DTO con todas las métricas agregadas, listo para serializar.
     */
    public DashboardSummaryDTO buildSummary() {
        LocalDateTime now = LocalDateTime.now();

        // ─── Usuarios ─────────────────────────────────────────────────────
        long totalUsers = userRepo.count();
        long newUsersThisWeek = auditRepo.countByEventTypeAndOccurredAtBetween(
                EventType.USER_REGISTERED,
                now.minusDays(NEW_USERS_WINDOW_DAYS),
                now);

        // ─── Partidos ─────────────────────────────────────────────────────
        long totalMatches = matchRepo.count();
        long liveMatches = matchRepo.countByStatus(MatchStatus.LIVE);

        // ─── Pollas y pronósticos ─────────────────────────────────────────
        long totalPollGroups = pollGroupRepo.count();
        long totalPredictions = predictionRepo.count();

        // ─── Entradas ─────────────────────────────────────────────────────
        long activeTickets = ticketRepo.countByStatusIn(
                List.of(TicketStatus.RESERVED, TicketStatus.PAID));
        long ticketsExpiringSoon = ticketRepo.countByStatusAndReservationExpiresAtBetween(
                TicketStatus.RESERVED,
                now,
                now.plusMinutes(EXPIRING_SOON_WINDOW_MINUTES));

        return new DashboardSummaryDTO(
                totalUsers, newUsersThisWeek,
                totalMatches, liveMatches,
                totalPollGroups, totalPredictions,
                activeTickets, ticketsExpiringSoon);
    }
}
