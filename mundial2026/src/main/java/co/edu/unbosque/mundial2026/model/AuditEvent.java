/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
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
 * Entidad JPA que representa un evento de auditoría en la plataforma
 * Mundial 2026 Hub.
 * <p>
 * El sistema registra eventos relevantes de forma auditable: cambios en
 * el calendario, notificaciones emitidas, compras simuladas, transferencias,
 * reembolsos, decisiones antifraude, aperturas de paquetes, intercambios
 * de láminas y acciones administrativas.
 * </p>
 * <p>
 * Esta entidad es el hilo conductor de trazabilidad y transparencia del proyecto.
 * Los registros son inmutables una vez creados; no deben modificarse ni eliminarse.
 * El campo {@code correlationId} permite agrupar eventos relacionados a una
 * misma operación (por ejemplo, todos los eventos de una reserva de entrada).
 * </p>
 * <p>
 * Los registros de auditoría se indexan en Splunk o ElasticSearch para
 * facilitar búsquedas por operador de soporte y auditoría de compliance.
 * </p>
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    /**
     * Identificador único del evento de auditoría generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo del evento, definido mediante el enum {@link EventType}.
     * Se usa para filtrar y categorizar eventos en el panel de soporte.
     */
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    /**
     * Usuario que generó o sobre el que aplica el evento.
     * Puede ser nulo para eventos de sistema sin usuario asociado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Identificador de correlación que agrupa eventos relacionados a una
     * misma operación. Por ejemplo, todos los eventos de una reserva
     * comparten el mismo {@code correlationId} que la entrada ({@link Ticket}).
     */
    private String correlationId;

    /**
     * Descripción detallada del evento en texto plano.
     * Debe ser suficientemente descriptiva para que soporte pueda
     * entender qué ocurrió sin necesidad de contexto adicional.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Datos adicionales del evento en formato JSON.
     * Almacena el payload completo para casos que requieran detalle:
     * el marcador al momento de una notificación, el estado anterior
     * de una entrada, los metadatos de un intercambio, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * Fecha y hora exacta en que ocurrió el evento (UTC).
     */
    private LocalDateTime occurredAt;

    /**
     * Dirección IP desde la que se originó la acción, si aplica.
     * Es nulo para eventos generados internamente por el sistema.
     */
    private String sourceIp;

    /**
     * Resultado del evento, definido mediante el enum {@link EventResult}.
     */
    @Enumerated(EnumType.STRING)
    private EventResult result;

    /**
     * Constructor por defecto requerido por JPA.
     * Registra la fecha y hora del evento automáticamente.
     */
    public AuditEvent() {
        this.occurredAt = LocalDateTime.now();
        this.result = EventResult.SUCCESS;
    }

    /**
     * Constructor con los datos principales del evento de auditoría.
     *
     * @param eventType     Tipo del evento.
     * @param user          Usuario asociado al evento.
     * @param correlationId ID de correlación de la operación.
     * @param description   Descripción del evento.
     * @param result        Resultado del evento.
     */
    public AuditEvent(EventType eventType, User user, String correlationId,
                      String description, EventResult result) {
        this();
        this.eventType = eventType;
        this.user = user;
        this.correlationId = correlationId;
        this.description = description;
        this.result = result;
    }

    /**
     * Enumeración que define los tipos de eventos auditables en la plataforma.
     */
    public enum EventType {
        // --- Autenticación ---
        /** Inicio de sesión exitoso. */
        USER_LOGIN,
        /** Intento de inicio de sesión fallido. */
        USER_LOGIN_FAILED,
        /** Registro de nuevo usuario. */
        USER_REGISTERED,
        /** Cierre de sesión. */
        USER_LOGOUT,

        // --- Gestión de usuarios (admin) ---
        /** Cuenta bloqueada por administrador. */
        USER_BLOCKED,
        /** Cuenta desbloqueada por administrador. */
        USER_UNBLOCKED,
        /** Cuenta eliminada por administrador. */
        USER_DELETED,
        /** Rol de usuario actualizado. */
        USER_ROLE_UPDATED,

        // --- Partidos ---
        /** Datos de un partido actualizados desde la API externa. */
        MATCH_DATA_UPDATED,
        /** Estado de un partido cambiado (SCHEDULED → LIVE → FINISHED). */
        MATCH_STATUS_CHANGED,
        /** Discrepancia detectada entre fuentes de datos. */
        MATCH_DATA_CONFLICT,

        // --- Pollas y pronósticos ---
        /** Grupo de polla creado. */
        POLL_GROUP_CREATED,
        /** Usuario unido a un grupo de polla. */
        POLL_GROUP_JOINED,
        /** Pronóstico registrado. */
        PREDICTION_SUBMITTED,
        /** Pronóstico modificado. */
        PREDICTION_UPDATED,
        /** Pronóstico bloqueado al iniciar el partido. */
        PREDICTION_LOCKED,
        /** Puntaje de pronóstico calculado al finalizar el partido. */
        PREDICTION_EVALUATED,

        // --- Álbum y láminas ---
        /** Paquete de láminas otorgado al usuario. */
        PACKAGE_GRANTED,
        /** Paquete de láminas abierto por el usuario. */
        PACKAGE_OPENED,
        /** Solicitud de intercambio de láminas creada. */
        EXCHANGE_REQUESTED,
        /** Intercambio de láminas completado. */
        EXCHANGE_COMPLETED,
        /** Intercambio de láminas rechazado o cancelado. */
        EXCHANGE_CANCELLED,

        // --- Entradas (tickets) ---
        /** Entrada reservada por un usuario. */
        TICKET_RESERVED,
        /** Pago de entrada confirmado. */
        TICKET_PAID,
        /** Reserva de entrada expirada por TTL. */
        TICKET_EXPIRED,
        /** Entrada transferida a otro usuario. */
        TICKET_TRANSFERRED,
        /** Reembolso de entrada procesado. */
        TICKET_REFUNDED,

        // --- Notificaciones ---
        /** Notificación enviada a uno o más usuarios. */
        NOTIFICATION_SENT,
        /** Error al enviar una notificación. */
        NOTIFICATION_FAILED,

        // --- Seguridad y antifraude ---
        /** Patrón anómalo detectado; cuenta limitada temporalmente. */
        FRAUD_PATTERN_DETECTED,
        /** Caso de investigación abierto por compliance. */
        INVESTIGATION_OPENED
    }

    /**
     * Enumeración que define el resultado de un evento de auditoría.
     */
    public enum EventResult {
        /** La operación se completó exitosamente. */
        SUCCESS,
        /** La operación falló por un error del sistema o datos inválidos. */
        FAILURE,
        /** La operación fue bloqueada por reglas de negocio o antifraude. */
        BLOCKED
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del evento de auditoría. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del evento. */
    public void setId(Long id) { this.id = id; }

    /** @return El tipo del evento. */
    public EventType getEventType() { return eventType; }

    /** @param eventType El nuevo tipo de evento. */
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    /** @return El usuario asociado al evento. */
    public User getUser() { return user; }

    /** @param user El nuevo usuario asociado. */
    public void setUser(User user) { this.user = user; }

    /** @return El ID de correlación del evento. */
    public String getCorrelationId() { return correlationId; }

    /** @param correlationId El nuevo ID de correlación. */
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    /** @return La descripción detallada del evento. */
    public String getDescription() { return description; }

    /** @param description La nueva descripción del evento. */
    public void setDescription(String description) { this.description = description; }

    /** @return El payload JSON con datos adicionales del evento. */
    public String getPayload() { return payload; }

    /** @param payload El nuevo payload JSON. */
    public void setPayload(String payload) { this.payload = payload; }

    /** @return La fecha y hora en que ocurrió el evento. */
    public LocalDateTime getOccurredAt() { return occurredAt; }

    /** @param occurredAt La nueva fecha y hora del evento. */
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    /** @return La dirección IP de origen de la acción. */
    public String getSourceIp() { return sourceIp; }

    /** @param sourceIp La nueva IP de origen. */
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    /** @return El resultado del evento. */
    public EventResult getResult() { return result; }

    /** @param result El nuevo resultado del evento. */
    public void setResult(EventResult result) { this.result = result; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuditEvent other = (AuditEvent) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "AuditEvent [id=" + id + ", eventType=" + eventType
                + ", user=" + (user != null ? user.getUsername() : "system")
                + ", correlationId=" + correlationId
                + ", result=" + result
                + ", occurredAt=" + occurredAt + "]";
    }
}