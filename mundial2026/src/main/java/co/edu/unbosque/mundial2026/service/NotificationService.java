/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.NotificationDTO;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de notificaciones para la plataforma Mundial 2026 Hub.
 * Gestiona el envío de notificaciones por dos canales: push (Firebase Cloud
 * Messaging / FCM) y correo electrónico (SendGrid o JavaMail). Evalúa las
 * preferencias de cada usuario antes de enviar y registra evidencia de cada
 * envío en {@link AuditEventService} (qué se envió, a quién, cuándo y por qué
 * canal). Los operadores pueden además enviar notificaciones masivas segmentadas
 * por partido, ciudad o equipo.
 */
@Service
public class NotificationService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AuditEventService auditService;

    /**
     * Dirección de correo remitente de la plataforma.
     */
    private static final String FROM_EMAIL = "noreply@mundial2026hub.com";

    /**
     * Constructor por defecto requerido por Spring.
     */
    public NotificationService() {
    }

    // =========================================================================
    // Notificaciones personalizadas
    // =========================================================================

    /**
     * Envía una notificación a un usuario específico usando los canales
     * que tenga habilitados en sus preferencias (push y/o email).
     * Registra el resultado del envío en auditoría.
     *
     * @param dto El DTO con los datos de la notificación.
     */
    public void sendToUser(NotificationDTO dto) {
        if (dto.getTargetUserId() == null) return;

        userRepo.findById(dto.getTargetUserId()).ifPresent(user -> {
            boolean sent = false;

            if (user.isPushNotificationsEnabled()) {
                sent = sendPush(user, dto.getTitle(), dto.getBody(),
                        dto.getNotificationType(), dto.getResourceId());
            }
            if (user.isEmailNotificationsEnabled()) {
                sent = sendEmail(user, dto.getTitle(), dto.getBody()) || sent;
            }

            if (sent) {
                auditService.logNotificationSent(user.getId(), dto.getChannel(), dto.getTitle());
            } else {
                auditService.logNotificationFailed(user.getId(),
                        "Usuario sin canales habilitados o error en envío");
            }
        });
    }

    /**
     * Envía una notificación masiva a todos los usuarios suscritos a un equipo
     * específico (por {@code favoriteTeamCode}). Se usa para alertas de inicio
     * de partido y goles.
     *
     * @param teamIsoCode      El código ISO del equipo (ej. "COL", "BRA").
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación para enrutamiento en el cliente.
     * @param resourceId       ID del partido o recurso asociado.
     * @return Número de usuarios notificados.
     */
    public int sendToTeamFollowers(String teamIsoCode, String title,
                                    String body, String notificationType, Long resourceId) {
        List<User> followers = userRepo.findAll().stream()
                .filter(u -> teamIsoCode.equals(u.getFavoriteTeamCode()))
                .toList();

        int notified = 0;
        for (User user : followers) {
            if (user.isPushNotificationsEnabled()) {
                boolean ok = sendPush(user, title, body, notificationType, resourceId);
                if (ok) {
                    auditService.logNotificationSent(user.getId(), "PUSH", title);
                    notified++;
                }
            }
        }
        return notified;
    }

    /**
     * Envía una notificación masiva a todos los usuarios cuya ciudad preferida
     * coincide con la ciudad indicada. Se usa para alertas de partidos en una
     * sede específica.
     *
     * @param city             La ciudad del estadio.
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación.
     * @param resourceId       ID del recurso asociado.
     * @return Número de usuarios notificados.
     */
    public int sendToCityFollowers(String city, String title,
                                    String body, String notificationType, Long resourceId) {
        List<User> cityUsers = userRepo.findAll().stream()
                .filter(u -> city.equals(u.getPreferredCity()))
                .toList();

        int notified = 0;
        for (User user : cityUsers) {
            if (user.isPushNotificationsEnabled()) {
                boolean ok = sendPush(user, title, body, notificationType, resourceId);
                if (ok) {
                    auditService.logNotificationSent(user.getId(), "PUSH", title);
                    notified++;
                }
            }
        }
        return notified;
    }

    // =========================================================================
    // Canales de envío
    // =========================================================================

    /**
     * Envía una notificación push via Firebase Cloud Messaging (FCM).
     * En el MVP el token FCM del usuario debe estar almacenado en su perfil.
     * Este método sirve de punto de integración; la implementación real
     * realiza una llamada HTTP a {@code https://fcm.googleapis.com/v1/messages:send}.
     *
     * @param user             El usuario destinatario.
     * @param title            Título de la notificación push.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación para deep-link en el cliente.
     * @param resourceId       ID del recurso para navegación.
     * @return {@code true} si el envío fue exitoso.
     */
    private boolean sendPush(User user, String title, String body,
                              String notificationType, Long resourceId) {
        // Punto de integración con FCM.
        // En la implementación real se hace un POST a:
        //   https://fcm.googleapis.com/v1/projects/{projectId}/messages:send
        // con el token del dispositivo del usuario y el payload de la notificación.
        // Para el MVP se registra el intento y se retorna true como stub.
        System.out.println("[FCM] Enviando push a usuario " + user.getId()
                + " | Título: " + title
                + " | Tipo: " + notificationType
                + " | Recurso: " + resourceId);
        return true;
    }

    /**
     * Envía una notificación por correo electrónico usando JavaMailSender.
     * Mismo patrón que el {@code EmailService} del proyecto VirusDetected.
     *
     * @param user    El usuario destinatario.
     * @param subject Asunto del correo.
     * @param body    Cuerpo HTML del correo.
     * @return {@code true} si el envío fue exitoso.
     */
    private boolean sendEmail(User user, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("[Mundial 2026 Hub] " + subject);
            helper.setFrom(FROM_EMAIL);
            helper.setText(
                    "<html><body><p>" + body + "</p>"
                    + "<p style='color:#888;font-size:12px;'>Mundial 2026 Hub — "
                    + LocalDateTime.now() + "</p></body></html>",
                    true
            );
            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}