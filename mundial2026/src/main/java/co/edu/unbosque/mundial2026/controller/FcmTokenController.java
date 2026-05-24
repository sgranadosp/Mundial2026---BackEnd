/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller para gestionar el token FCM del navegador del usuario.
 * <p>
 * El frontend llama a {@code PUT /fcm-token/{userId}} apenas el usuario se
 * loguea y obtiene un token de Firebase, para que el backend pueda enviar
 * notificaciones push a ese dispositivo. Cuando el usuario cierra sesión
 * o revoca permisos, el frontend llama {@code DELETE /fcm-token/{userId}}
 * para que el backend deje de intentar enviar push a un token muerto.
 * </p>
 */
@RestController
@RequestMapping("/fcm-token")
@Tag(name = "FCM Tokens", description = "Registro y limpieza del token FCM del navegador")
@SecurityRequirement(name = "bearerAuth")
public class FcmTokenController {

    private static final Logger log = LoggerFactory.getLogger(FcmTokenController.class);

    @Autowired
    private UserRepository userRepo;

    /** Constructor por defecto. */
    public FcmTokenController() {
    }

    /**
     * Registra o actualiza el token FCM del navegador para un usuario.
     *
     * @param userId  ID del usuario (debe coincidir con la sesión actual).
     * @param payload Cuerpo JSON con {@code { "token": "..." }}.
     * @return 200 si se guardó; 404 si el usuario no existe; 400 si el
     *         payload está vacío.
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Registra/actualiza el token FCM del usuario",
               description = "Llamado por el frontend tras autorizar push notifications")
    public ResponseEntity<?> register(@PathVariable Long userId,
                                       @RequestBody Map<String, String> payload) {
        String token = payload != null ? payload.get("token") : null;
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "token requerido", "success", false));
        }
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        User user = userOpt.get();
        user.setFcmToken(token);
        userRepo.save(user);
        log.info("[FCM] Token registrado para user={} ({} chars)", userId, token.length());
        return ResponseEntity.ok(Map.of("message", "Token registrado", "success", true));
    }

    /**
     * Borra el token FCM del usuario. Llamado al logout o al revocar permisos
     * de notificación en el navegador.
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Elimina el token FCM del usuario",
               description = "Llamado al logout o cuando el usuario revoca permisos")
    public ResponseEntity<?> unregister(@PathVariable Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        User user = userOpt.get();
        user.setFcmToken(null);
        userRepo.save(user);
        log.info("[FCM] Token eliminado para user={}", userId);
        return ResponseEntity.ok(Map.of("message", "Token eliminado", "success", true));
    }
}