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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.trade.CreateTradeRequestDTO;
import co.edu.unbosque.mundial2026.dto.trade.SimpleStickerDTO;
import co.edu.unbosque.mundial2026.dto.trade.TradeRequestDTO;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST del módulo de intercambios.
 *
 * <h3>NOTA IMPORTANTE sobre códigos HTTP</h3>
 * Los errores de regla de negocio (intentar aceptar cuando no cumples las
 * condiciones, no tener la lámina pedida, etc.) se devuelven como
 * <b>409 CONFLICT</b> en lugar de 403 FORBIDDEN. Esto es porque el
 * interceptor axios del frontend tiene una política global de auto-logout
 * + redirect a /logIn ante cualquier 401/403, lo cual es correcto para
 * "token inválido / sesión expirada", pero NO para "request bien
 * autenticado pero rechazado por regla de negocio".
 *
 * Convención de códigos en este controller:
 * <ul>
 *   <li><b>200/201/202</b>: éxito.</li>
 *   <li><b>400 BAD_REQUEST</b>: datos del request inválidos o incompletos.</li>
 *   <li><b>401 UNAUTHORIZED</b>: solo cuando NO hay sesión (token ausente).
 *       Aquí sí queremos que el front redirija al login.</li>
 *   <li><b>404 NOT_FOUND</b>: entidad no existe (solicitud, lámina, usuario).</li>
 *   <li><b>409 CONFLICT</b>: regla de negocio violada — NO dispara logout.
 *       El front muestra el mensaje y deja al usuario en la misma página.</li>
 * </ul>
 */
@RestController
@RequestMapping("/trades")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081",
        "http://localhost:8082", "http://localhost:4200",
        "http://localhost:3000", "http://localhost:5173" })
@Transactional
@Tag(name = "Intercambios", description = "Solicitudes de intercambio de láminas")
@SecurityRequirement(name = "bearerAuth")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    public TradeController() {}

    /**
     * Crea una nueva solicitud de intercambio del usuario autenticado.
     */
    @PostMapping
    @Operation(summary = "Crear solicitud de intercambio")
    public ResponseEntity<?> createRequest(@RequestBody CreateTradeRequestDTO body,
                                            Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return unauth();
        }
        if (body == null
                || body.getOfferedStickerId() == null
                || body.getRequestedStickerId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Debes indicar la lámina ofrecida y la lámina deseada",
                            "success", false));
        }
        int code = tradeService.createRequest(userId,
                body.getOfferedStickerId(), body.getRequestedStickerId());
        return switch (code) {
            case 0 -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Solicitud creada exitosamente",
                            "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "Ya tienes 5 solicitudes activas. Espera a que se completen o cancela alguna.",
                            "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message",
                            "Lámina o usuario no encontrado",
                            "success", false));
            case 3 -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Las dos láminas deben ser distintas",
                            "success", false));
            // FIX: usar 409 CONFLICT en lugar de 403 para que el interceptor
            // axios del front no dispare el auto-logout.
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "No cumples los requisitos: debes tener la lámina ofrecida como REPETIDA y la pedida como FALTANTE",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Error al crear la solicitud", "success", false));
        };
    }

    /**
     * Lista las solicitudes activas que el usuario puede ver en su bandeja.
     */
    @GetMapping("/received")
    @Operation(summary = "Solicitudes recibidas")
    public ResponseEntity<List<TradeRequestDTO>> getReceived(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(tradeService.getReceivedByUser(userId));
    }

    /**
     * Lista las solicitudes activas creadas por el usuario.
     */
    @GetMapping("/mine")
    @Operation(summary = "Mis solicitudes activas")
    public ResponseEntity<List<TradeRequestDTO>> getMine(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(tradeService.getMyRequests(userId));
    }

    /**
     * Acepta una solicitud y ejecuta el intercambio.
     */
    @PostMapping("/{tradeId}/accept")
    @Operation(summary = "Aceptar solicitud (ejecuta el intercambio)")
    public ResponseEntity<?> accept(@PathVariable Long tradeId, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return unauth();
        }
        int code = tradeService.acceptRequest(tradeId, userId);
        return switch (code) {
            case 0 -> ResponseEntity.ok(Map.of(
                    "message", "Intercambio realizado con éxito, revisa en tu álbum la lámina que te llegó",
                    "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message",
                            "La solicitud ya no existe o ha sido cerrada",
                            "success", false));
            // FIX: usar 409 CONFLICT (regla de negocio, no auth)
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "No puedes realizar este intercambio",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Error al procesar el intercambio",
                            "success", false));
        };
    }

    /**
     * Rechaza una solicitud (la oculta solo para mí).
     */
    @PostMapping("/{tradeId}/reject")
    @Operation(summary = "Rechazar solicitud (oculta solo para mí)")
    public ResponseEntity<?> reject(@PathVariable Long tradeId, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return unauth();
        }
        int code = tradeService.rejectRequest(tradeId, userId);
        return switch (code) {
            case 0 -> ResponseEntity.ok(Map.of(
                    "message", "Solicitud rechazada", "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "Ya habías rechazado esta solicitud",
                            "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "La solicitud no existe",
                            "success", false));
            // FIX: usar 409 CONFLICT (regla de negocio, no auth)
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "No puedes rechazar esta solicitud",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al rechazar la solicitud",
                            "success", false));
        };
    }

    /**
     * Cancela una solicitud propia (no implementado en UI por ahora).
     */
    @DeleteMapping("/mine/{tradeId}")
    @Operation(summary = "Cancelar mi solicitud")
    public ResponseEntity<?> cancelMine(@PathVariable Long tradeId, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return unauth();
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message",
                        "La cancelación manual aún no está habilitada",
                        "success", false));
    }

    // ─── Autocompletes ──────────────────────────────────────────────────

    @GetMapping("/autocomplete/countries-with-repetidas")
    @Operation(summary = "Países con láminas repetidas del usuario")
    public ResponseEntity<List<Map<String, String>>> getCountriesWithRepetidas(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Map<String, String> map = tradeService.getCountriesWithRepetidas(userId);
        List<Map<String, String>> body = map.entrySet().stream()
                .map(e -> Map.of("code", e.getKey(), "name", e.getValue()))
                .sorted((a, b) -> a.get("name").compareTo(b.get("name")))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/autocomplete/countries-with-faltantes")
    @Operation(summary = "Países con láminas faltantes del usuario")
    public ResponseEntity<List<Map<String, String>>> getCountriesWithFaltantes(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Map<String, String> map = tradeService.getCountriesWithFaltantes(userId);
        List<Map<String, String>> body = map.entrySet().stream()
                .map(e -> Map.of("code", e.getKey(), "name", e.getValue()))
                .sorted((a, b) -> a.get("name").compareTo(b.get("name")))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/autocomplete/repetidas/{countryCode}")
    @Operation(summary = "Láminas repetidas en una selección")
    public ResponseEntity<List<SimpleStickerDTO>> getRepetidasInCountry(
            @PathVariable String countryCode, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(tradeService.getRepetidasInCountry(userId, countryCode));
    }

    @GetMapping("/autocomplete/faltantes/{countryCode}")
    @Operation(summary = "Láminas faltantes en una selección")
    public ResponseEntity<List<SimpleStickerDTO>> getFaltantesInCountry(
            @PathVariable String countryCode, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(tradeService.getFaltantesInCountry(userId, countryCode));
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private ResponseEntity<?> unauth() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "No autenticado", "success", false));
    }
}