/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.AuditEventDTO;
import co.edu.unbosque.mundial2026.model.AuditEvent;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventResult;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventType;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.AuditEventRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Servicio centralizado de auditoría para la plataforma Mundial 2026 Hub.
 * Todos los eventos relevantes del sistema se registran a través de este servicio.
 * Proporciona métodos de conveniencia altamente descriptivos para cada tipo de
 * evento, de modo que los demás servicios solo necesitan llamar, por ejemplo,
 * {@code auditService.logTicketReserved(userId, correlationId, matchId)}
 * sin conocer la estructura interna del {@link AuditEvent}.
 * Los registros son inmutables. Este servicio nunca expone métodos de
 * actualización ni eliminación de eventos.
 * Los registros se indexan en Splunk/ElasticSearch para búsquedas del operador.
 */
@Service
public class AuditEventService {

    @Autowired
    private AuditEventRepository auditRepo;

    @Autowired
    private UserRepository userRepo;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public AuditEventService() {
    }

    // =========================================================================
    // Métodos de registro por módulo
    // =========================================================================

    /**
     * Registra un inicio de sesión exitoso.
     *
     * @param userId   El ID del usuario que inició sesión.
     * @param sourceIp La IP de origen de la petición.
     */
    public void logLogin(Long userId, String sourceIp) {
        log(EventType.USER_LOGIN, userId, null,
                "Inicio de sesión exitoso", null, sourceIp, EventResult.SUCCESS);
    }

    /**
     * Registra un intento de inicio de sesión fallido.
     *
     * @param userId   El ID del usuario (si existe) o null.
     * @param sourceIp La IP de origen.
     */
    public void logLoginFailed(Long userId, String sourceIp) {
        log(EventType.USER_LOGIN_FAILED, userId, null,
                "Intento de inicio de sesión fallido", null, sourceIp, EventResult.FAILURE);
    }

    /**
     * Registra el registro de un nuevo usuario.
     *
     * @param userId El ID del nuevo usuario registrado.
     */
    public void logUserRegistered(Long userId) {
        log(EventType.USER_REGISTERED, userId, null,
                "Nuevo usuario registrado en la plataforma", null, null, EventResult.SUCCESS);
    }

    /**
     * Registra el bloqueo de una cuenta por un administrador (HU17).
     *
     * @param targetUserId El ID del usuario bloqueado.
     * @param adminId      El ID del administrador que realizó la acción.
     */
    public void logUserBlocked(Long targetUserId, Long adminId) {
        log(EventType.USER_BLOCKED, targetUserId, null,
                "Cuenta bloqueada por administrador ID " + adminId,
                null, null, EventResult.SUCCESS);
    }

    /**
     * Registra el desbloqueo de una cuenta por un administrador (HU18).
     *
     * @param targetUserId El ID del usuario desbloqueado.
     * @param adminId      El ID del administrador que realizó la acción.
     */
    public void logUserUnblocked(Long targetUserId, Long adminId) {
        log(EventType.USER_UNBLOCKED, targetUserId, null,
                "Cuenta desbloqueada por administrador ID " + adminId,
                null, null, EventResult.SUCCESS);
    }

    /**
     * Registra la eliminación de un usuario (HU19).
     *
     * @param targetUserId El ID del usuario eliminado.
     * @param adminId      El ID del administrador que realizó la acción.
     */
    public void logUserDeleted(Long targetUserId, Long adminId) {
        log(EventType.USER_DELETED, targetUserId, null,
                "Usuario eliminado por administrador ID " + adminId,
                null, null, EventResult.SUCCESS);
    }

    /**
     * Registra el cambio de rol de un usuario por un administrador (HU26).
     *
     * @param targetUserId El ID del usuario al que se le cambió el rol.
     * @param adminId      El ID del administrador que realizó la acción.
     * @param newRole      El nuevo rol asignado (ej. "USER", "ADMIN").
     */
    public void logUserRoleUpdated(Long targetUserId, Long adminId, String newRole) {
        log(EventType.USER_ROLE_UPDATED, targetUserId, null,
                "Rol cambiado a " + newRole + " por administrador ID " + adminId,
                null, null, EventResult.SUCCESS);
    }

