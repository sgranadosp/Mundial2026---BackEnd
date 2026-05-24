package co.edu.unbosque.mundial2026.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.NotificationInboxDTO;
import co.edu.unbosque.mundial2026.model.NotificationInbox;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.NotificationInboxRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Servicio que administra el inbox personal de notificaciones de cada
 * usuario. Es el punto único de entrada para:
 *
 * <ol>
 *   <li>Persistir una notificación entrante ({@link #saveForUser}). Este
 *       método lo llama {@link NotificationService} dentro de sus flujos
 *       de envío push, de modo que toda push entregada queda registrada.</li>
 *   <li>Listar el inbox de un usuario para mostrarlo en el frontend.</li>
 *   <li>Marcar como leídas (una por una, o todas a la vez).</li>
 *   <li>Contar las no leídas para badges.</li>
 * </ol>
 *
 * Las operaciones de escritura usan try/catch internamente para no romper
 * el flujo de llamada (ej. si el inbox falla, el push igual debe enviarse).
 */
@Service
public class NotificationInboxService {

    @Autowired
    private NotificationInboxRepository inboxRepo;

    @Autowired
    private UserRepository userRepo;

    /**
     * Persiste una notificación en el inbox del usuario. Si el usuario no
     * existe la operación es no-op (no lanza). Si ocurre un error de BD,
     * se loguea pero no se propaga, para no afectar el flujo de envío
     * push del que es invocado.
     *
     * @param userId Destinatario de la notificación.
     * @param title  Título corto.
     * @param body   Cuerpo del mensaje.
     * @param type   Tipo (ej. "GENERIC", "EXCHANGE_COMPLETED", "POLL_JOINED").
     */
    @Transactional
    public void saveForUser(Long userId, String title, String body, String type) {
        if (userId == null) return;
        try {
            Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                System.err.println("[inbox] Usuario " + userId
                        + " no encontrado; skip persistencia.");
                return;
            }
            NotificationInbox entry = new NotificationInbox(
                    userOpt.get(), title, body, type != null ? type : "GENERIC");
            inboxRepo.save(entry);
        } catch (Exception ex) {
            System.err.println("[inbox] Error guardando notificación para user "
                    + userId + ": " + ex.getMessage());
        }
    }

    /**
     * Devuelve el inbox completo del usuario, de la más reciente a la más
     * antigua. La lista no tiene paginación: dado el volumen esperado
     * (decenas de notificaciones por usuario en el peor caso) no hace
     * falta. Si en el futuro crece, se puede limitar con un slice o
     * agregar paginación real.
     */
    @Transactional(readOnly = true)
    public List<NotificationInboxDTO> getInbox(Long userId) {
        List<NotificationInbox> entries =
                inboxRepo.findByUserIdOrderByOccurredAtDesc(userId);
        List<NotificationInboxDTO> dtoList = new ArrayList<>();
        for (NotificationInbox e : entries) {
            dtoList.add(toDTO(e));
        }
        return dtoList;
    }

    /**
     * Marca como leída una notificación específica. Devuelve true si
     * efectivamente se modificó, false si no se encontró o ya estaba
     * leída.
     */
    @Transactional
    public boolean markAsRead(Long notificationId) {
        Optional<NotificationInbox> opt = inboxRepo.findById(notificationId);
        if (opt.isEmpty()) return false;
        NotificationInbox entry = opt.get();
        if (entry.isRead()) return false;
        entry.setRead(true);
        inboxRepo.save(entry);
        return true;
    }

    /**
     * Marca como leídas todas las notificaciones no leídas del usuario.
     * Devuelve cuántas fueron afectadas.
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        List<NotificationInbox> entries =
                inboxRepo.findByUserIdOrderByOccurredAtDesc(userId);
        int count = 0;
        for (NotificationInbox entry : entries) {
            if (!entry.isRead()) {
                entry.setRead(true);
                count++;
            }
        }
        if (count > 0) {
            inboxRepo.saveAll(entries);
        }
        return count;
    }

    /**
     * Cantidad de notificaciones no leídas del usuario, para mostrar
     * badge en la UI ("3 mensajes nuevos").
     */
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return inboxRepo.countUnreadByUserId(userId);
    }

    /**
     * Mapeo entidad → DTO.
     */
    private NotificationInboxDTO toDTO(NotificationInbox e) {
        return new NotificationInboxDTO(
                e.getId(),
                e.getUser() != null ? e.getUser().getId() : null,
                e.getTitle(),
                e.getBody(),
                e.getType(),
                e.isRead(),
                e.getOccurredAt());
    }
}