/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.TeamDTO;
import co.edu.unbosque.mundial2026.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de selecciones (HU05 — Preferencias del usuario).
 * <p>
 * Expone endpoints de solo lectura sobre los equipos sincronizados desde la
 * API externa de fútbol. Los datos se replicaron previamente a la tabla
 * {@code teams} mediante el flujo {@code POST /admin/sync/fixtures}; este
 * controlador se limita a servirlos al frontend.
 * </p>
 * <p>
 * Ambos endpoints son accesibles por roles {@code USER} y {@code ADMIN}.
 * La política CORS y la autenticación JWT se gestionan en {@code SecurityConfig}.
 * </p>
 */
@RestController
@RequestMapping("/teams")
@Tag(name = "Selecciones", description = "Listado de selecciones nacionales del Mundial 2026")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    /**
     * Servicio de lógica de negocio de selecciones.
     */
    @Autowired
    private TeamService teamService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public TeamController() {
    }

    /**
     * Lista todas las selecciones registradas, ordenadas alfabéticamente por
     * nombre para facilitar la selección desde el desplegable del perfil.
     *
     * @return 202 Accepted con la lista de equipos; 204 No Content si la
     *         tabla está vacía (aún no se ha hecho sync).
     */
    @GetMapping
    @Operation(summary = "Listar selecciones",
               description = "Retorna todas las selecciones sincronizadas, ordenadas por nombre.")
    public ResponseEntity<List<TeamDTO>> getAll() {
        List<TeamDTO> teams = teamService.getAll();
        if (teams.isEmpty()) {
            return new ResponseEntity<>(teams, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(teams, HttpStatus.ACCEPTED);
    }

    /**
     * Busca una selección por su código ISO-3166 alfa-3 (ej. "COL", "BRA").
     * Útil para resolver el equipo favorito guardado en el perfil del
     * usuario y mostrar nombre/escudo en pantallas que solo guardan el ISO.
     *
     * @param isoCode Código ISO alfa-3 (case-insensitive).
     * @return 202 Accepted con el equipo; 404 Not Found si no existe.
     */
    @GetMapping("/iso/{isoCode}")
    @Operation(summary = "Buscar selección por ISO",
               description = "Retorna la selección cuyo código ISO alfa-3 coincide.")
    public ResponseEntity<TeamDTO> getByIsoCode(@PathVariable String isoCode) {
        TeamDTO found = teamService.getByIsoCode(isoCode);
        if (found != null) {
            return new ResponseEntity<>(found, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }
}
