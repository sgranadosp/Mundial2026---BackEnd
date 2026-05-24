package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ticket de soporte enviado por un usuario al equipo administrador.
 *
 * <p>El flujo es muy simple: el usuario escribe un mensaje desde la
 * sección "Contacto y Soporte", el ticket queda en estado {@code OPEN}, el
 * admin lo ve en su panel "Tickets de Soporte" y responde con un mensaje
 * libre. Cuando el admin responde, el ticket pasa a {@code CLOSED} y se
 * dispara una notificación push automática al usuario para que sepa que
 * ya tiene respuesta. El usuario puede eliminar (ocultar) el ticket
 * después de leer la respuesta.</p>
 *
 * <p>No hay hilo de conversación: una pregunta + una respuesta. Si el
 * usuario quiere continuar, abre un ticket nuevo.</p>
 */
@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    /**
     * Estados posibles del ticket. Un ticket nace OPEN y pasa a CLOSED
     * cuando el admin lo responde. No hay vuelta atrás.
     */
    public enum Status {
        /** El ticket está pendiente de respuesta del admin. */
        OPEN,
        /** El admin ya respondió. */
        CLOSED
    }

    /** Identificador autogenerado por la BD. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que creó el ticket. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Mensaje original del usuario. */
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    /** Estado actual del ticket (OPEN / CLOSED). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    /** Respuesta libre del admin. {@code null} si aún no ha respondido. */
    @Column(name = "response", length = 2000)
    private String response;

    /** ID del admin que respondió. {@code null} si aún no responde nadie. */
    @Column(name = "responded_by_admin_id")
    private Long respondedByAdminId;

    /** Momento de creación del ticket. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Momento en que el admin respondió. {@code null} mientras está OPEN. */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public SupportTicket() {
    }

    /**
     * Constructor de conveniencia para crear un ticket nuevo. Inicializa
     * el estado en OPEN y el momento de creación en {@code now()}.
     *
     * @param user    Usuario remitente.
     * @param message Mensaje original.
     */
    public SupportTicket(User user, String message) {
        this.user = user;
        this.message = message;
        this.status = Status.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    // ─── Getters y setters ───────────────────────────────────────────────

    /** @return El ID del ticket. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El usuario remitente. */
    public User getUser() { return user; }

    /** @param user El nuevo remitente. */
    public void setUser(User user) { this.user = user; }

    /** @return El mensaje original del usuario. */
    public String getMessage() { return message; }

    /** @param message El nuevo mensaje. */
    public void setMessage(String message) { this.message = message; }

    /** @return El estado actual. */
    public Status getStatus() { return status; }

    /** @param status El nuevo estado. */
    public void setStatus(Status status) { this.status = status; }

    /** @return La respuesta del admin o {@code null}. */
    public String getResponse() { return response; }

    /** @param response La nueva respuesta. */
    public void setResponse(String response) { this.response = response; }

    /** @return El ID del admin que respondió o {@code null}. */
    public Long getRespondedByAdminId() { return respondedByAdminId; }

    /** @param respondedByAdminId El nuevo ID del admin. */
    public void setRespondedByAdminId(Long respondedByAdminId) {
        this.respondedByAdminId = respondedByAdminId;
    }

    /** @return Momento de creación. */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt El nuevo momento de creación. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return Momento de respuesta o {@code null}. */
    public LocalDateTime getRespondedAt() { return respondedAt; }

    /** @param respondedAt El nuevo momento de respuesta. */
    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupportTicket)) return false;
        SupportTicket that = (SupportTicket) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SupportTicket [id=" + id
                + ", userId=" + (user != null ? user.getId() : null)
                + ", status=" + status
                + ", createdAt=" + createdAt + "]";
    }
}