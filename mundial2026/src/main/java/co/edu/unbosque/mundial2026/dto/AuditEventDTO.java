/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.unbosque.mundial2026.model.AuditEvent.EventResult;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventType;

/**
 * Clase de Transferencia de Datos (DTO) para representar un evento de
 * auditoría en la plataforma Mundial 2026 Hub.
 * Solo accesible para usuarios con rol {@code ADMIN}. Se usa en el panel de
 * soporte y en el módulo de compliance para consultar la línea de tiempo de
 * eventos de un usuario o de una operación específica (filtrado por
 * {@code correlationId}). El campo {@code payload} solo se incluye cuando el
 * administrador solicita el detalle completo del evento; en las listas se omite
 * para reducir el tamaño de la respuesta.
 */
public class AuditEventDTO {

    /**
     * Identificador único del evento de auditoría.
     */
    private Long id;

    /**
     * Tipo del evento.
     */
    private EventType eventType;

    /**
     * ID del usuario asociado al evento. Nulo para eventos de sistema.
     */
    private Long userId;

    /**
     * Nombre de usuario asociado al evento.
     */
    private String username;

    /**
     * ID de correlación que agrupa eventos de una misma operación.
     */
    private String correlationId;

    /**
     * Descripción legible del evento para el panel de soporte.
     */
    private String description;

    /**
     * Payload JSON con el detalle completo del evento.
     * Solo se incluye en consultas de detalle, no en listados.
     */
    private String payload;

    /**
     * Fecha y hora exacta en que ocurrió el evento (UTC).
     */
    private LocalDateTime occurredAt;

    /**
     * Dirección IP de origen de la acción. Nulo para eventos del sistema.
     */
    private String sourceIp;

    /**
     * Resultado del evento: SUCCESS, FAILURE o BLOCKED.
     */
    private EventResult result;

    /**
     * Constructor por defecto de {@code AuditEventDTO}.
     */
    public AuditEventDTO() {
    }

    /**
     * Constructor con los campos principales del evento de auditoría.
     *
     * @param eventType     Tipo del evento.
     * @param userId        ID del usuario asociado.
     * @param correlationId ID de correlación.
     * @param description   Descripción del evento.
     * @param result        Resultado del evento.
     */
    public AuditEventDTO(EventType eventType, Long userId, String correlationId,
                         String description, EventResult result) {
        this.eventType = eventType;
        this.userId = userId;
        this.correlationId = correlationId;
        this.description = description;
        this.result = result;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del evento. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El tipo del evento. */
    public EventType getEventType() { return eventType; }

    /** @param eventType El nuevo tipo de evento. */
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    /** @return El ID del usuario asociado. */
    public Long getUserId() { return userId; }

    /** @param userId El nuevo ID de usuario. */
    public void setUserId(Long userId) { this.userId = userId; }

    /** @return El nombre de usuario asociado. */
    public String getUsername() { return username; }

    /** @param username El nuevo nombre de usuario. */
    public void setUsername(String username) { this.username = username; }

    /** @return El ID de correlación. */
    public String getCorrelationId() { return correlationId; }

    /** @param correlationId El nuevo ID de correlación. */
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    /** @return La descripción del evento. */
    public String getDescription() { return description; }

    /** @param description La nueva descripción. */
    public void setDescription(String description) { this.description = description; }

    /** @return El payload JSON del evento. */
    public String getPayload() { return payload; }

    /** @param payload El nuevo payload. */
    public void setPayload(String payload) { this.payload = payload; }

    /** @return La fecha y hora del evento. */
    public LocalDateTime getOccurredAt() { return occurredAt; }

    /** @param occurredAt La nueva fecha. */
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    /** @return La IP de origen. */
    public String getSourceIp() { return sourceIp; }

    /** @param sourceIp La nueva IP. */
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    /** @return El resultado del evento. */
    public EventResult getResult() { return result; }

    /** @param result El nuevo resultado. */
    public void setResult(EventResult result) { this.result = result; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuditEventDTO other = (AuditEventDTO) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "AuditEventDTO [id=" + id + ", eventType=" + eventType
                + ", username=" + username
                + ", correlationId=" + correlationId
                + ", result=" + result
                + ", occurredAt=" + occurredAt + "]";
    }
}