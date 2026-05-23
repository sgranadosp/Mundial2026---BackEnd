package co.edu.unbosque.mundial2026.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.pack.OpenPackResultDTO;
import co.edu.unbosque.mundial2026.dto.pack.PackHistoryEntryDTO;
import co.edu.unbosque.mundial2026.dto.pack.PackInventoryDTO;
import co.edu.unbosque.mundial2026.model.StickerPack.Origin;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.service.PackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST del módulo de Packs.
 *
 * Expone 3 endpoints:
 * <ul>
 *   <li>{@code GET  /packs/me/inventory} → refresca y devuelve el inventario
 *       (otorga packs pendientes según las reglas y devuelve contadores).</li>
 *   <li>{@code POST /packs/me/open?origin=WELCOME|DAILY|POLL} → abre el pack
 *       más antiguo pendiente de ese origen y devuelve las 5 láminas.</li>
 *   <li>{@code GET  /packs/me/history} → lista todos los packs (abiertos
 *       y pendientes) del usuario para la tabla del historial.</li>
 * </ul>
 *
 * Todos extraen el userId del JWT (no se acepta como parámetro). Esto es
 * coherente con el resto de controllers refactorizados de la app.
 */
@RestController
@RequestMapping("/packs")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081",
        "http://localhost:8082", "http://localhost:4200",
        "http://localhost:3000", "http://localhost:5173" })
@Transactional
@Tag(name = "Packs", description = "Paquetes de láminas: bienvenida, diarios y por pronósticos")
@SecurityRequirement(name = "bearerAuth")
public class PackController {

    @Autowired
    private PackService packService;

    public PackController() {}

    /**
     * Refresca y devuelve el inventario de packs del usuario autenticado.
     * Otorga automáticamente los packs pendientes (WELCOME en primer login,
     * DAILY si pasaron ≥12h, POLL si llegó a un múltiplo de 6 predicciones).
     */
    @GetMapping("/me/inventory")
    @Operation(summary = "Inventario de packs",
               description = "Aplica las reglas de otorgamiento y devuelve los contadores actualizados.")
    public ResponseEntity<?> getInventory(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado", "success", false));
        }
        PackInventoryDTO inventory = packService.refreshAndGetInventory(userId);
        if (inventory == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        return ResponseEntity.ok(inventory);
    }

    /**
     * Abre el pack más antiguo pendiente del origen indicado y devuelve las
     * 5 láminas obtenidas + el inventario actualizado.
     */
    @PostMapping("/me/open")
    @Operation(summary = "Abrir pack",
               description = "Abre el pack pendiente más antiguo del origen indicado y devuelve 5 láminas.")
    public ResponseEntity<?> openPack(@RequestParam Origin origin, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autenticado", "success", false));
        }
        OpenPackResultDTO result = packService.openPack(userId, origin);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message",
                            "No tienes packs pendientes de este tipo",
                            "success", false));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Lista todos los packs del usuario para la tabla de historial.
     */
    @GetMapping("/me/history")
    @Operation(summary = "Historial de packs",
               description = "Devuelve los packs abiertos y pendientes ordenados por fecha descendente.")
    public ResponseEntity<List<PackHistoryEntryDTO>> getHistory(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<PackHistoryEntryDTO> history = packService.getHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Extrae el ID del usuario autenticado desde el {@link Authentication}.
     */
    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        return null;
    }
}