    /**
     * Registra la reserva de una entrada.
     *
     * @param userId        El ID del usuario que reservó.
     * @param correlationId El ID de correlación de la entrada.
     * @param matchId       El ID del partido.
     */
    public void logTicketReserved(Long userId, String correlationId, Long matchId) {
        log(EventType.TICKET_RESERVED, userId, correlationId,
                "Entrada reservada para el partido ID " + matchId,
                "{\"matchId\":" + matchId + "}", null, EventResult.SUCCESS);
    }

    /**
     * Registra la confirmación del pago de una entrada.
     *
     * @param userId        El ID del usuario que pagó.
     * @param correlationId El ID de correlación de la entrada.
     */
    public void logTicketPaid(Long userId, String correlationId) {
        log(EventType.TICKET_PAID, userId, correlationId,
                "Pago de entrada confirmado", null, null, EventResult.SUCCESS);
    }

    /**
     * Registra la expiración de una reserva por TTL.
     *
     * @param userId        El ID del usuario.
     * @param correlationId El ID de correlación de la entrada expirada.
     */
    public void logTicketExpired(Long userId, String correlationId) {
        log(EventType.TICKET_EXPIRED, userId, correlationId,
                "Reserva expirada por TTL; cupo liberado",
                null, null, EventResult.SUCCESS);
    }

    /**
     * Registra la transferencia de una entrada.
     *
     * @param fromUserId    El ID del usuario que transfiere.
     * @param toUserId      El ID del usuario receptor.
     * @param correlationId El ID de correlación de la entrada.
     */
    public void logTicketTransferred(Long fromUserId, Long toUserId, String correlationId) {
        log(EventType.TICKET_TRANSFERRED, fromUserId, correlationId,
                "Entrada transferida al usuario ID " + toUserId,
                "{\"fromUserId\":" + fromUserId + ",\"toUserId\":" + toUserId + "}",
                null, EventResult.SUCCESS);
    }

    /**
     * Registra el reembolso de una entrada.
     *
     * @param userId        El ID del usuario.
     * @param correlationId El ID de correlación de la entrada.
     */
    public void logTicketRefunded(Long userId, String correlationId) {
        log(EventType.TICKET_REFUNDED, userId, correlationId,
                "Reembolso de entrada procesado", null, null, EventResult.SUCCESS);
    }

    /**
     * Registra la apertura de un paquete de láminas.
     *
     * @param userId    El ID del usuario.
     * @param packageId El ID del paquete abierto.
     */
    public void logPackageOpened(Long userId, Long packageId) {
        log(EventType.PACKAGE_OPENED, userId, null,
                "Paquete de láminas abierto ID " + packageId,
                "{\"packageId\":" + packageId + "}", null, EventResult.SUCCESS);
    }

    /**
     * Registra la completación de un intercambio de láminas.
     *
     * @param exchangeId El ID del intercambio completado.
     * @param userId1    El ID del primer usuario participante.
     * @param userId2    El ID del segundo usuario participante.
     */
    public void logExchangeCompleted(Long exchangeId, Long userId1, Long userId2) {
        log(EventType.EXCHANGE_COMPLETED, userId1, null,
                "Intercambio de láminas completado ID " + exchangeId,
                "{\"exchangeId\":" + exchangeId + ",\"userId2\":" + userId2 + "}",
                null, EventResult.SUCCESS);
    }

    /**
     * Registra la detección de un patrón anómalo de comportamiento
     * para revisión del módulo antifraude.
     *
     * @param userId      El ID del usuario con comportamiento anómalo.
     * @param description Descripción del patrón detectado.
     */
    public void logFraudPattern(Long userId, String description) {
        log(EventType.FRAUD_PATTERN_DETECTED, userId, null,
                "Patrón anómalo detectado: " + description,
                null, null, EventResult.BLOCKED);
    }

    /**
     * Registra el envío de una notificación.
     *
     * @param targetUserId El ID del usuario destinatario.
     * @param channel      Canal de envío (PUSH, EMAIL, IN_APP).
     * @param title        Título de la notificación.
     */
    public void logNotificationSent(Long targetUserId, String channel, String title) {
        log(EventType.NOTIFICATION_SENT, targetUserId, null,
                "Notificación enviada por " + channel + ": " + title,
                null, null, EventResult.SUCCESS);
    }

