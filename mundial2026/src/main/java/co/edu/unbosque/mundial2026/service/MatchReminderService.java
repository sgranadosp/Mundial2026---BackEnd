/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.NotificationDTO;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Ticket;
import co.edu.unbosque.mundial2026.model.Ticket.TicketStatus;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.TicketRepository;

/**
 * Servicio que dispara notificaciones programadas según el calendario del
 * Mundial.
 * <p>
 * Por ahora implementa un único job:
 * <ul>
 *   <li><b>Recordatorio "el partido empieza pronto"</b>: cada 5 minutos
 *       revisa qué partidos arrancan en los próximos 60 minutos. A los
 *       usuarios que compraron ticket para ese partido, les envía una
 *       notificación push.</li>
 * </ul>
 * </p>
 * <p>
 * Para evitar duplicados, mantiene en memoria los IDs de partidos ya
 * notificados durante la sesión actual de la JVM. Esto basta para el MVP;
 * si la app se reinicia justo en la ventana de 1 hora, podría haber un
 * duplicado, pero el FCM duplicate no es un problema crítico.
 * </p>
 */
@Service
public class MatchReminderService {

    private static final Logger log = LoggerFactory.getLogger(MatchReminderService.class);

    /** Cuán cerca debe estar el partido para notificar (en minutos). */
    private static final long REMINDER_WINDOW_MINUTES = 60;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private TicketRepository ticketRepo;

    @Autowired
    private NotificationService notificationService;

    /** IDs de partidos ya notificados, para no enviar dos veces. */
    private final Set<Long> alreadyNotified = new HashSet<>();

    /** Constructor por defecto. */
    public MatchReminderService() {
    }

    /**
     * Cada 5 minutos busca partidos que empiezan en la próxima hora y manda
     * push a los compradores de tickets.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000L) // 5 minutos
    public void notifyUpcomingMatches() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(REMINDER_WINDOW_MINUTES);

        List<Match> upcoming = matchRepo.findAll().stream()
                .filter(m -> m.getScheduledAt() != null)
                .filter(m -> m.getScheduledAt().isAfter(now)
                        && m.getScheduledAt().isBefore(windowEnd))
                .filter(m -> !alreadyNotified.contains(m.getId()))
                .toList();

        if (upcoming.isEmpty()) return;

        log.info("[reminders] Encontrados {} partidos próximos en ventana de {} min",
                upcoming.size(), REMINDER_WINDOW_MINUTES);

        for (Match m : upcoming) {
            // Compradores únicos de tickets pagados para este partido.
            Set<Long> buyers = ticketRepo.findByMatchIdAndStatus(m.getId(), TicketStatus.PAID).stream()
                    .filter(t -> t.getHolder() != null)
                    .map(t -> t.getHolder().getId())
                    .collect(java.util.stream.Collectors.toSet());

            if (buyers.isEmpty()) {
                // Aún así marcamos como notificado para no recalcular cada 5 min.
                alreadyNotified.add(m.getId());
                continue;
            }

            String homeName = m.getHomeTeam() != null ? m.getHomeTeam().getName() : "?";
            String awayName = m.getAwayTeam() != null ? m.getAwayTeam().getName() : "?";
            String title = "El partido empieza pronto";
            String body = String.format("%s vs %s arranca en menos de una hora. ¡Prepárate!",
                    homeName, awayName);

            for (Long userId : buyers) {
                NotificationDTO n = new NotificationDTO();
                n.setTargetUserId(userId);
                n.setTitle(title);
                n.setBody(body);
                n.setChannel("PUSH");
                n.setNotificationType("MATCH_STARTING_SOON");
                n.setResourceId(m.getId());
                try {
                    notificationService.sendToUser(n);
                } catch (Exception e) {
                    log.warn("[reminders] Fallo notif a user={}: {}", userId, e.getMessage());
                }
            }
            alreadyNotified.add(m.getId());
            log.info("[reminders] Notificado match={} a {} usuario(s)", m.getId(), buyers.size());
        }
    }
}