/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.CreateGroupRequest;
import co.edu.unbosque.mundial2026.dto.PollGroupDTO;
import co.edu.unbosque.mundial2026.dto.PredictionDTO;
import co.edu.unbosque.mundial2026.dto.RankingDTO;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para el módulo de pollas futboleras y pronósticos.
 *
 * <h3>Cambios respecto a la versión anterior</h3>
 * <ul>
 *   <li><b>JWT Principal</b>: ya no se reciben {@code userId}/{@code ownerId}
 *       como parámetros. Cada endpoint extrae el ID del usuario autenticado
 *       desde el {@link Authentication} inyectado por Spring Security
 *       (que carga el principal en el {@code JwtAuthenticationFilter}).</li>
 *   <li><b>POST /polls/groups</b>: ahora recibe un body JSON con {@code name}
 *       y {@code description} (en lugar de query params).</li>
 *   <li><b>DELETE /polls/groups/&#123;id&#125;</b>: nuevo endpoint para eliminar
 *       grupos definitivamente (hard delete). Solo el creador puede hacerlo.</li>
 *   <li><b>GET /polls/groups/mine</b>: reemplaza a {@code /groups/user/&#123;userId&#125;}.
 *       Devuelve los grupos del usuario autenticado, con inviteCode visible solo
 *       en los grupos donde es el creador.</li>
 *   <li><b>PUT /polls/predictions/&#123;id&#125;</b>: siempre devuelve 403.
 *       Las predicciones no son modificables (regla del proyecto).</li>
 * </ul>
 */
@RestController
@RequestMapping("/polls")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000", "http://localhost:5173" })
@Transactional
@Tag(name = "Pollas", description = "Grupos de predicciones, pronósticos y ranking")
@SecurityRequirement(name = "bearerAuth")
public class PollController {

    @Autowired
    private PollService pollService;

    public PollController() {
    }

    // =========================================================================
    // Gestión de grupos
    // =========================================================================

    /**
     * Crea un nuevo grupo de polla. El creador es el usuario autenticado.
     *
     * @param body Body JSON con {@code name} y {@code description}.
     * @param auth Principal del JWT (inyectado por Spring Security).
     * @return 201 Created con el {@link PollGroupDTO} (incluye inviteCode);
     *         400 si el nombre está vacío; 404 si el usuario del JWT no existe.
     */
    @PostMapping("/groups")
    @Operation(summary = "Crear grupo de polla",
               description = "Crea un grupo y genera un código de invitación único. " +
                             "El creador es el usuario autenticado (no se envía en el body).")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest body,
                                          Authentication auth) {
        Long ownerId = extractUserId(auth);
        if (ownerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado", "success", false));
        }
        if (body == null || body.getName() == null || body.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El nombre del grupo es obligatorio",
                            "success", false));
        }

        PollGroupDTO group = pollService.createGroup(
                body.getName().trim(),
                body.getDescription(),
                ownerId);
        if (group != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(group);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuario creador no encontrado",
                        "success", false));
    }

    /**
     * Une al usuario autenticado a un grupo de polla mediante el código de invitación.
     *
     * @param inviteCode El código de invitación del grupo.
     * @param auth       Principal del JWT.
     * @return 202 si se unió; 409 si ya era miembro; 404 si el código no existe;
     *         400 si el usuario es el creador del grupo (mensaje específico).
     */
    @PostMapping("/groups/join")
    @Operation(summary = "Unirse a un grupo",
               description = "Une al usuario autenticado al grupo cuyo código de invitación se proporciona.")
    public ResponseEntity<?> joinGroup(@RequestParam String inviteCode,
                                        Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado", "success", false));
        }
        if (inviteCode == null || inviteCode.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El código de invitación es obligatorio",
                            "success", false));
        }

        int status = pollService.joinGroup(userId, inviteCode.trim());

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Te has unido al grupo exitosamente",
                            "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ya eres miembro de este grupo",
                            "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Código de invitación inválido",
                            "success", false));
            case 5 -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Ya eres el creador de este grupo",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al unirse al grupo",
                            "success", false));
        };
    }

    /**
     * Lista todos los grupos activos del usuario autenticado.
     * El {@code inviteCode} solo se incluye en los grupos donde es el creador.
     *
     * @param auth Principal del JWT.
     * @return 202 con la lista; 204 si no tiene grupos.
     */
    @GetMapping("/groups/mine")
    @Operation(summary = "Mis grupos de polla",
               description = "Lista los grupos activos del usuario autenticado.")
    public ResponseEntity<List<PollGroupDTO>> getMyGroups(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<PollGroupDTO> groups = pollService.getGroupsByUser(userId);
        if (groups.isEmpty()) {
            return new ResponseEntity<>(groups, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(groups, HttpStatus.ACCEPTED);
    }

    /**
     * Detalle de un grupo por ID. Solo el creador ve el {@code inviteCode}.
     *
     * @param groupId El ID del grupo.
     * @param auth    Principal del JWT (para decidir si mostrar el inviteCode).
     * @return 202 con el grupo; 404 si no existe.
     */
    @GetMapping("/groups/{groupId}")
    @Operation(summary = "Detalle de grupo",
               description = "Detalle del grupo. El inviteCode solo se muestra al creador.")
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId, Authentication auth) {
        PollGroupDTO group = pollService.getGroupById(groupId);
        if (group == null) {
            return new ResponseEntity<>(new PollGroupDTO(), HttpStatus.NOT_FOUND);
        }
        Long userId = extractUserId(auth);
        // Si el solicitante no es el creador, ocultamos el código.
        if (userId == null || group.getOwnerId() == null
                || !group.getOwnerId().equals(userId)) {
            group.setInviteCode(null);
        }
        return new ResponseEntity<>(group, HttpStatus.ACCEPTED);
    }

    /**
     * Desactiva (soft delete) un grupo de polla. Solo el creador.
     */
    @PutMapping("/groups/{groupId}/deactivate")
    @Operation(summary = "Desactivar grupo (soft)",
               description = "Solo el creador. Conserva el historial de pronósticos.")
    public ResponseEntity<?> deactivateGroup(@PathVariable Long groupId,
                                              Authentication auth) {
        Long ownerId = extractUserId(auth);
        if (ownerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado", "success", false));
        }
        int status = pollService.deactivateGroup(groupId, ownerId);
        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Grupo desactivado", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Grupo no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo el creador puede desactivar el grupo",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al desactivar el grupo",
                            "success", false));
        };
    }

    /**
     * Elimina completamente un grupo y todos sus pronósticos (hard delete).
     * Solo el creador puede hacerlo.
     *
     * @param groupId El ID del grupo a eliminar.
     * @param auth    Principal del JWT.
     * @return 202 si fue eliminado; 404 si no existe; 403 si no es el creador.
     */
    @DeleteMapping("/groups/{groupId}")
    @Operation(summary = "Eliminar grupo (hard delete)",
               description = "Borra el grupo y todos sus pronósticos. Solo el creador.")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId,
                                          Authentication auth) {
        Long ownerId = extractUserId(auth);
        if (ownerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado", "success", false));
        }
        int status = pollService.deleteGroup(groupId, ownerId);
        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Grupo eliminado exitosamente",
                            "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Grupo no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo el creador puede eliminar el grupo",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al eliminar el grupo",
                            "success", false));
        };
    }

    // =========================================================================
    // Gestión de pronósticos
    // =========================================================================

    /**
     * Registra el pronóstico del usuario autenticado para un partido.
     *
     * @param data El DTO con matchId, pollGroupId y marcadores predichos.
     *             El {@code userId} del DTO se ignora.
     * @param auth Principal del JWT.
     */
    @PostMapping("/predictions")
    @Operation(summary = "Realizar pronóstico",
               description = "Registra el pronóstico del usuario autenticado. " +
                             "Una vez creado, NO es modificable.")
    public ResponseEntity<?> submitPrediction(@RequestBody PredictionDTO data,
                                               Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado", "success", false));
        }
        if (data == null
                || data.getMatchId() == null
                || data.getPollGroupId() == null
                || data.getPredictedHomeScore() == null
                || data.getPredictedAwayScore() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Faltan datos: matchId, pollGroupId, predictedHomeScore o predictedAwayScore",
                            "success", false));
        }
        if (data.getPredictedHomeScore() < 0 || data.getPredictedAwayScore() < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Los marcadores no pueden ser negativos",
                            "success", false));
        }

        int status = pollService.submitPrediction(data, userId);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Pronóstico registrado exitosamente",
                            "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "Ya tienes un pronóstico para este partido en este grupo. " +
                            "Las predicciones no son modificables.",
                            "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message",
                            "Partido, grupo o usuario no encontrado",
                            "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "El partido ya inició o no eres miembro del grupo",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al registrar el pronóstico",
                            "success", false));
        };
    }

    /**
     * Endpoint de edición de pronóstico — SIEMPRE devuelve 403.
     * Se conserva la ruta para responder con un mensaje claro a clientes que
     * pudieran invocarla por accidente.
     */
    @PutMapping("/predictions/{predictionId}")
    @Operation(summary = "Editar pronóstico (DESHABILITADO)",
               description = "Endpoint deshabilitado: los pronósticos no son modificables.")
    public ResponseEntity<?> editPrediction(@PathVariable Long predictionId,
                                             @RequestParam(required = false) Integer homeScore,
                                             @RequestParam(required = false) Integer awayScore) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message",
                        "Los pronósticos no se pueden modificar una vez registrados.",
                        "success", false));
    }

    /**
     * Lista los pronósticos del usuario autenticado en todos sus grupos.
     */
    @GetMapping("/predictions/mine")
    @Operation(summary = "Mis pronósticos",
               description = "Lista todos los pronósticos del usuario autenticado.")
    public ResponseEntity<List<PredictionDTO>> getMyPredictions(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<PredictionDTO> predictions = pollService.getPredictionsByUser(userId);
        if (predictions.isEmpty()) {
            return new ResponseEntity<>(predictions, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(predictions, HttpStatus.ACCEPTED);
    }

    /**
     * Lista los pronósticos del usuario autenticado dentro de un grupo.
     */
    @GetMapping("/predictions/mine/group/{pollGroupId}")
    @Operation(summary = "Mis pronósticos en un grupo",
               description = "Lista los pronósticos del usuario autenticado dentro del grupo.")
    public ResponseEntity<List<PredictionDTO>> getMyPredictionsInGroup(
            @PathVariable Long pollGroupId, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<PredictionDTO> predictions =
                pollService.getPredictionsByUserAndGroup(userId, pollGroupId);
        if (predictions.isEmpty()) {
            return new ResponseEntity<>(predictions, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(predictions, HttpStatus.ACCEPTED);
    }

    // =========================================================================
    // Ranking
    // =========================================================================

    @GetMapping("/groups/{groupId}/ranking")
    @Operation(summary = "Ranking del grupo",
               description = "Ranking ordenado por puntos totales descendente.")
    public ResponseEntity<List<RankingDTO>> getRanking(@PathVariable Long groupId) {
        List<RankingDTO> ranking = pollService.getRankingByGroup(groupId);
        if (ranking.isEmpty()) {
            return new ResponseEntity<>(ranking, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(ranking, HttpStatus.ACCEPTED);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Extrae el ID del usuario autenticado desde el {@link Authentication}.
     * El JwtAuthenticationFilter pone como principal una instancia de {@link User}
     * (que implementa UserDetails), por lo que un cast directo basta para
     * obtener el ID.
     *
     * @param auth El objeto Authentication inyectado por Spring Security.
     * @return El ID del usuario, o {@code null} si no hay sesión válida.
     */
    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        return null;
    }
}