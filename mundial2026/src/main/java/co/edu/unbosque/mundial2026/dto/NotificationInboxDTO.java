package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO de salida para una notificación del inbox del usuario.
 *
 * <p>Se construye a partir de la entidad {@code NotificationInbox} y se
 * envía al frontend como parte del listado de notificaciones del usuario.
 * El campo {@code userId} es solo para referencia: el endpoint que devuelve
 * estos DTOs ya filtra por el usuario autenticado.</p>
 */
public class NotificationInboxDTO {

    private Long id;
    private Long userId;
    private String title;
    private String body;
    private String type;
    private boolean read;
    private LocalDateTime occurredAt;

    /** Constructor vacío para Jackson y similar. */
    public NotificationInboxDTO() {
    }

    /** Constructor completo. */
    public NotificationInboxDTO(Long id, Long userId, String title, String body,
                                 String type, boolean read, LocalDateTime occurredAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.read = read;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationInboxDTO other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NotificationInboxDTO{id=" + id
                + ", userId=" + userId
                + ", title='" + title + '\''
                + ", type='" + type + '\''
                + ", read=" + read
                + ", occurredAt=" + occurredAt + '}';
    }
}