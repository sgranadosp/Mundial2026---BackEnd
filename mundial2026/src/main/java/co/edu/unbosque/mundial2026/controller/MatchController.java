/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.MatchDTO;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.Match.Phase;
import co.edu.unbosque.mundial2026.service.ExternalMatchService;
import co.edu.unbosque.mundial2026.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para la consulta y gestión de partidos del Mundial 2026.
 * Provee los endpoints del módulo de partidos: listado general, detalle,
 * resultados y filtros por fecha, equipo, estadio y fase (HU06, HU07, HU08,
 * HU09, HU10). Los endpoints de sincronización con la API externa son
 * exclusivos para administradores.
 */
@RestController
@RequestMapping("/matches")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Tag(name = "Partidos", description = "Consulta y gestión de partidos del Mundial 2026")
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

    /**
     * Servicio de lógica de negocio de partidos.
     */
    @Autowired
    private MatchService matchService;

    /**
     * Adaptador del servicio externo de datos deportivos.
     */
    @Autowired
    private ExternalMatchService externalMatchService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public MatchController() {
    }

    // =========================================================================
    // Endpoints de consulta (USER y ADMIN)
    // =========================================================================

    /**
     * Lista todos los partidos disponibles (HU06 — Ver lista de partidos).
     *
     * @return 202 Accepted con la lista de partidos; 204 No Content si está vacía.
     */
    @GetMapping
    @Operation(summary = "Listar todos los partidos",
               description = "Retorna todos los partidos con datos de equipos y estadio aplanados.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> getAll() {
        List<MatchDTO> matches = matchService.getAll();
        if (matches.isEmpty()) {
            return new ResponseEntity<>(matches, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(matches, HttpStatus.ACCEPTED);
    }

    /**
     * Obtiene el detalle de un partido específico (HU07 — Ver detalles de partido).
     *
     * @param id El ID interno del partido.
     * @return 202 Accepted con el partido; 404 Not Found si no existe.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Ver detalle de partido",
               description = "Retorna información completa de un partido: equipos, estadio, marcador y estado.")
    @Transactional(readOnly = true)
    public ResponseEntity<MatchDTO> getById(@PathVariable Long id) {
        MatchDTO match = matchService.getById(id);
        if (match != null) {
            return new ResponseEntity<>(match, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(new MatchDTO(), HttpStatus.NOT_FOUND);
    }

    /**
     * Filtra partidos por estado del ciclo de vida.
     * Caso de uso principal: obtener partidos SCHEDULED para la agenda
     * y partidos FINISHED para ver resultados (HU08 — Ver resultados de partidos).
     *
     * @param status El estado del partido: SCHEDULED, LIVE o FINISHED.
     * @return 202 Accepted con los partidos filtrados; 204 si no hay resultados.
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar partidos por estado",
               description = "Filtra por SCHEDULED (programados), LIVE (en juego) o FINISHED (finalizados).")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> getByStatus(@PathVariable MatchStatus status) {
        List<MatchDTO> matches = matchService.getByStatus(status);
        if (matches.isEmpty()) {
            return new ResponseEntity<>(matches, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(matches, HttpStatus.ACCEPTED);
    }

    /**
     * Filtra partidos dentro de un rango de fechas (HU09 — Filtrar partidos por fecha).
     *
     * @param start Fecha y hora de inicio del rango en formato ISO (UTC).
     * @param end   Fecha y hora de fin del rango en formato ISO (UTC).
     * @return 202 Accepted con los partidos en el rango; 204 si no hay resultados.
     */
    @GetMapping("/byDate")
    @Operation(summary = "Filtrar partidos por fecha",
               description = "Retorna partidos programados dentro del rango de fechas indicado (UTC).")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<MatchDTO> matches = matchService.getByDateRange(start, end);
        if (matches.isEmpty()) {
            return new ResponseEntity<>(matches, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(matches, HttpStatus.ACCEPTED);
    }

    /**
     * Filtra partidos por fase del torneo.
     *
     * @param phase La fase del torneo: GROUP_STAGE, ROUND_OF_32, QUARTER_FINALS, etc.
     * @return 202 Accepted con los partidos de la fase; 204 si no hay resultados.
     */
    @GetMapping("/phase/{phase}")
    @Operation(summary = "Filtrar partidos por fase",
               description = "Retorna todos los partidos de la fase del torneo indicada.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> getByPhase(@PathVariable Phase phase) {
        List<MatchDTO> matches = matchService.getByPhase(phase);
        if (matches.isEmpty()) {
            return new ResponseEntity<>(matches, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(matches, HttpStatus.ACCEPTED);
    }

    /**
     * Obtiene todos los partidos de un equipo (como local o visitante).
     * Se usa en la agenda personalizada del usuario según su equipo favorito.
     *
     * @param teamId El ID interno del equipo.
     * @return 202 Accepted con los partidos del equipo; 204 si no hay resultados.
     */
    @GetMapping("/team/{teamId}")
    @Operation(summary = "Partidos de un equipo",
               description = "Retorna todos los partidos en los que participa el equipo indicado.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> getByTeam(@PathVariable Long teamId) {
        List<MatchDTO> matches = matchService.getByTeam(teamId);
        if (matches.isEmpty()) {
            return new ResponseEntity<>(matches, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(matches, HttpStatus.ACCEPTED);
    }

    /**
     * Obtiene todos los partidos de un estadio.
     *
     * @param stadiumId El ID interno del estadio.
     * @return 202 Accepted con los partidos del estadio; 204 si no hay resultados.
     */
    @GetMapping("/stadium/{stadiumId}")
    @Operation(summary = "Partidos de un estadio",
               description = "Retorna todos los partidos programados en el estadio indicado.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> getByStadium(@PathVariable Long stadiumId) {
        List<MatchDTO> matches = matchService.getByStadium(stadiumId);
        if (matches.isEmpty()) {
            return new ResponseEntity<>(matches, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(matches, HttpStatus.ACCEPTED);
    }

    // =========================================================================
    // Endpoints administrativos (solo ADMIN)
    // =========================================================================

    /**
     * Sincroniza todos los partidos desde la API externa (football-data.org o
     * WireMock). Crea o actualiza registros sin duplicar. Solo ADMIN.
     *
     * @return 200 OK con el número de partidos sincronizados; 500 si falla la API.
     */
    @PostMapping("/admin/sync")
    @Operation(summary = "Sincronizar partidos desde API externa",
               description = "Solo ADMIN. Importa o actualiza todos los partidos desde la fuente de datos.")
    @Transactional(readOnly = true)
    public ResponseEntity<?> syncFromExternalApi() {
        int synced = externalMatchService.syncAllMatches();
        if (synced >= 0) {
            return ResponseEntity.ok(
                    Map.of("message", "Sincronización completada", "synced", synced, "success", true));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error al conectar con la API externa", "success", false));
    }

    /**
     * Actualiza manualmente el resultado de un partido finalizado.
     * Solo ADMIN. Usado cuando la API externa no actualiza automáticamente.
     *
     * @param id        El ID interno del partido.
     * @param status    El nuevo estado del partido.
     * @param homeScore Goles del equipo local.
     * @param awayScore Goles del equipo visitante.
     * @return 202 Accepted si fue actualizado; 404 si no existe.
     */
    @PutMapping("/admin/{id}/result")
    @Operation(summary = "Actualizar resultado de partido",
               description = "Solo ADMIN. Actualiza marcador y estado de un partido manualmente.")
    @Transactional(readOnly = true)
    public ResponseEntity<?> updateResult(@PathVariable Long id,
                                          @RequestParam MatchStatus status,
                                          @RequestParam(required = false) Integer homeScore,
                                          @RequestParam(required = false) Integer awayScore) {
        MatchDTO update = new MatchDTO();
        update.setId(id);
        update.setStatus(status);
        update.setHomeScore(homeScore);
        update.setAwayScore(awayScore);

        int result = matchService.updateById(id, update);

        return switch (result) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Resultado actualizado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Partido no encontrado", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al actualizar el resultado", "success", false));
        };
    }

    /**
     * Cuenta el total de partidos registrados en la plataforma.
     *
     * @return 202 Accepted con el conteo; 204 si no hay partidos.
     */
    @GetMapping("/count")
    @Operation(summary = "Contar partidos",
               description = "Retorna el número total de partidos registrados.")
    @Transactional(readOnly = true)
    public ResponseEntity<Long> countAll() {
        long count = matchService.count();
        if (count == 0) {
            return new ResponseEntity<>(count, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(count, HttpStatus.ACCEPTED);
    }
    

    /**
     * Devuelve los partidos de fase de grupos disponibles para pronosticar:
     * estado SCHEDULED, fecha futura.
     *
     * @return 200 OK con la lista de partidos pronosticables.
     */
    @GetMapping("/available-for-polls")
    @Operation(summary = "Partidos disponibles para pronóstico",
               description = "Lista los partidos de fase de grupos en estado SCHEDULED.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> availableForPolls() {
        return ResponseEntity.ok(matchService.getAvailableForPolls());
    }
 
    /**
     * Devuelve los partidos de fase de grupos disponibles para compra de tickets.
     *
     * @return 200 OK con la lista de partidos.
     */
    @GetMapping("/available-for-tickets")
    @Operation(summary = "Partidos disponibles para compra de tickets",
               description = "Lista los partidos de fase de grupos que aún no han iniciado.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchDTO>> availableForTickets() {
        return ResponseEntity.ok(matchService.getAvailableForTickets());
    }
 
}