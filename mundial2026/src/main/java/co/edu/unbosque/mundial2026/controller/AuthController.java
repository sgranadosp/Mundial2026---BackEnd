/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.UserDTO;
import co.edu.unbosque.mundial2026.model.User;
//import co.edu.unbosque.mundial2026.security.JwtUtil;
import co.edu.unbosque.mundial2026.service.AuditEventService;
import co.edu.unbosque.mundial2026.service.UserService;
import co.edu.unbosque.mundial2026.util.AESUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para autenticación de usuarios en la plataforma
 * Mundial 2026 Hub.
 * Expone los endpoints de registro, inicio de sesión y recuperación de
 * contraseña. Estos endpoints son públicos (no requieren JWT). El patrón
 * sigue exactamente la implementación del proyecto VirusDetected:
 * {@link AuthenticationManager} para autenticar, {@link JwtUtil} para
 * generar el token y {@link UserService} para la lógica de negocio.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Tag(name = "Autenticación", description = "Endpoints públicos de registro, login y recuperación de contraseña")
public class AuthController {

    /**
     * Gestor de autenticación de Spring Security.
     */
    //private final AuthenticationManager authenticationManager;

    /**
     * Utilidad para generación y validación de tokens JWT.
     */
   // private final JwtUtil jwtUtil;

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
     * Constructor con inyección de dependencias para los beans de seguridad.
     *
     * @param authenticationManager El gestor de autenticación de Spring Security.
     * @param jwtUtil               La utilidad JWT.
     * @param userService           El servicio de usuarios.
     */
    /*
    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }
    */

    // =========================================================================
    // Endpoints públicos
    // =========================================================================

    /**
     * Registra un nuevo usuario en el sistema con rol USER por defecto.
     * <p>
     * Valida unicidad de username/email, política de contraseña y formato
     * de email antes de persistir. Al registrarse, el sistema crea automáticamente
     * el álbum digital del usuario.
     *
     * @param registerRequest El DTO con nombre, username, password y email.
     * @return 201 Created si el registro fue exitoso; 409 Conflict si el
     *         username/email ya existen o la contraseña/email no son válidos;
     *         400 Bad Request en caso de error genérico.
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario",
               description = "Crea una cuenta de usuario con rol USER. Valida unicidad y política de contraseña.")
    public ResponseEntity<?> register(@RequestBody UserDTO registerRequest) {
        int result = userService.create(registerRequest);

        return switch (result) {
            case 0 -> {
                auditService.logUserRegistered(
                        userService.getByUsername(registerRequest.getUsername()) != null
                                ? userService.getByUsername(registerRequest.getUsername()).getId()
                                : null);
                yield ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Usuario registrado exitosamente", "success", true));
            }
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El username o email ya están en uso", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "La contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula, número y símbolo",
                            "success", false));
            case 5 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El formato del correo electrónico no es válido", "success", false));
            case 6 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Los campos no pueden contener caracteres HTML (< > :)", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al registrar el usuario", "success", false));
        };
    }

    /**
     * Inicia sesión de un usuario existente y retorna un token JWT.
     * <p>
     * Autentica las credenciales con {@link AuthenticationManager}, genera un
     * JWT con {@link JwtUtil} y retorna el token junto con el rol del usuario.
     * Las credenciales se encriptan con AES antes de buscar en la base de datos.
     *
     * @param loginRequest El DTO con username y password.
     * @return 200 OK con el token JWT y el rol; 401 Unauthorized si las
     *         credenciales son incorrectas.
     */
    
    /*
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
               description = "Autentica usuario y retorna token JWT + rol.")
    public ResponseEntity<?> login(@RequestBody UserDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            AESUtil.encrypt(loginRequest.getUsername()),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
       //     String jwt = jwtUtil.generateToken(userDetails, AESUtil.encrypt(loginRequest.getUsername()));

            String role = null;
            Long userId = null;
            if (userDetails instanceof User user) {
                role = user.getRole().name();
                userId = user.getId();
            }

            auditService.logLogin(userId, null);
      //      return ResponseEntity.ok(new AuthResponse(jwt, role));

        } catch (AuthenticationException e) {
            auditService.logLoginFailed(null, null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas", "success", false));
        }
    }
*/
    /**
     * Actualiza la contraseña de un usuario verificado (flujo de recuperación).
     * <p>
     * El frontend primero envía el código de verificación al correo del usuario;
     * una vez validado, llama a este endpoint con el nuevo valor.
     *
     * @param username    El username del usuario (sin encriptar).
     * @param newPassword La nueva contraseña que reemplazará a la actual.
     * @return 202 Accepted si fue actualizada; 404 Not Found si el usuario
     *         no existe; 409 Conflict si la contraseña no cumple la política.
     */
    @PutMapping("/rememberPassword")
    @Operation(summary = "Recuperar contraseña",
               description = "Actualiza la contraseña de un usuario tras verificación por correo.")
    public ResponseEntity<?> rememberPassword(@RequestParam String username,
                                               @RequestParam String newPassword) {
        int status = userService.updatePassword(username, newPassword);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Contraseña actualizada exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "La contraseña no cumple la política de seguridad",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al actualizar la contraseña", "success", false));
        };
    }

    // =========================================================================
    // Clase interna de respuesta de autenticación
    // =========================================================================

    /**
     * Clase interna que encapsula la respuesta del login con el JWT y el rol.
     */
    private static class AuthResponse {

        /** Token JWT generado para el usuario autenticado. */
        private final String token;

        /** Rol del usuario autenticado (USER o ADMIN). */
        private final String role;

        /**
         * Constructor de la respuesta de autenticación.
         *
         * @param token El JWT generado.
         * @param role  El rol del usuario.
         */
        public AuthResponse(String token, String role) {
            this.token = token;
            this.role = role;
        }

        /**
         * Retorna el token JWT.
         *
         * @return El token JWT.
         */
        public String getToken() { return token; }

        /**
         * Retorna el rol del usuario.
         *
         * @return El rol del usuario.
         */
        public String getRole() { return role; }
    }
}