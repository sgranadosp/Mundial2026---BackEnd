/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.NotificationDTO;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.util.AESUtil;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de notificaciones para la plataforma Mundial 2026 Hub.
 * <p>
 * Gestiona el envío de notificaciones por dos canales: push (Firebase Cloud
 * Messaging / FCM) y correo electrónico (JavaMail). Evalúa las preferencias
 * de cada usuario antes de enviar y registra evidencia de cada envío en
 * {@link AuditEventService}.
 * </p>
 * <p>
 * <b>Segmentación por equipo o ciudad:</b> los métodos
 * {@link #sendToTeamFollowers(String, String, String, String, Long)} y
 * {@link #sendToCityFollowers(String, String, String, String, Long)} quedan
 * disponibles como puntos de extensión. En el MVP actual no hay tabla de
 * preferencias de usuario (equipo favorito / ciudad preferida), por lo que
 * retornan 0 sin enviar. Cuando se agregue esa funcionalidad, basta con
 * reemplazar la lógica de filtrado y mantener el resto del flujo intacto.
 * </p>
 */
@Service
public class NotificationService {

    /**
     * Logger para registrar el progreso y los errores del envío.
     */
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AuditEventService auditService;

    /**
     * Cliente de Firebase Cloud Messaging para enviar push al navegador del
     * usuario. Inyectado por la configuración {@code FirebaseConfig}.
     */
    @Autowired
    private FirebaseMessaging firebaseMessaging;

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
     * específico. Reservado para uso futuro cuando exista la tabla de
     * preferencias de usuario (equipo favorito).
     * <p>
     * <b>Estado actual:</b> stub — no envía nada y retorna 0. Cuando se
     * implemente la preferencia de equipo favorito, esta firma puede consumir
     * un nuevo repositorio de preferencias para filtrar usuarios.
     * </p>
     *
     * @param teamIsoCode      El código ISO del equipo (ej. "COL", "BRA").
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación para enrutamiento en el cliente.
     * @param resourceId       ID del partido o recurso asociado.
     * @return Número de usuarios notificados (0 en la implementación actual).
     */
    public int sendToTeamFollowers(String teamIsoCode, String title,
                                    String body, String notificationType, Long resourceId) {
        log.warn("sendToTeamFollowers invocado para equipo {} pero la preferencia"
                + " 'equipo favorito' no está implementada en este MVP. No se envió nada.",
                teamIsoCode);
        return 0;
    }

    /**
     * Envía una notificación masiva a todos los usuarios cuya ciudad preferida
     * coincide con la ciudad indicada. Reservado para uso futuro cuando exista
     * la tabla de preferencias de usuario (ciudad preferida).
     * <p>
     * <b>Estado actual:</b> stub — no envía nada y retorna 0. Cuando se
     * implemente la preferencia de ciudad, esta firma puede consumir un nuevo
     * repositorio de preferencias para filtrar usuarios.
     * </p>
     *
     * @param city             La ciudad del estadio.
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación.
     * @param resourceId       ID del recurso asociado.
     * @return Número de usuarios notificados (0 en la implementación actual).
     */
    public int sendToCityFollowers(String city, String title,
                                    String body, String notificationType, Long resourceId) {
        log.warn("sendToCityFollowers invocado para ciudad {} pero la preferencia"
                + " 'ciudad preferida' no está implementada en este MVP. No se envió nada.",
                city);
        return 0;
    }

    // =========================================================================
    // Canales de envío
    // =========================================================================

    /**
     * Envía una notificación push via Firebase Cloud Messaging (FCM) al
     * token registrado del usuario.
     * <p>
     * Si el usuario no tiene token registrado (nunca autorizó push o no se
     * ha logueado desde un navegador con FCM habilitado), se omite el envío
     * y se retorna {@code false}. Si el token quedó inválido (porque el
     * usuario revocó permisos o cambió de navegador), se loguea un warning
     * y también se retorna {@code false}; idealmente debería limpiarse el
     * token de BD, lo cual queda como mejora futura.
     * </p>
     * <p>
     * Los campos {@code notificationType} y {@code resourceId} se incluyen
     * como {@code data} del mensaje para que el cliente pueda hacer un
     * deep-link al recurso correcto al hacer click en la notificación.
     * </p>
     *
     * @param user             El usuario destinatario.
     * @param title            Título de la notificación push.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación para deep-link en el cliente.
     * @param resourceId       ID del recurso para navegación.
     * @return {@code true} si FCM aceptó el envío.
     */
    private boolean sendPush(User user, String title, String body,
                              String notificationType, Long resourceId) {
        String token = user.getFcmToken();
        if (token == null || token.isBlank()) {
            log.debug("[FCM] Usuario {} no tiene token registrado, push omitido", user.getId());
            return false;
        }

        try {
            // Construye el mensaje con tres bloques:
            //   1. notification: lo que muestra el SO/navegador al usuario.
            //   2. data: payload que recibe el Service Worker / la app para
            //      hacer deep-link al recurso.
            //   3. webpush: opciones específicas para navegadores (icono,
            //      click action, etc.).
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", notificationType != null ? notificationType : "")
                    .putData("resourceId", resourceId != null ? resourceId.toString() : "")
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setIcon("/favicon.png")
                                    .build())
                            .putHeader("Urgency", "high")
                            .build())
                    .build();

            String fcmResponse = firebaseMessaging.send(message);
            log.info("[FCM] Push enviado a user={} | type={} | resource={} | messageId={}",
                    user.getId(), notificationType, resourceId, fcmResponse);
            return true;

        } catch (FirebaseMessagingException e) {
            // Errores típicos:
            //   - UNREGISTERED: el token fue revocado o ya no es válido
            //   - INVALID_ARGUMENT: token malformado
            //   - SENDER_ID_MISMATCH: token de otro proyecto Firebase
            log.warn("[FCM] Falló envío a user={} | code={} | msg={}",
                    user.getId(), e.getMessagingErrorCode(), e.getMessage());
            // Si el token quedó inválido, lo limpiamos de BD para no seguir
            // intentando enviar a un destino muerto.
            if (e.getMessagingErrorCode() != null
                    && ("UNREGISTERED".equals(e.getMessagingErrorCode().name())
                            || "INVALID_ARGUMENT".equals(e.getMessagingErrorCode().name()))) {
                user.setFcmToken(null);
                userRepo.save(user);
                log.info("[FCM] Token inválido limpiado para user={}", user.getId());
            }
            return false;
        }
    }

    /**
     * Envía una notificación por correo electrónico usando JavaMailSender.
     * El email del usuario está almacenado encriptado con AES en BD; se
     * desencripta antes de usarlo como destinatario SMTP.
     *
     * @param user    El usuario destinatario.
     * @param subject Asunto del correo.
     * @param body    Cuerpo HTML del correo.
     * @return {@code true} si el envío fue exitoso.
     */
    private boolean sendEmail(User user, String subject, String body) {
        try {
            String plainEmail = AESUtil.decrypt(user.getEmail());
            if (plainEmail == null || plainEmail.isEmpty()) {
                log.warn("No se pudo desencriptar el email del usuario {}", user.getId());
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(plainEmail);
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
            log.error("Error enviando correo a usuario {}: {}", user.getId(), e.getMessage());
            return false;
        }
    }
}