/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.AuditEventDTO;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventType;
import co.edu.unbosque.mundial2026.service.AuditEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para el panel de auditoría y soporte de la plataforma
 * Mundial 2026 Hub.
 * Todos los endpoints de este controlador son exclusivos para el rol ADMIN.
 * Implementa la trazabilidad y transparencia requeridas por el proyecto:
 * línea de tiempo de eventos de un usuario, reconstrucción de una operación
 * por correlationId, y búsqueda de eventos por tipo y rango de fechas para
 * compliance (HU29 — Ver registro de actividades, HU30 — Proteger la información).
 */
@RestController
@RequestMapping("/admin/audit")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Tag(name = "Auditoría", description = "Solo ADMIN. Panel de trazabilidad y soporte de operaciones")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    /**
     * Servicio centralizado de auditoría.
     */
    @Autowired
    private AuditEventService auditService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public AuditController() {
    }

    /**
     * Obtiene la línea de tiempo de eventos de un usuario específico,
     * paginada y ordenada de más reciente a más antiguo
     * (HU29 — Ver registro de actividades).
     *
     * @param userId El ID del usuario a consultar.
     * @param page   Número de página (0-indexed, por defecto 0).
     * @param size   Tamaño de página (por defecto 20).
     * @return 202 Accepted con la lista paginada de eventos; 204 si está vacía.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Línea de tiempo del usuario",
               description = "Solo ADMIN. Retorna todos los eventos auditados del usuario, del más reciente al más antiguo.")
    public ResponseEntity<List<AuditEventDTO>> getEventsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AuditEventDTO> events = auditService.getEventsByUser(userId, page, size);
        if (events.isEmpty()) {
            return new ResponseEntity<>(events, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(events, HttpStatus.ACCEPTED);
    }

    /**
     * Reconstruye la secuencia completa de eventos de una operación usando
     * su correlationId. Retorna los eventos en orden cronológico ascendente
     * con el payload completo para análisis de soporte.
     *
     * @param correlationId El correlationId de la operación (ej. UUID de una entrada).
     * @return 202 Accepted con la secuencia de eventos; 204 si no hay registros.
     */
    @GetMapping("/correlation/{correlationId}")
    @Operation(summary = "Trazabilidad por correlationId",
               description = "Solo ADMIN. Reconstruye la secuencia completa de eventos de una operación.")
    public ResponseEntity<List<AuditEventDTO>> getEventsByCorrelationId(
            @PathVariable String correlationId) {
        List<AuditEventDTO> events = auditService.getEventsByCorrelationId(correlationId);
        if (events.isEmpty()) {
            return new ResponseEntity<>(events, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(events, HttpStatus.ACCEPTED);
    }

    /**
     * Obtiene eventos filtrados por tipo dentro de un rango de fechas, paginados.
     * Se usa en compliance para detectar patrones de seguridad
     * (ej. todos los USER_LOGIN_FAILED del último día).
     *
     * @param eventType El tipo de evento (ej. USER_LOGIN_FAILED, FRAUD_PATTERN_DETECTED).
     * @param start     Fecha y hora de inicio del rango en formato ISO (UTC).
     * @param end       Fecha y hora de fin del rango en formato ISO (UTC).
     * @param page      Número de página (0-indexed, por defecto 0).
     * @param size      Tamaño de página (por defecto 50).
     * @return 202 Accepted con los eventos filtrados; 204 si no hay resultados.
     */
    @GetMapping("/events")
    @Operation(summary = "Eventos por tipo y fecha",
               description = "Solo ADMIN. Filtra eventos por tipo y rango de fechas para análisis de compliance.")
    public ResponseEntity<List<AuditEventDTO>> getEventsByTypeAndDate(
            @RequestParam EventType eventType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<AuditEventDTO> events = auditService.getEventsByTypeAndDateRange(
                eventType, start, end, page, size);
        if (events.isEmpty()) {
            return new ResponseEntity<>(events, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(events, HttpStatus.ACCEPTED);
    }
}