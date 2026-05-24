package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO de un ticket de soporte. Espejo plano de
 * {@link co.edu.unbosque.mundial2026.model.SupportTicket} para serializar
 * en JSON sin exponer la entidad JPA ni el grafo de relaciones (User).
 */
public class SupportTicketDTO {

    private Long id;
    private Long userId;
    private String username;
    private String userName;
    private String message;
    private String status;
    private String response;
    private Long respondedByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    /** Constructor por defecto. */
    public SupportTicketDTO() {
    }

    /**
     * Constructor con todos los campos.
     */
    public SupportTicketDTO(Long id, Long userId, String username, String userName,
                             String message, String status, String response,
                             Long respondedByAdminId, LocalDateTime createdAt,
                             LocalDateTime respondedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.userName = userName;
        this.message = message;
        this.status = status;
        this.response = response;
        this.respondedByAdminId = respondedByAdminId;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
    }

    // ─── Getters y setters ───────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Long getRespondedByAdminId() { return respondedByAdminId; }
    public void setRespondedByAdminId(Long respondedByAdminId) {
        this.respondedByAdminId = respondedByAdminId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupportTicketDTO)) return false;
        SupportTicketDTO that = (SupportTicketDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SupportTicketDTO [id=" + id
                + ", userId=" + userId
                + ", status=" + status
                + ", createdAt=" + createdAt + "]";
    }
}