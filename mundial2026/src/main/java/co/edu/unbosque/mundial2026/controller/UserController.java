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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.UserDTO;
import co.edu.unbosque.mundial2026.model.User.Role;
import co.edu.unbosque.mundial2026.service.AuditEventService;
import co.edu.unbosque.mundial2026.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para la gestión de usuarios en la plataforma Mundial 2026 Hub.
 * Expone endpoints para operaciones de perfil del usuario autenticado (HU04,
 * HU05) y operaciones administrativas reservadas para el rol ADMIN (HU16, HU17,
 * HU18, HU19, HU26, HU29). La protección por rol se configura en
 * {@code SecurityConfig}. La política CORS se gestiona centralmente en
 * {@code SecurityConfig.corsConfigurationSource()}. Todos los endpoints
 * requieren token JWT Bearer.
 */
@RestController
@RequestMapping("/users")
@Transactional
@Tag(name = "Usuarios", description = "Gestión de perfiles y administración de cuentas")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    /**
     * Servicio de lógica de negocio de usuarios.
     */
    @Autowired
    private UserService userService;

    /**
     * Servicio centralizado de auditoría.
     */
    @Autowired
    private AuditEventService auditService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public UserController() {
    }

    // =========================================================================
    // Endpoints de perfil (USER y ADMIN)
    // =========================================================================

    /**
     * Obtiene el perfil del usuario por su ID (HU04 — Ver perfil).
     *
     * @param id El ID del usuario a consultar.
     * @return 202 Accepted con el {@link UserDTO} (email desencriptado); 404 si no existe.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Ver perfil de usuario",
               description = "Retorna los datos del perfil del usuario con el email desencriptado.")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        UserDTO found = userService.getById(id);
        if (found != null) {
            return new ResponseEntity<>(found, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(new UserDTO(), HttpStatus.NOT_FOUND);
    }

    /**
     * Actualiza los datos del perfil del usuario autenticado (HU05 — Editar perfil).
     * Permite modificar el nombre y las preferencias de notificación. No actualiza
     * contraseña ni email por este endpoint.
     *
     * @param id      El ID del usuario a actualizar.
     * @param newData El DTO con los nuevos datos del perfil.
     * @return 202 Accepted si fue actualizado; 404 si no existe; 409 si hay HTML
     *         en los campos.
     */
    @PutMapping("/{id}/profile")
    @Operation(summary = "Editar perfil",
               description = "Actualiza nombre y preferencias de notificación del usuario.")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody UserDTO newData) {
        int status = userService.updateById(id, newData);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Perfil actualizado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
            case 6 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Los campos no pueden contener caracteres HTML", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al actualizar el perfil", "success", false));
        };
    }

    /**
     * Actualiza el correo electrónico del usuario.
     *
     * @param id       El ID del usuario.
     * @param newEmail El nuevo correo electrónico sin encriptar.
     * @return 202 Accepted si fue actualizado; 409 si ya está en uso o el
     *         formato no es válido; 404 si no existe.
     */
    @PutMapping("/{id}/email")
    @Operation(summary = "Actualizar correo electrónico",
               description = "Actualiza el email del usuario validando formato y unicidad.")
    public ResponseEntity<?> updateEmail(@PathVariable Long id, @RequestParam String newEmail) {
        int status = userService.updateEmail(id, newEmail);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Correo actualizado exitosamente", "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El correo ya está en uso", "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
            case 5 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El formato del correo no es válido", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al actualizar el correo", "success", false));
        };
    }

    /**
     * Verifica si un username ya está registrado. Usado en el formulario
     * de registro para feedback en tiempo real.
     *
     * @param username El username a verificar (texto plano).
     * @return 202 Accepted con {@code true} si existe; 204 No Content si no existe.
     */
    @GetMapping("/exists")
    @Operation(summary = "Verificar disponibilidad de username",
               description = "Retorna true si el username ya está registrado.")
    public ResponseEntity<Boolean> usernameExists(@RequestParam String username) {
        boolean exists = userService.usernameExists(username);
        if (exists) {
            return new ResponseEntity<>(true, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(false, HttpStatus.NO_CONTENT);
    }

    // =========================================================================
    // Endpoints administrativos (solo ADMIN)
    // =========================================================================

    /**
     * Lista todos los usuarios registrados (HU16 — Ver lista de usuarios).
     * Solo accesible para administradores.
     *
     * @return 202 Accepted con la lista de usuarios (email desencriptado); 204 si está vacía.
     */
    @GetMapping
    @Operation(summary = "Listar todos los usuarios",
               description = "Solo ADMIN. Retorna todos los usuarios con email desencriptado.")
    public ResponseEntity<List<UserDTO>> getAll() {
        List<UserDTO> users = userService.getAll();
        if (users.isEmpty()) {
            return new ResponseEntity<>(users, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(users, HttpStatus.ACCEPTED);
    }

    /**
     * Bloquea la cuenta de un usuario (HU17 — Bloquear usuario).
     * Solo accesible para administradores.
     *
     * @param id      El ID del usuario a bloquear.
     * @param adminId El ID del administrador que realiza la acción (para auditoría).
     * @return 202 Accepted si fue bloqueado; 404 si no existe.
     */
    @PutMapping("/{id}/block")
    @Operation(summary = "Bloquear usuario",
               description = "Solo ADMIN. Bloquea la cuenta del usuario indicado.")
    public ResponseEntity<?> blockUser(@PathVariable Long id, @RequestParam Long adminId) {
        int status = userService.blockUser(id);

        if (status == 0) {
            auditService.logUserBlocked(id, adminId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Usuario bloqueado exitosamente", "success", true));
        } else if (status == 2) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error al bloquear el usuario", "success", false));
    }

    /**
     * Desbloquea la cuenta de un usuario (HU18 — Desbloquear usuario).
     * Solo accesible para administradores.
     *
     * @param id      El ID del usuario a desbloquear.
     * @param adminId El ID del administrador que realiza la acción.
     * @return 202 Accepted si fue desbloqueado; 404 si no existe.
     */
    @PutMapping("/{id}/unblock")
    @Operation(summary = "Desbloquear usuario",
               description = "Solo ADMIN. Reactiva la cuenta del usuario indicado.")
    public ResponseEntity<?> unblockUser(@PathVariable Long id, @RequestParam Long adminId) {
        int status = userService.unblockUser(id);

        if (status == 0) {
            auditService.logUserUnblocked(id, adminId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Usuario desbloqueado exitosamente", "success", true));
        } else if (status == 2) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error al desbloquear el usuario", "success", false));
    }

    /**
     * Elimina un usuario del sistema (HU19 — Eliminar usuario).
     * Solo accesible para administradores.
     *
     * @param id      El ID del usuario a eliminar.
     * @param adminId El ID del administrador que realiza la acción.
     * @return 202 Accepted si fue eliminado; 404 si no existe.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario",
               description = "Solo ADMIN. Elimina definitivamente el usuario y sus datos.")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestParam Long adminId) {
        int status = userService.deleteById(id);

        if (status == 0) {
            auditService.logUserDeleted(id, adminId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Usuario eliminado exitosamente", "success", true));
        } else if (status == 2) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Error al eliminar el usuario", "success", false));
    }

    /**
     * Asigna un rol a un usuario (HU26 — Gestionar roles).
     * Solo accesible para administradores.
     *
     * @param id   El ID del usuario.
     * @param role El nuevo rol: "USER" o "ADMIN".
     * @return 202 Accepted si fue actualizado; 404 si no existe; 400 si el rol
     *         no es válido.
     */
    @PutMapping("/{id}/role")
    @Operation(summary = "Asignar rol",
               description = "Solo ADMIN. Asigna rol USER o ADMIN al usuario indicado.")
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                         @RequestParam String role,
                                         @RequestParam Long adminId) {
        Role parsedRole;
        try {
            parsedRole = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Rol no válido. Use USER o ADMIN", "success", false));
        }

        int status = userService.updateRole(id, parsedRole);

        if (status == 0) {
            auditService.logUserRoleUpdated(id, adminId, parsedRole.name());
        }

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Rol actualizado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al actualizar el rol", "success", false));
        };
    }

    /**
     * Cuenta el total de usuarios registrados (HU20 — Ver información del sistema).
     *
     * @return 202 Accepted con el conteo; 204 si no hay usuarios.
     */
    @GetMapping("/count")
    @Operation(summary = "Contar usuarios",
               description = "Retorna el número total de usuarios registrados en la plataforma.")
    public ResponseEntity<Long> countAll() {
        long count = userService.count();
        if (count == 0) {
            return new ResponseEntity<>(count, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(count, HttpStatus.ACCEPTED);
    }
}