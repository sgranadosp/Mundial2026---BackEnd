/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.PollGroupDTO;
import co.edu.unbosque.mundial2026.dto.PredictionDTO;
import co.edu.unbosque.mundial2026.dto.RankingDTO;
import co.edu.unbosque.mundial2026.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para el módulo de pollas futboleras y pronósticos
 * en la plataforma Mundial 2026 Hub.
 * Gestiona el ciclo completo: creación de grupos (HU11), incorporación por
 * código de invitación, registro de pronósticos (HU11), edición antes del
 * partido (HU12), consulta del historial (HU13, HU14, HU15) y visualización
 * del ranking (HU21, HU22, HU23, HU24, HU25).
 */
@RestController
@RequestMapping("/polls")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Transactional
@Tag(name = "Pollas", description = "Grupos de predicciones, pronósticos y ranking")
@SecurityRequirement(name = "bearerAuth")
public class PollController {

    /**
     * Servicio de lógica de negocio de pollas y pronósticos.
     */
    @Autowired
    private PollService pollService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public PollController() {
    }

    // =========================================================================
    // Gestión de grupos
    // =========================================================================

    /**
     * Crea un nuevo grupo de polla para el usuario autenticado.
     * Genera automáticamente el código de invitación único.
     *
     * @param name    El nombre del grupo.
     * @param ownerId El ID del usuario creador.
     * @return 201 Created con el {@link PollGroupDTO} (incluye inviteCode);
     *         404 Not Found si el usuario no existe.
     */
    @PostMapping("/groups")
    @Operation(summary = "Crear grupo de polla",
               description = "Crea un grupo y genera un código de invitación único para compartir.")
    public ResponseEntity<?> createGroup(@RequestParam String name, @RequestParam Long ownerId) {
        PollGroupDTO group = pollService.createGroup(name, ownerId);
        if (group != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(group);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuario creador no encontrado", "success", false));
    }

    /**
     * Une a un usuario a un grupo de polla existente usando el código de invitación.
     *
     * @param userId     El ID del usuario que quiere unirse.
     * @param inviteCode El código de invitación del grupo.
     * @return 202 Accepted si se unió; 409 Conflict si ya era miembro;
     *         404 Not Found si el código o el usuario no existen.
     */
    @PostMapping("/groups/join")
    @Operation(summary = "Unirse a un grupo",
               description = "Permite a un usuario unirse a una polla existente con el código de invitación.")
    public ResponseEntity<?> joinGroup(@RequestParam Long userId, @RequestParam String inviteCode) {
        int status = pollService.joinGroup(userId, inviteCode);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Te has unido al grupo exitosamente", "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ya eres miembro de este grupo", "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Código de invitación inválido o usuario no encontrado",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al unirse al grupo", "success", false));
        };
    }

    /**
     * Lista todos los grupos activos en los que participa un usuario.
     *
     * @param userId El ID del usuario.
     * @return 202 Accepted con la lista de grupos; 204 si no tiene grupos.
     */
    @GetMapping("/groups/user/{userId}")
    @Operation(summary = "Grupos del usuario",
               description = "Retorna todos los grupos activos en los que el usuario participa.")
    public ResponseEntity<List<PollGroupDTO>> getGroupsByUser(@PathVariable Long userId) {
        List<PollGroupDTO> groups = pollService.getGroupsByUser(userId);
        if (groups.isEmpty()) {
            return new ResponseEntity<>(groups, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(groups, HttpStatus.ACCEPTED);
    }

    /**
     * Obtiene el detalle de un grupo por su ID.
     *
     * @param groupId El ID del grupo.
     * @return 202 Accepted con el grupo; 404 si no existe.
     */
    @GetMapping("/groups/{groupId}")
    @Operation(summary = "Detalle de grupo",
               description = "Retorna los datos del grupo incluyendo nombre del creador y número de miembros.")
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId) {
        PollGroupDTO group = pollService.getGroupById(groupId);
        if (group != null) {
            return new ResponseEntity<>(group, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(new PollGroupDTO(), HttpStatus.NOT_FOUND);
    }

    /**
     * Desactiva un grupo de polla. Solo el creador puede hacerlo.
     *
     * @param groupId El ID del grupo a desactivar.
     * @param ownerId El ID del creador que solicita la desactivación.
     * @return 202 Accepted si fue desactivado; 404 si no existe;
     *         403 Forbidden si el usuario no es el creador.
     */
    @PutMapping("/groups/{groupId}/deactivate")
    @Operation(summary = "Desactivar grupo",
               description = "Solo el creador puede desactivar su grupo. Los pronósticos previos se conservan.")
    public ResponseEntity<?> deactivateGroup(@PathVariable Long groupId, @RequestParam Long ownerId) {
        int status = pollService.deactivateGroup(groupId, ownerId);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Grupo desactivado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Grupo no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo el creador puede desactivar el grupo", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al desactivar el grupo", "success", false));
        };
    }

    // =========================================================================
    // Gestión de pronósticos
    // =========================================================================

    /**
     * Registra un pronóstico de un usuario para un partido (HU11 — Realizar predicción).
     * Solo se permite si el partido tiene estado SCHEDULED.
     *
     * @param data El DTO con userId, matchId, pollGroupId y los marcadores predichos.
     * @return 201 Created si fue registrado; 409 si ya existe un pronóstico del mismo
     *         usuario para ese partido y grupo; 404 si alguna entidad no existe;
     *         403 si el partido ya inició.
     */
    @PostMapping("/predictions")
    @Operation(summary = "Realizar pronóstico",
               description = "Registra el marcador predicho para un partido. Solo válido antes del inicio.")
    public ResponseEntity<?> submitPrediction(@RequestBody PredictionDTO data) {
        int status = pollService.submitPrediction(data);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Pronóstico registrado exitosamente", "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ya tienes un pronóstico para este partido en este grupo",
                            "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Partido, grupo o usuario no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "El partido ya inició, no se aceptan más pronósticos",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al registrar el pronóstico", "success", false));
        };
    }

    /**
     * Edita un pronóstico existente (HU12 — Editar predicción).
     * Solo se permite si el pronóstico está en estado OPEN.
     *
     * @param predictionId El ID del pronóstico a editar.
     * @param userId       El ID del usuario dueño del pronóstico.
     * @param homeScore    Nuevo marcador predicho para el equipo local.
     * @param awayScore    Nuevo marcador predicho para el equipo visitante.
     * @return 202 Accepted si fue editado; 404 si no existe; 403 si está bloqueado
     *         o no pertenece al usuario.
     */
    @PutMapping("/predictions/{predictionId}")
    @Operation(summary = "Editar pronóstico",
               description = "Modifica el marcador predicho. Solo si el partido aún no ha iniciado.")
    public ResponseEntity<?> editPrediction(@PathVariable Long predictionId,
                                             @RequestParam Long userId,
                                             @RequestParam Integer homeScore,
                                             @RequestParam Integer awayScore) {
        int status = pollService.editPrediction(predictionId, userId, homeScore, awayScore);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Pronóstico actualizado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Pronóstico no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "El pronóstico está bloqueado o no te pertenece", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al editar el pronóstico", "success", false));
        };
    }

    /**
     * Lista los pronósticos realizados por un usuario en todos sus grupos
     * (HU13 — Ver predicciones realizadas).
     *
     * @param userId El ID del usuario.
     * @return 202 Accepted con la lista; 204 si no hay pronósticos.
     */
    @GetMapping("/predictions/user/{userId}")
    @Operation(summary = "Ver pronósticos del usuario",
               description = "Retorna todos los pronósticos del usuario en todos sus grupos.")
    public ResponseEntity<List<PredictionDTO>> getPredictionsByUser(@PathVariable Long userId) {
        List<PredictionDTO> predictions = pollService.getPredictionsByUser(userId);
        if (predictions.isEmpty()) {
            return new ResponseEntity<>(predictions, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(predictions, HttpStatus.ACCEPTED);
    }

    /**
     * Lista el historial de pronósticos de un usuario en un grupo específico,
     * incluyendo los resultados reales (HU14 — Ver historial de predicciones).
     *
     * @param userId      El ID del usuario.
     * @param pollGroupId El ID del grupo.
     * @return 202 Accepted con el historial; 204 si no hay registros.
     */
    @GetMapping("/predictions/user/{userId}/group/{pollGroupId}")
    @Operation(summary = "Historial de pronósticos en un grupo",
               description = "Retorna pronósticos del usuario en un grupo con resultados reales evaluados.")
    public ResponseEntity<List<PredictionDTO>> getPredictionsByUserAndGroup(
            @PathVariable Long userId, @PathVariable Long pollGroupId) {
        List<PredictionDTO> predictions = pollService.getPredictionsByUserAndGroup(userId, pollGroupId);
        if (predictions.isEmpty()) {
            return new ResponseEntity<>(predictions, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(predictions, HttpStatus.ACCEPTED);
    }

    // =========================================================================
    // Ranking
    // =========================================================================

    /**
     * Obtiene el ranking actualizado de un grupo de polla
     * (HU21 — Ver ranking de usuarios, HU23 — Ver clasificación general,
     * HU24 — Ver actualización del ranking, HU25 — Ver número de aciertos).
     *
     * @param groupId El ID del grupo de polla.
     * @return 202 Accepted con el ranking ordenado por puntos; 204 si está vacío.
     */
    @GetMapping("/groups/{groupId}/ranking")
    @Operation(summary = "Ranking del grupo",
               description = "Retorna el ranking actualizado del grupo ordenado por puntos totales.")
    public ResponseEntity<List<RankingDTO>> getRanking(@PathVariable Long groupId) {
        List<RankingDTO> ranking = pollService.getRankingByGroup(groupId);
        if (ranking.isEmpty()) {
            return new ResponseEntity<>(ranking, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(ranking, HttpStatus.ACCEPTED);
    }
}