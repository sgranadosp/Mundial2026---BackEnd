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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.album.AlbumPageDTO;
import co.edu.unbosque.mundial2026.dto.album.CountryOptionDTO;
import co.edu.unbosque.mundial2026.dto.album.OpenPackageResponseDTO;
import co.edu.unbosque.mundial2026.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para la sección de álbum de láminas del Mundial 2026 Hub.
 * <p>
 * Expone los endpoints para: 
 * <ul>
 *   <li>Listar las selecciones disponibles para el autocomplete del filtro.</li>
 *   <li>Consultar la página de una selección para el usuario actual con su
 *       estado por casilla (pegada / repetida / faltante).</li>
 *   <li>Aplicar filtros por estado dentro de una página.</li>
 *   <li>Abrir un paquete de láminas y agregar las nuevas a la colección.</li>
 *   <li>Consultar el progreso global del álbum del usuario.</li>
 * </ul>
 * Todos los endpoints requieren JWT (rol USER o ADMIN).
 * </p>
 */
@RestController
@RequestMapping("/album")
@Transactional
@Tag(name = "Álbum", description = "Catálogo de láminas, colección personal y apertura de paquetes")
@SecurityRequirement(name = "bearerAuth")
public class AlbumController {

    /**
     * Servicio de lógica de negocio del álbum.
     */
    @Autowired
    private AlbumService albumService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public AlbumController() {
    }

    // =========================================================================
    // Catálogo / metadata
    // =========================================================================

    /**
     * Lista todas las selecciones disponibles para el autocomplete del filtro.
     * Devuelve las 48 selecciones nacionales más la página de "Especiales"
     * al final.
     *
     * @return 200 OK con la lista de opciones (código + nombre).
     */
    @GetMapping("/countries")
    @Operation(summary = "Listar selecciones disponibles",
               description = "Devuelve las 48 selecciones + 'Especiales' para poblar el autocomplete.")
    public ResponseEntity<List<CountryOptionDTO>> getCountries() {
        return ResponseEntity.ok(albumService.getAvailableCountries());
    }

    // =========================================================================
    // Página del álbum
    // =========================================================================

    /**
     * Devuelve el álbum completo del usuario como una única página agrupando
     * las láminas de todas las selecciones, opcionalmente filtrada por estado.
     * <p>
     * Pensado para la vista inicial del álbum cuando aún no hay una selección
     * elegida en el autocomplete, de modo que el usuario vea toda su colección
     * sin tener que iterar selección por selección.
     * </p>
     *
     * @param userId ID del usuario consultante.
     * @param filter Filtro de estado: {@code TODAS} (por defecto), {@code PEGADA},
     *               {@code REPETIDA}, {@code FALTANTE}.
     * @return 200 OK con la página completa; 404 si el usuario no existe.
     */
    @GetMapping("/users/{userId}/pages")
    @Operation(summary = "Ver álbum completo del usuario",
               description = "Devuelve todas las láminas del catálogo con el estado del usuario,"
                       + " opcionalmente filtradas por PEGADA, REPETIDA o FALTANTE.")
    public ResponseEntity<?> getFullAlbum(@PathVariable Long userId,
                                           @RequestParam(required = false, defaultValue = "TODAS") String filter) {
        AlbumPageDTO page = albumService.getFullAlbum(userId, filter);
        if (page == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        return ResponseEntity.ok(page);
    }

    /**
     * Devuelve la página del álbum del usuario para una selección, opcionalmente
     * filtrada por estado de posesión.
     *
     * @param userId      ID del usuario consultante.
     * @param countryCode Código ASCII de la selección (ej. {@code colombia}).
     * @param filter      Filtro de estado: {@code TODAS} (por defecto), {@code PEGADA},
     *                    {@code REPETIDA}, {@code FALTANTE}.
     * @return 200 OK con la página construida; 404 si el usuario o la selección no existen.
     */
    @GetMapping("/users/{userId}/pages/{countryCode}")
    @Operation(summary = "Ver página del álbum por selección",
               description = "Devuelve las 6 láminas de la selección con su estado para el usuario,"
                       + " opcionalmente filtradas por PEGADA, REPETIDA o FALTANTE.")
    public ResponseEntity<?> getPage(@PathVariable Long userId,
                                      @PathVariable String countryCode,
                                      @RequestParam(required = false, defaultValue = "TODAS") String filter) {
        AlbumPageDTO page = albumService.getAlbumPage(userId, countryCode, filter);
        if (page == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario o selección no encontrados", "success", false));
        }
        return ResponseEntity.ok(page);
    }

    // =========================================================================
    // Abrir paquete
    // =========================================================================

    /**
     * Abre un paquete de 5 láminas aleatorias del catálogo para el usuario indicado.
     *
     * @param userId ID del usuario que abre el paquete.
     * @return 200 OK con las láminas obtenidas y el progreso actualizado;
     *         404 si el usuario no existe.
     */
    @PostMapping("/users/{userId}/packages/open")
    @Operation(summary = "Abrir paquete de láminas",
               description = "Genera 5 láminas aleatorias del catálogo y las agrega a la colección.")
    public ResponseEntity<?> openPackage(@PathVariable Long userId) {
        OpenPackageResponseDTO result = albumService.openPackage(userId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Progreso del usuario
    // =========================================================================

    /**
     * Devuelve el progreso global del álbum del usuario:
     * cuántas láminas únicas tiene y cuántas hay en el catálogo total.
     *
     * @param userId ID del usuario consultante.
     * @return 200 OK con el progreso.
     */
    @GetMapping("/users/{userId}/progress")
    @Operation(summary = "Ver progreso del álbum",
               description = "Devuelve cuántas láminas únicas tiene el usuario y el total del catálogo.")
    public ResponseEntity<?> getProgress(@PathVariable Long userId) {
        long collected = albumService.getUserProgress(userId);
        long total = albumService.getCatalogSize();
        return ResponseEntity.ok(Map.of(
                "collected", collected,
                "total", total,
                "percentage", total == 0 ? 0 : Math.round(((double) collected / total) * 100)));
    }
}