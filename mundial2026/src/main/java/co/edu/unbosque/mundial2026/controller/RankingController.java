/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.PredictionDTO;
import co.edu.unbosque.mundial2026.dto.RankingDTO;
import co.edu.unbosque.mundial2026.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para rankings y estadísticas personales en la plataforma
 * Mundial 2026 Hub.
 * Separa la responsabilidad del ranking en su propio controlador para mayor
 * claridad en la API. Agrupa los endpoints de HU21 (ranking de usuarios),
 * HU22 (estadísticas personales), HU23 (clasificación general),
 * HU24 (actualización del ranking) y HU25 (número de aciertos).
 * Los datos de ranking se calculan en tiempo real desde {@link PollService}.
 */
@RestController
@RequestMapping("/ranking")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Tag(name = "Ranking", description = "Clasificaciones y estadísticas de predicciones")
@SecurityRequirement(name = "bearerAuth")
public class RankingController {

    /**
     * Servicio de lógica de negocio de pollas y pronósticos.
     */
    @Autowired
    private PollService pollService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public RankingController() {
    }

    /**
     * Obtiene el ranking actualizado de un grupo de polla (HU21 — Ver ranking,
     * HU23 — Ver clasificación, HU24 — Ver actualización del ranking,
     * HU25 — Ver número de aciertos).
     * <p>
     * El ranking se calcula en tiempo real sumando los puntos de todos los
     * pronósticos evaluados del grupo. Incluye posición, puntos totales,
     * aciertos de resultado y marcadores exactos.
     *
     * @param groupId El ID del grupo de polla.
     * @return 202 Accepted con el ranking ordenado; 204 si el grupo está vacío;
     *         404 si el grupo no existe.
     */
    @GetMapping("/group/{groupId}")
    @Operation(summary = "Ranking de grupo",
               description = "Retorna el ranking del grupo ordenado por puntos. Incluye aciertos y marcadores exactos.")
    public ResponseEntity<List<RankingDTO>> getGroupRanking(@PathVariable Long groupId) {
        List<RankingDTO> ranking = pollService.getRankingByGroup(groupId);
        if (ranking.isEmpty()) {
            return new ResponseEntity<>(ranking, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(ranking, HttpStatus.ACCEPTED);
    }

    /**
     * Obtiene las estadísticas personales de un usuario en un grupo específico
     * (HU22 — Ver estadísticas personales). Retorna su posición, puntos,
     * predicciones totales, aciertos y marcadores exactos.
     *
     * @param groupId El ID del grupo de polla.
     * @param userId  El ID del usuario.
     * @return 202 Accepted con las estadísticas del usuario; 404 si no se
     *         encuentran datos del usuario en ese grupo.
     */
    @GetMapping("/group/{groupId}/user/{userId}")
    @Operation(summary = "Estadísticas personales en un grupo",
               description = "Retorna posición, puntos y métricas de rendimiento del usuario en el grupo.")
    public ResponseEntity<?> getUserStats(@PathVariable Long groupId, @PathVariable Long userId) {
        List<RankingDTO> ranking = pollService.getRankingByGroup(groupId);
        RankingDTO userStats = ranking.stream()
                .filter(r -> r.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (userStats != null) {
            return new ResponseEntity<>(userStats, HttpStatus.ACCEPTED);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuario no encontrado en este grupo", "success", false));
    }

    /**
     * Obtiene el historial de predicciones evaluadas de un usuario en un grupo
     * (HU14 — Ver historial, HU15 — Ver resultado de predicciones).
     * Incluye el marcador real de cada partido para comparar con el pronóstico.
     *
     * @param userId      El ID del usuario.
     * @param pollGroupId El ID del grupo de polla.
     * @return 202 Accepted con el historial evaluado; 204 si no hay registros.
     */
    @GetMapping("/user/{userId}/group/{pollGroupId}/history")
    @Operation(summary = "Historial de predicciones evaluadas",
               description = "Retorna pronósticos con marcador real y puntos obtenidos por partido.")
    public ResponseEntity<List<PredictionDTO>> getPredictionHistory(
            @PathVariable Long userId, @PathVariable Long pollGroupId) {
        List<PredictionDTO> history = pollService.getPredictionsByUserAndGroup(userId, pollGroupId);
        if (history.isEmpty()) {
            return new ResponseEntity<>(history, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(history, HttpStatus.ACCEPTED);
    }
}