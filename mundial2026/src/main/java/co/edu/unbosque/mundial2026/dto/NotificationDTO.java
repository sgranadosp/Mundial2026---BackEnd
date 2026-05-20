/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase de Transferencia de Datos (DTO) para representar una notificación
 * en la plataforma Mundial 2026 Hub.
 * Se usa tanto para notificaciones personalizadas (inicio de partido,
 * gol, cambio de horario) como para comunicados masivos o segmentados
 * enviados por el operador desde el backoffice. Incluye los campos
 * necesarios para que el sistema de notificaciones (FCM para push,
 * SendGrid para email) procese el envío, y para que la auditoría registre
 * qué se envió, a quién y cuándo.
 */
public class NotificationDTO {

    /**
     * Identificador único de la notificación.
     */
    private Long id;

    /**
     * ID del usuario destinatario. Nulo si es una notificación masiva
     * procesada por segmento (partido, ciudad o estadio).
     */
    private Long targetUserId;

    /**
     * Título de la notificación (ej. "¡Arrancó el partido!").
     */
    private String title;

    /**
     * Cuerpo del mensaje de la notificación.
     */
    private String body;

    /**
     * Canal de envío: "PUSH" (FCM), "EMAIL" (SendGrid) o "IN_APP".
     */
    private String channel;

    /**
     * Tipo de notificación para que el cliente enrute la acción al tap:
     * "MATCH_START", "GOAL", "SCHEDULE_CHANGE", "RANKING_UPDATE",
     * "PACKAGE_GRANTED", "EXCHANGE_REQUEST", "SYSTEM".
     */
    private String notificationType;

    /**
     * ID del recurso asociado a la notificación (partido, grupo, intercambio).
     * El cliente usa este valor junto con {@code notificationType} para
     * navegar a la pantalla correcta al abrir la notificación.
     */
    private Long resourceId;

    /**
     * Estado del envío: "PENDING", "SENT" o "FAILED".
     */
    private String deliveryStatus;

    /**
     * Fecha y hora en que la notificación fue enviada o intentada.
     */
    private LocalDateTime sentAt;

    /**
     * Motivo del fallo de entrega, si aplica. Nulo si el envío fue exitoso.
     */
    private String failureReason;

    /**
     * Constructor por defecto de {@code NotificationDTO}.
     */
    public NotificationDTO() {
    }

    /**
     * Constructor con los campos mínimos para crear una notificación.
     *
     * @param targetUserId     ID del usuario destinatario.
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param channel          Canal de envío.
     * @param notificationType Tipo de notificación.
     */
    public NotificationDTO(Long targetUserId, String title, String body,
                           String channel, String notificationType) {
        this.targetUserId = targetUserId;
        this.title = title;
        this.body = body;
        this.channel = channel;
        this.notificationType = notificationType;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID de la notificación. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID del usuario destinatario. */
    public Long getTargetUserId() { return targetUserId; }

    /** @param targetUserId El nuevo ID de usuario destinatario. */
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    /** @return El título de la notificación. */
    public String getTitle() { return title; }

    /** @param title El nuevo título. */
    public void setTitle(String title) { this.title = title; }

    /** @return El cuerpo del mensaje. */
    public String getBody() { return body; }

    /** @param body El nuevo cuerpo. */
    public void setBody(String body) { this.body = body; }

    /** @return El canal de envío. */
    public String getChannel() { return channel; }

    /** @param channel El nuevo canal. */
    public void setChannel(String channel) { this.channel = channel; }

    /** @return El tipo de notificación. */
    public String getNotificationType() { return notificationType; }

    /** @param notificationType El nuevo tipo. */
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    /** @return El ID del recurso asociado. */
    public Long getResourceId() { return resourceId; }

    /** @param resourceId El nuevo ID del recurso. */
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    /** @return El estado del envío. */
    public String getDeliveryStatus() { return deliveryStatus; }

    /** @param deliveryStatus El nuevo estado del envío. */
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    /** @return La fecha y hora del envío. */
    public LocalDateTime getSentAt() { return sentAt; }

    /** @param sentAt La nueva fecha de envío. */
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    /** @return El motivo del fallo de entrega. */
    public String getFailureReason() { return failureReason; }

    /** @param failureReason El nuevo motivo de fallo. */
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NotificationDTO other = (NotificationDTO) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "NotificationDTO [id=" + id + ", targetUserId=" + targetUserId
                + ", title=" + title + ", channel=" + channel
                + ", notificationType=" + notificationType
                + ", deliveryStatus=" + deliveryStatus
                + ", sentAt=" + sentAt + "]";
    }
}