    /**
     * Registra un fallo en el envío de una notificación.
     *
     * @param targetUserId El ID del usuario destinatario.
     * @param reason       Motivo del fallo.
     */
    public void logNotificationFailed(Long targetUserId, String reason) {
        log(EventType.NOTIFICATION_FAILED, targetUserId, null,
                "Fallo al enviar notificación: " + reason,
                null, null, EventResult.FAILURE);
    }

    /**
     * Registra la ejecución de un job administrativo en la auditoría.
     * Pensado para el job de expiración manual de reservas y otros
     * procesos batch disparados desde el panel ADMIN. La descripción
     * debe incluir métricas significativas del job (ej. cantidad de
     * registros procesados).
     *
     * @param adminId       El ID del administrador que disparó el job.
     * @param jobName       Nombre corto del job (ej. "EXPIRE_RESERVATIONS").
     * @param description   Descripción detallada con métricas del resultado.
     * @param result        Resultado del job ({@link EventResult#SUCCESS} si
     *                      terminó correctamente, FAILURE si lanzó error).
     */
    public void logJobExecuted(Long adminId, String jobName, String description, EventResult result) {
        log(EventType.SYSTEM_JOB_EXECUTED, adminId, null,
                "[" + jobName + "] " + description,
                null, null, result);
    }

    // =========================================================================
    // Consultas para soporte y compliance (solo ADMIN)
    // =========================================================================

