/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.NotificationDTO;
import co.edu.unbosque.mundial2026.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para el módulo de notificaciones en la plataforma
 * Mundial 2026 Hub.
 * Expone endpoints para que el backoffice (operadores ADMIN) envíe
 * notificaciones personalizadas a un usuario, masivas a seguidores de un
 * equipo o masivas a usuarios interesados en una ciudad sede. El registro
 * de cada envío queda en auditoría automáticamente vía {@link NotificationService}.
 */
@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Tag(name = "Notificaciones", description = "Envío de notificaciones push y email a usuarios")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    /**
     * Servicio de envío de notificaciones.
     */
    @Autowired
    private NotificationService notificationService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public NotificationController() {
    }

    /**
     * Envía una notificación a un usuario específico usando sus canales activos
     * (push y/o email según sus preferencias).
     * Solo ADMIN.
     *
     * @param dto El DTO con targetUserId, title, body, channel y notificationType.
     * @return 200 OK si fue procesado.
     */
    @PostMapping("/send")
    @Operation(summary = "Enviar notificación a usuario",
               description = "Solo ADMIN. Envía notificación personalizada por los canales activos del usuario.")
    public ResponseEntity<?> sendToUser(@RequestBody NotificationDTO dto) {
        notificationService.sendToUser(dto);
        return ResponseEntity.ok(
                Map.of("message", "Notificación enviada", "success", true));
    }

    /**
     * Envía una notificación masiva a todos los seguidores de un equipo específico.
     * Se usa para alertas de inicio de partido, goles y resultados.
     * Solo ADMIN.
     *
     * @param teamIsoCode      El código ISO del equipo (ej. "COL", "BRA").
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación para enrutamiento en el cliente.
     * @param resourceId       ID del partido u otro recurso relacionado.
     * @return 200 OK con el número de usuarios notificados.
     */
    @PostMapping("/broadcast/team/{teamIsoCode}")
    @Operation(summary = "Notificación masiva por equipo",
               description = "Solo ADMIN. Notifica a todos los usuarios que siguen al equipo indicado.")
    public ResponseEntity<?> broadcastToTeam(@PathVariable String teamIsoCode,
                                              @RequestParam String title,
                                              @RequestParam String body,
                                              @RequestParam String notificationType,
                                              @RequestParam(required = false) Long resourceId) {
        int notified = notificationService.sendToTeamFollowers(
                teamIsoCode, title, body, notificationType, resourceId);
        return ResponseEntity.ok(Map.of(
                "message", "Notificación masiva enviada",
                "notified", notified,
                "success", true));
    }

    /**
     * Envía una notificación masiva a todos los usuarios interesados en una ciudad.
     * Se usa para avisos logísticos, cambios de sede y eventos en el estadio.
     * Solo ADMIN.
     *
     * @param city             La ciudad del estadio (ej. "East Rutherford", "Miami").
     * @param title            Título de la notificación.
     * @param body             Cuerpo del mensaje.
     * @param notificationType Tipo de notificación para enrutamiento en el cliente.
     * @param resourceId       ID del recurso relacionado.
     * @return 200 OK con el número de usuarios notificados.
     */
    @PostMapping("/broadcast/city/{city}")
    @Operation(summary = "Notificación masiva por ciudad",
               description = "Solo ADMIN. Notifica a todos los usuarios con preferencia en la ciudad indicada.")
    public ResponseEntity<?> broadcastToCity(@PathVariable String city,
                                              @RequestParam String title,
                                              @RequestParam String body,
                                              @RequestParam String notificationType,
                                              @RequestParam(required = false) Long resourceId) {
        int notified = notificationService.sendToCityFollowers(
                city, title, body, notificationType, resourceId);
        return ResponseEntity.ok(Map.of(
                "message", "Notificación masiva por ciudad enviada",
                "notified", notified,
                "success", true));
    }
}