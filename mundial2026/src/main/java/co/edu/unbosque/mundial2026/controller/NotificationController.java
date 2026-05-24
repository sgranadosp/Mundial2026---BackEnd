/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.NotificationDTO;
import co.edu.unbosque.mundial2026.dto.NotificationInboxDTO;
import co.edu.unbosque.mundial2026.model.AuditEvent.EventResult;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.service.AuditEventService;
import co.edu.unbosque.mundial2026.service.NotificationInboxService;
import co.edu.unbosque.mundial2026.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para el módulo de notificaciones en la plataforma
 * Mundial 2026 Hub.
 * <p>
 * Expone endpoints para que el backoffice (operadores ADMIN) envíe
 * notificaciones personalizadas a un usuario, masivas a seguidores de un
 * equipo o masivas a usuarios interesados en una ciudad sede. El registro
 * de cada envío queda en auditoría automáticamente vía {@link NotificationService}
 * (un evento por usuario), y adicionalmente cada job masivo se audita
 * con un evento agregado {@code SYSTEM_JOB_EXECUTED} que indica admin
 * ejecutor, target del broadcast, cantidad notificada y resultado.
 * </p>
 */
@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Tag(name = "Notificaciones", description = "Envío de notificaciones push y email a usuarios")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuditEventService auditService;

    /**
     * Servicio que administra el inbox in-app de notificaciones del usuario.
     */
    @Autowired
    private NotificationInboxService inboxService;

    public NotificationController() {
    }

    // =========================================================================
    // Envío a un usuario
    // =========================================================================

    /**
     * Envía una notificación a un usuario específico usando los canales
     * activos del usuario (push y/o email según sus preferencias). Solo ADMIN.
     *
     * Valida que el usuario destino exista y que el título y body no estén
     * vacíos. Si el resolver-by-id falla, retorna 404. Si título o body
     * vienen vacíos, retorna 400 sin tocar nada.
     *
     * @param dto      DTO con targetUserId, title, body, channel y notificationType.
     * @param adminId  ID del administrador que dispara el envío.
     * @return 200 OK si fue procesado; 404 si no existe el usuario; 400 si
     *         título o body están vacíos.
     */
    @PostMapping("/send")
    @Operation(summary = "Enviar notificación a usuario",
               description = "Solo ADMIN. Envía notificación personalizada por los canales activos del usuario.")
    public ResponseEntity<?> sendToUser(@RequestBody NotificationDTO dto,
                                         @RequestParam Long adminId) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()
                || dto.getBody() == null || dto.getBody().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El título y el mensaje son obligatorios", "success", false));
        }
        if (dto.getTargetUserId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Falta el ID del usuario destino", "success", false));
        }
        if (userRepo.findById(dto.getTargetUserId()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario destinatario no encontrado", "success", false));
        }
        notificationService.sendToUser(dto);
        auditService.logJobExecuted(adminId, "NOTIFICATION_USER",
                "Notificación enviada a usuario " + dto.getTargetUserId()
                        + " · canal=" + (dto.getChannel() == null ? "AUTO" : dto.getChannel())
                        + " · título=" + dto.getTitle(),
                EventResult.SUCCESS);
        return ResponseEntity.ok(
                Map.of("message", "Notificación enviada", "success", true));
    }

    /**
     * Resuelve el ID de un usuario a partir de un identificador que puede
     * ser un ID numérico o un username. Se usa en la pestaña "A un usuario"
     * del backoffice para que el admin pueda escribir "42" o "juan.diaz"
     * indistintamente.
     *
     * @param identifier Cadena con el ID o el username del usuario.
     * @return 200 OK con {@code {id, username, name}} si encuentra el
     *         usuario; 404 si no existe.
     */
    @GetMapping("/resolve-user")
    @Operation(summary = "Resolver usuario por ID o username",
               description = "Solo ADMIN. Devuelve los datos básicos del usuario a partir de un ID o username.")
    public ResponseEntity<?> resolveUser(@RequestParam String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Identificador vacío", "success", false));
        }
        Optional<User> found;
        try {
            // Si parsea como número, asumimos que es un ID.
            Long id = Long.parseLong(identifier.trim());
            found = userRepo.findById(id);
        } catch (NumberFormatException ex) {
            // No es un número: lo tratamos como username.
            found = userRepo.findByUsername(identifier.trim());
        }
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        User u = found.get();
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "username", u.getUsername() == null ? "" : u.getUsername(),
                "name", u.getName() == null ? "" : u.getName(),
                "success", true));
    }

    // =========================================================================
    // Broadcast por equipo
    // =========================================================================

    /**
     * Cuenta cuántos usuarios tienen al equipo indicado como favorito.
     * Se usa para mostrar el alcance estimado antes de disparar el broadcast.
     *
     * @param teamIsoCode Código ISO del equipo.
     * @return 200 OK con {@code {teamIsoCode, reach}}.
     */
    @GetMapping("/reach/team/{teamIsoCode}")
    @Operation(summary = "Alcance estimado por equipo",
               description = "Solo ADMIN. Cuenta cuántos usuarios tienen al equipo como favorito.")
    public ResponseEntity<?> reachByTeam(@PathVariable String teamIsoCode) {
        long reach = userRepo.countByFavoriteTeamCode(teamIsoCode.toUpperCase());
        return ResponseEntity.ok(Map.of(
                "teamIsoCode", teamIsoCode.toUpperCase(),
                "reach", reach));
    }

    /**
     * Envía una notificación masiva a todos los usuarios que tienen al
     * equipo como favorito. Se usa para alertas de inicio de partido,
     * goles y resultados. Solo ADMIN.
     *
     * Registra un evento {@code SYSTEM_JOB_EXECUTED} con el admin ejecutor,
     * código del equipo, cantidad notificada y resultado (SUCCESS si al
     * menos un usuario recibió la notificación; FAILURE si nadie).
     *
     * @param teamIsoCode      Código ISO del equipo (ej. "COL", "BRA").
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación para enrutamiento en el cliente.
     * @param resourceId       ID del partido u otro recurso (opcional).
     * @param adminId          ID del administrador que dispara el envío.
     * @return 200 OK con la cantidad notificada.
     */
    @PostMapping("/broadcast/team/{teamIsoCode}")
    @Operation(summary = "Notificación masiva por equipo",
               description = "Solo ADMIN. Notifica a todos los usuarios que siguen al equipo indicado.")
    public ResponseEntity<?> broadcastToTeam(@PathVariable String teamIsoCode,
                                              @RequestParam String title,
                                              @RequestParam String body,
                                              @RequestParam String notificationType,
                                              @RequestParam(required = false) Long resourceId,
                                              @RequestParam Long adminId) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El título y el mensaje son obligatorios", "success", false));
        }
        int notified;
        try {
            notified = notificationService.sendToTeamFollowers(
                    teamIsoCode, title, body, notificationType, resourceId);
        } catch (RuntimeException ex) {
            auditService.logJobExecuted(adminId, "BROADCAST_TEAM",
                    "Error en broadcast equipo " + teamIsoCode + ": " + ex.getMessage(),
                    EventResult.FAILURE);
            throw ex;
        }
        auditService.logJobExecuted(adminId, "BROADCAST_TEAM",
                "Broadcast equipo " + teamIsoCode.toUpperCase()
                        + " · " + notified + " usuarios notificados · título=" + title,
                notified > 0 ? EventResult.SUCCESS : EventResult.FAILURE);
        return ResponseEntity.ok(Map.of(
                "message", "Notificación masiva enviada",
                "notified", notified,
                "success", true));
    }

    // =========================================================================
    // Broadcast por ciudad
    // =========================================================================

    /**
     * Cuenta cuántos usuarios tienen a la ciudad indicada como ciudad
     * preferida. Alcance estimado para broadcast por ciudad.
     *
     * @param city Ciudad sede.
     * @return 200 OK con {@code {city, reach}}.
     */
    @GetMapping("/reach/city/{city}")
    @Operation(summary = "Alcance estimado por ciudad",
               description = "Solo ADMIN. Cuenta cuántos usuarios tienen a la ciudad como preferida.")
    public ResponseEntity<?> reachByCity(@PathVariable String city) {
        long reach = userRepo.countByFavoriteCity(city);
        return ResponseEntity.ok(Map.of(
                "city", city,
                "reach", reach));
    }

    /**
     * Envía una notificación masiva a todos los usuarios cuya ciudad
     * preferida coincide con la indicada. Solo ADMIN. Audita la ejecución
     * del job con el admin ejecutor, la ciudad, la cantidad notificada y
     * el resultado.
     *
     * @param city             Ciudad sede.
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación.
     * @param resourceId       ID del recurso asociado (opcional).
     * @param adminId          ID del administrador ejecutor.
     * @return 200 OK con la cantidad notificada.
     */
    @PostMapping("/broadcast/city/{city}")
    @Operation(summary = "Notificación masiva por ciudad",
               description = "Solo ADMIN. Notifica a todos los usuarios con preferencia en la ciudad indicada.")
    public ResponseEntity<?> broadcastToCity(@PathVariable String city,
                                              @RequestParam String title,
                                              @RequestParam String body,
                                              @RequestParam String notificationType,
                                              @RequestParam(required = false) Long resourceId,
                                              @RequestParam Long adminId) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El título y el mensaje son obligatorios", "success", false));
        }
        int notified;
        try {
            notified = notificationService.sendToCityFollowers(
                    city, title, body, notificationType, resourceId);
        } catch (RuntimeException ex) {
            auditService.logJobExecuted(adminId, "BROADCAST_CITY",
                    "Error en broadcast ciudad " + city + ": " + ex.getMessage(),
                    EventResult.FAILURE);
            throw ex;
        }
        auditService.logJobExecuted(adminId, "BROADCAST_CITY",
                "Broadcast ciudad " + city
                        + " · " + notified + " usuarios notificados · título=" + title,
                notified > 0 ? EventResult.SUCCESS : EventResult.FAILURE);
        return ResponseEntity.ok(Map.of(
                "message", "Notificación masiva por ciudad enviada",
                "notified", notified,
                "success", true));
    }

    // =========================================================================
    // Inbox in-app del usuario
    // =========================================================================

    /**
     * Devuelve el inbox completo de notificaciones de un usuario, ordenado
     * de la más reciente a la más antigua. Cada item incluye id, título,
     * cuerpo, tipo, estado de lectura y timestamp.
     *
     * <p>Pensado para alimentar la pantalla "Notificaciones" del frontend
     * del usuario regular. No requiere ningún rol especial: el usuario
     * solo puede leer su propio inbox (la verificación de "propio"
     * la hace el frontend con el token, no se cruza con el path).</p>
     *
     * @param userId ID del usuario dueño del inbox.
     * @return Lista de DTOs, vacía si no hay notificaciones.
     */
    @GetMapping("/inbox/{userId}")
    @Operation(summary = "Inbox de notificaciones del usuario",
               description = "Lista todas las notificaciones recibidas por el usuario, de la más reciente a la más antigua.")
    public ResponseEntity<List<NotificationInboxDTO>> getInbox(@PathVariable Long userId) {
        List<NotificationInboxDTO> inbox = inboxService.getInbox(userId);
        return ResponseEntity.ok(inbox);
    }

    /**
     * Cuenta cuántas notificaciones no leídas tiene un usuario. Útil para
     * mostrar un badge en el sidebar o el header sin tener que descargar
     * todo el inbox.
     */
    @GetMapping("/inbox/{userId}/unread-count")
    @Operation(summary = "Cantidad de notificaciones no leídas",
               description = "Devuelve cuántas notificaciones no leídas tiene el usuario para mostrar en un badge.")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        long count = inboxService.countUnread(userId);
        return ResponseEntity.ok(Map.of("unread", count));
    }

    /**
     * Marca una notificación específica como leída. Idempotente: si ya
     * estaba leída, devuelve igualmente 200 OK pero con changed=false.
     */
    @PutMapping("/inbox/{notificationId}/read")
    @Operation(summary = "Marcar notificación como leída",
               description = "Marca una notificación específica del inbox como leída.")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long notificationId) {
        boolean changed = inboxService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("changed", changed, "success", true));
    }

    /**
     * Marca como leídas todas las notificaciones no leídas del usuario.
     * Útil para el botón "marcar todas como leídas" del frontend.
     */
    @PutMapping("/inbox/user/{userId}/mark-all-read")
    @Operation(summary = "Marcar todas como leídas",
               description = "Marca todas las notificaciones no leídas del usuario como leídas.")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable Long userId) {
        int count = inboxService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("affected", count, "success", true));
    }
}