package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Notificación persistida en el "inbox" personal de un usuario.
 *
 * <p>Cada vez que el sistema dispara una notificación push hacia un usuario
 * (sea desde el panel admin, desde un evento automático del sistema, o
 * como consecuencia de una acción social como unirse a una polla o
 * completar un intercambio), se inserta una fila en esta tabla. Eso
 * permite que el usuario consulte después su historial de notificaciones
 * desde la pantalla "Notificaciones" del frontend incluso si el push se
 * envió cuando no tenía el navegador abierto.</p>
 *
 * <p>La tabla no reemplaza al push FCM: este se sigue enviando en
 * paralelo. El inbox es estrictamente para mostrar el historial al
 * usuario dentro de la app.</p>
 *
 * <p>Campos importantes:
 * <ul>
 *   <li>{@code type}: categoría de la notificación. Usamos los mismos
 *       valores que ya manejaba {@link NotificationDTO#getNotificationType()}
 *       (ej. "MATCH_START", "GOAL", "EXCHANGE_COMPLETED", "POLL_JOINED",
 *       "VENUE_INFO", "GENERIC"). Es un VARCHAR libre para no atarse a
 *       un enum y permitir futuros tipos sin migración.</li>
 *   <li>{@code read}: marca si el usuario ya vio la notificación. Empieza
 *       en false; se actualiza con PUT /notifications/inbox/{id}/read.</li>
 *   <li>{@code occurredAt}: instante UTC del disparo de la notificación.
 *       Se setea automáticamente en la creación.</li>
 * </ul></p>
 */
@Entity
@Table(name = "notification_inbox")
public class NotificationInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario destinatario de la notificación. La relación es muchos a
     * uno: un usuario puede tener muchas notificaciones en su inbox.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Título corto que mostrará la card en el frontend (max ~80 chars).
     */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /**
     * Cuerpo completo del mensaje. Sin límite estricto pero se recomienda
     * mantenerlo bajo 500 chars para que se vea bien en mobile.
     */
    @Column(name = "body", length = 2000, nullable = false)
    private String body;

    /**
     * Tipo de la notificación. Texto libre para permitir nuevos tipos
     * sin migración de esquema. Valores comunes: "GENERIC",
     * "MATCH_START", "GOAL", "SCHEDULE_CHANGE", "EXCHANGE_COMPLETED",
     * "POLL_JOINED", "VENUE_INFO".
     */
    @Column(name = "type", length = 64, nullable = false)
    private String type;

    /**
     * Flag de "leída". Empieza en false; el frontend marca como true
     * cuando el usuario abre la notificación.
     */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /**
     * Timestamp UTC del envío. Se setea automáticamente al crear el
     * registro y nunca se modifica después.
     */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // =========================================================================
    // Constructores
    // =========================================================================

    /** Constructor vacío requerido por JPA. */
    public NotificationInbox() {
    }

    /**
     * Constructor de conveniencia: setea automáticamente {@code occurredAt=now}
     * y {@code read=false}.
     */
    public NotificationInbox(User user, String title, String body, String type) {
        this.user = user;
        this.title = title;
        this.body = body;
        this.type = type;
        this.read = false;
        this.occurredAt = LocalDateTime.now();
    }

    // =========================================================================
    // Getters y setters
    // =========================================================================

    /** @return ID interno de la notificación. */
    public Long getId() { return id; }

    /** @param id Nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return Usuario destinatario. */
    public User getUser() { return user; }

    /** @param user Nuevo destinatario. */
    public void setUser(User user) { this.user = user; }

    /** @return Título de la notificación. */
    public String getTitle() { return title; }

    /** @param title Nuevo título. */
    public void setTitle(String title) { this.title = title; }

    /** @return Cuerpo de la notificación. */
    public String getBody() { return body; }

    /** @param body Nuevo cuerpo. */
    public void setBody(String body) { this.body = body; }

    /** @return Tipo de la notificación. */
    public String getType() { return type; }

    /** @param type Nuevo tipo. */
    public void setType(String type) { this.type = type; }

    /** @return true si el usuario ya vio la notificación. */
    public boolean isRead() { return read; }

    /** @param read Nuevo estado de lectura. */
    public void setRead(boolean read) { this.read = read; }

    /** @return Timestamp del envío. */
    public LocalDateTime getOccurredAt() { return occurredAt; }

    /** @param occurredAt Nuevo timestamp del envío. */
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    // =========================================================================
    // equals / hashCode / toString
    // =========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationInbox other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NotificationInbox{id=" + id
                + ", userId=" + (user != null ? user.getId() : null)
                + ", title='" + title + '\''
                + ", type='" + type + '\''
                + ", read=" + read
                + ", occurredAt=" + occurredAt + '}';
    }
}