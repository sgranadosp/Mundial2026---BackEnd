/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.AuditEvent;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventResult;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventType;

/**
 * Interfaz de repositorio para la entidad {@link AuditEvent}.
 * Extiende {@link JpaRepository} para proveer operaciones de persistencia sobre
 * la tabla {@code audit_events}. Los registros de auditoría son inmutables:
 * no se actualizan ni eliminan una vez creados. Los métodos personalizados
 * permiten que soporte y compliance consulten la línea de tiempo de eventos
 * por usuario, por operación (correlationId) o por tipo de evento.
 * Las consultas paginadas son obligatorias para este repositorio dado el
 * volumen esperado de registros durante el torneo (millones de eventos).
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /**
     * Obtiene todos los eventos de auditoría de un usuario específico, paginados
     * y ordenados por fecha descendente (más reciente primero).
     * Se usa en el panel de soporte para ver la línea de tiempo de actividad
     * de un usuario (HU29 — Ver registro de actividades).
     *
     * @param userId   El ID del usuario.
     * @param pageable Parámetros de paginación y ordenamiento.
     * @return Página de eventos del usuario.
     */
    Page<AuditEvent> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);

    /**
     * Obtiene todos los eventos agrupados por un ID de correlación.
     * Se usa para reconstruir la línea de tiempo completa de una operación
     * (todos los eventos de una reserva, un intercambio o una transferencia),
     * permitiendo a soporte diagnosticar qué ocurrió y en qué orden.
     *
     * @param correlationId El ID de correlación de la operación.
     * @return Lista de eventos ordenados por fecha ascendente para reconstruir
     *         la secuencia cronológica de la operación.
     */
    List<AuditEvent> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    /**
     * Obtiene todos los eventos de un tipo específico dentro de un rango de fechas,
     * paginados. Se usa en compliance para detectar patrones de eventos de
     * seguridad (ej. todos los {@code USER_LOGIN_FAILED} del último día).
     *
     * @param eventType El tipo de evento a filtrar.
     * @param start     Fecha y hora de inicio del rango (inclusive).
     * @param end       Fecha y hora de fin del rango (inclusive).
     * @param pageable  Parámetros de paginación.
     * @return Página de eventos del tipo indicado dentro del rango.
     */
    Page<AuditEvent> findByEventTypeAndOccurredAtBetween(EventType eventType,
                                                          LocalDateTime start,
                                                          LocalDateTime end,
                                                          Pageable pageable);

    /**
     * Obtiene todos los eventos con un resultado específico (SUCCESS, FAILURE
     * o BLOCKED), paginados. Se usa en compliance para auditar operaciones
     * fallidas o bloqueadas por el sistema antifraude.
     *
     * @param result   El resultado del evento.
     * @param pageable Parámetros de paginación.
     * @return Página de eventos con el resultado indicado.
     */
    Page<AuditEvent> findByResult(EventResult result, Pageable pageable);

    /**
     * Obtiene los eventos de un usuario filtrados por tipo, paginados.
     * Se usa en soporte para ver solo los eventos de autenticación, solo los
     * de entradas o solo los de pollas de un usuario específico.
     *
     * @param userId    El ID del usuario.
     * @param eventType El tipo de evento.
     * @param pageable  Parámetros de paginación.
     * @return Página de eventos del usuario con el tipo indicado.
     */
    Page<AuditEvent> findByUserIdAndEventType(Long userId, EventType eventType, Pageable pageable);

    /**
     * Cuenta el número de eventos de un tipo específico para un usuario en un
     * rango de tiempo. Se usa en el módulo antifraude para detectar patrones
     * anómalos (ej. muchos {@code TICKET_RESERVED} en poco tiempo).
     *
     * @param userId    El ID del usuario.
     * @param eventType El tipo de evento.
     * @param start     Inicio del rango de tiempo.
     * @param end       Fin del rango de tiempo.
     * @return Número de eventos del tipo indicado en el rango dado.
     */
    long countByUserIdAndEventTypeAndOccurredAtBetween(Long userId, EventType eventType,
                                                        LocalDateTime start, LocalDateTime end);

    /**
     * Cuenta el número total de eventos de un tipo específico en un rango
     * de tiempo, SIN filtrar por usuario. Se usa en el dashboard
     * administrativo para métricas globales: por ejemplo, "+N usuarios
     * registrados esta semana" se calcula contando los eventos
     * {@code USER_REGISTERED} de los últimos 7 días.
     *
     * @param eventType El tipo de evento.
     * @param start     Inicio del rango de tiempo (inclusive).
     * @param end       Fin del rango de tiempo (inclusive).
     * @return Número de eventos del tipo indicado en el rango dado.
     */
    long countByEventTypeAndOccurredAtBetween(EventType eventType,
                                               LocalDateTime start,
                                               LocalDateTime end);
}