    /**
     * Obtiene la línea de tiempo de eventos de un usuario, paginada.
     * Solo accesible para administradores.
     *
     * @param userId   El ID del usuario a consultar.
     * @param page     Número de página (0-indexed).
     * @param size     Tamaño de la página.
     * @return Lista de {@link AuditEventDTO} del usuario ordenados por fecha desc.
     */
    public List<AuditEventDTO> getEventsByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());
        Page<AuditEvent> events = auditRepo.findByUserIdOrderByOccurredAtDesc(userId, pageable);
        List<AuditEventDTO> dtoList = new ArrayList<>();
        events.forEach(e -> dtoList.add(toDTO(e, false)));
        return dtoList;
    }

    /**
     * Obtiene todos los eventos de una operación agrupados por su
     * {@code correlationId}, en orden cronológico ascendente.
     *
     * @param correlationId El ID de correlación de la operación.
     * @return Lista de {@link AuditEventDTO} en orden cronológico,
     *         con el payload completo para análisis detallado.
     */
    public List<AuditEventDTO> getEventsByCorrelationId(String correlationId) {
        List<AuditEvent> events = auditRepo.findByCorrelationIdOrderByOccurredAtAsc(correlationId);
        List<AuditEventDTO> dtoList = new ArrayList<>();
        events.forEach(e -> dtoList.add(toDTO(e, true)));
        return dtoList;
    }

    /**
     * Obtiene eventos filtrados por tipo y rango de fechas, paginados.
     * Se usa en compliance para detectar patrones de seguridad.
     *
     * @param eventType El tipo de evento.
     * @param start     Inicio del rango de fechas.
     * @param end       Fin del rango de fechas.
     * @param page      Número de página.
     * @param size      Tamaño de la página.
     * @return Lista paginada de {@link AuditEventDTO}.
     */
    public List<AuditEventDTO> getEventsByTypeAndDateRange(EventType eventType,
                                                            LocalDateTime start,
                                                            LocalDateTime end,
                                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditEvent> events = auditRepo.findByEventTypeAndOccurredAtBetween(eventType, start, end, pageable);
        List<AuditEventDTO> dtoList = new ArrayList<>();
        events.forEach(e -> dtoList.add(toDTO(e, false)));
        return dtoList;
    }

    /**
     * Obtiene los N eventos de auditoría más recientes, sin filtrar por
     * usuario ni tipo. Se usa en el feed "Actividad reciente" del dashboard
     * administrativo. Los eventos vienen ordenados desde el más reciente.
     *
     * <p>Es {@code @Transactional(readOnly = true)} para que el proxy
     * lazy {@code event.getUser()} pueda resolverse al construir el DTO.
     * Cada evento se mapea dentro de un try/catch para que un registro
     * corrupto (ej. user_id huérfano) no tumbe la respuesta entera.</p>
     *
     * @param limit Cantidad máxima de eventos a devolver (1-500).
     * @return Lista de DTOs ordenados por fecha descendente. No incluye
     *         {@code payload} para mantener el tamaño de la respuesta bajo.
     */
    @Transactional(readOnly = true)
    public List<AuditEventDTO> getRecentEvents(int limit) {
        int safeLimit = Math.max(1, Math.min(500, limit));
        Pageable pageable = PageRequest.of(0, safeLimit,
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditEvent> page = auditRepo.findAll(pageable);
        List<AuditEventDTO> dtoList = new ArrayList<>();
        page.forEach(e -> {
            try {
                dtoList.add(toDTO(e, false));
            } catch (RuntimeException ex) {
                // Un evento huérfano (user_id apuntando a un usuario borrado)
                // no debe tirar abajo todo el feed. Lo registramos en log y
                // seguimos con el siguiente.
                System.err.println("[audit] Evento " + e.getId()
                        + " no se pudo mapear: " + ex.getMessage());
            }
        });
        return dtoList;
    }

    /**
     * Obtiene un evento de auditoría por su ID con TODO el payload incluido.
     * Se usa en el panel administrativo para abrir el detalle completo de
     * un evento al hacer click en "Detalle".
     *
     * @param id ID del evento.
     * @return Optional con el DTO del evento; vacío si no existe.
     */
    @Transactional(readOnly = true)
    public Optional<AuditEventDTO> getEventById(Long id) {
        return auditRepo.findById(id).map(e -> {
            try {
                return toDTO(e, true);
            } catch (RuntimeException ex) {
                // Aun con user huérfano, devolvemos un DTO básico sin user.
                AuditEventDTO fallback = new AuditEventDTO();
                fallback.setId(e.getId());
                fallback.setEventType(e.getEventType());
                fallback.setCorrelationId(e.getCorrelationId());
                fallback.setDescription(e.getDescription());
                fallback.setOccurredAt(e.getOccurredAt());
                fallback.setSourceIp(e.getSourceIp());
                fallback.setResult(e.getResult());
                fallback.setPayload(e.getPayload());
                return fallback;
            }
        });
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Persiste un evento de auditoría. Método central usado por todos
     * los métodos de log públicos.
     *
     * @param eventType     Tipo del evento.
     * @param userId        ID del usuario asociado (puede ser null para eventos de sistema).
     * @param correlationId ID de correlación de la operación.
     * @param description   Descripción legible del evento.
     * @param payload       Payload JSON con datos adicionales (puede ser null).
     * @param sourceIp      IP de origen (puede ser null para eventos internos).
     * @param result        Resultado del evento.
     */
    private void log(EventType eventType, Long userId, String correlationId,
                     String description, String payload, String sourceIp, EventResult result) {
        User user = null;
        if (userId != null) {
            Optional<User> found = userRepo.findById(userId);
            user = found.orElse(null);
        }

        AuditEvent event = new AuditEvent(eventType, user, correlationId, description, result);
        event.setPayload(payload);
        event.setSourceIp(sourceIp);
        auditRepo.save(event);
    }

    /**
     * Convierte una entidad {@link AuditEvent} a {@link AuditEventDTO}.
     * Tolera que el {@code user} sea {@code null} o un proxy huérfano
     * (registro de auditoría apuntando a un usuario que ya fue borrado).
     *
     * @param event          La entidad del evento.
     * @param includePayload {@code true} para incluir el payload completo.
     * @return El DTO del evento.
     */
    private AuditEventDTO toDTO(AuditEvent event, boolean includePayload) {
        AuditEventDTO dto = new AuditEventDTO();
        dto.setId(event.getId());
        dto.setEventType(event.getEventType());
        dto.setCorrelationId(event.getCorrelationId());
        dto.setDescription(event.getDescription());
        dto.setOccurredAt(event.getOccurredAt());
        dto.setSourceIp(event.getSourceIp());
        dto.setResult(event.getResult());
        // event.getUser() puede ser null (eventos de sistema) o un proxy
        // que tira EntityNotFoundException si el user fue borrado. En ambos
        // casos dejamos userId/username en null.
        try {
            User u = event.getUser();
            if (u != null) {
                dto.setUserId(u.getId());
                dto.setUsername(u.getUsername());
            }
        } catch (RuntimeException ignored) {
            // Proxy lazy huérfano — registro queda sin usuario asociado.
        }
        if (includePayload) {
            dto.setPayload(event.getPayload());
        }
        return dto;
    }
}