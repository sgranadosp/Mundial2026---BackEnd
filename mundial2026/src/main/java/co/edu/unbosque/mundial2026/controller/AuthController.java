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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.UserDTO;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.security.JwtUtil;
import co.edu.unbosque.mundial2026.service.AuditEventService;
import co.edu.unbosque.mundial2026.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para autenticación de usuarios en la plataforma
 * Mundial 2026 Hub.
 * <p>
 * Expone los endpoints públicos (sin requerir JWT) para:
 * <ul>
 *   <li>Registro de nuevo usuario ({@code POST /auth/register}).</li>
 *   <li>Inicio de sesión por username o email ({@code POST /auth/login}).</li>
 *   <li>Verificación de cuenta tras registro ({@code POST /auth/verifyCode}).</li>
 *   <li>Solicitud de código de recuperación
 *       ({@code POST /auth/requestRecoveryCode}).</li>
 *   <li>Validación intermedia del código de recuperación
 *       ({@code POST /auth/validateRecoveryCode}).</li>
 *   <li>Reset final de contraseña con código
 *       ({@code PUT /auth/resetPassword}).</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000", "http://localhost:5173" })
@Transactional
@Tag(name = "Autenticación", description = "Endpoints públicos de registro, login y recuperación de contraseña")
public class AuthController {

    /**
     * Gestor de autenticación de Spring Security.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Utilidad para generación y validación de tokens JWT.
     */
    private final JwtUtil jwtUtil;

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
     */
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    // =========================================================================
    // Registro
    // =========================================================================

    /**
     * Registra un nuevo usuario en el sistema con rol USER por defecto.
     * <p>
     * Valida unicidad de username/email, política de contraseña y formato
     * de email antes de persistir. Si el registro tiene éxito, el sistema
     * genera un código de 6 dígitos, lo guarda encriptado en
     * {@code verificationCode} y lo envía por correo. La cuenta queda con
     * {@code enabled = false} hasta que el usuario verifique el código.
     * </p>
     *
     * @param registerRequest El DTO con name, username, password y email.
     * @return 201 Created si el registro fue exitoso; 409 Conflict si el
     *         username/email ya existen o la contraseña/email no son válidos;
     *         400 Bad Request si faltan campos.
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario",
               description = "Crea una cuenta de usuario con rol USER y envía código de verificación al correo.")
    public ResponseEntity<?> register(@RequestBody UserDTO registerRequest) {
        int result = userService.create(registerRequest);

        return switch (result) {
            case 0 -> {
                UserDTO created = userService.getByUsername(registerRequest.getUsername());
                if (created != null) {
                    auditService.logUserRegistered(created.getId());
                }
                yield ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message",
                                "Usuario registrado exitosamente. Revisa tu correo para verificar la cuenta.",
                                "success", true,
                                "email", registerRequest.getEmail()));
            }
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El username o email ya están en uso", "success", false));
            case 3 -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Faltan campos requeridos", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "La contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula, número y símbolo",
                            "success", false));
            case 5 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El formato del correo electrónico no es válido", "success", false));
            case 6 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Los campos no pueden contener caracteres HTML (< > :)",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al registrar el usuario", "success", false));
        };
    }

    // =========================================================================
    // Login (acepta username O email)
    // =========================================================================

    /**
     * Inicia sesión de un usuario existente y retorna un token JWT.
     * <p>
     * Acepta como identificador tanto el {@code username} como el
     * {@code email}. El servicio resuelve cuál de los dos coincide en BD
     * y obtiene el username encriptado correspondiente, que es lo que
     * Spring Security necesita para autenticar.
     * </p>
     *
     * @param loginRequest Mapa con las claves {@code identifier} (username o
     *                     email en texto plano) y {@code password}.
     * @return 200 OK con el token JWT, el rol, el id y el correo del usuario;
     *         401 Unauthorized si las credenciales son incorrectas;
     *         400 Bad Request si faltan campos.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
               description = "Autentica al usuario por username o email y retorna token JWT + rol.")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String identifier = loginRequest.get("identifier");
        String password = loginRequest.get("password");

        if (identifier == null || identifier.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Faltan campos: identifier o password", "success", false));
        }

        // Resuelve si el identifier es un username o un email y obtiene
        // el username encriptado real con el que Spring Security debe
        // autenticar contra el UserDetailsService.
        String encryptedUsername = userService.resolveIdentifierToEncryptedUsername(identifier);
        if (encryptedUsername == null) {
            auditService.logLoginFailed(null, null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas", "success", false));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(encryptedUsername, password));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(userDetails, encryptedUsername);

            String role = null;
            Long userId = null;
            String correo = null;
            String nombre = null;
            String usernamePlano = null;
            if (userDetails instanceof User user) {
                role = "ROLE_" + user.getRole().name();
                userId = user.getId();
                // Los campos sensibles están encriptados en la entidad;
                // se desencriptan vía el DTO del servicio.
                UserDTO dto = userService.getById(user.getId());
                if (dto != null) {
                    correo = dto.getEmail();
                    nombre = dto.getName();
                    usernamePlano = dto.getUsername();
                }
            }

            auditService.logLogin(userId, null);
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "tipo", "Bearer",
                    "rol", role == null ? "" : role,
                    "id", userId == null ? -1 : userId,
                    "correo", correo == null ? "" : correo,
                    "username", usernamePlano == null ? "" : usernamePlano,
                    "nombre", nombre == null ? "" : nombre));

        } catch (AuthenticationException e) {
            auditService.logLoginFailed(null, null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas", "success", false));
        }
    }

    // =========================================================================
    // Verificación de cuenta tras registro
    // =========================================================================

    /**
     * Valida el código de 6 dígitos que llegó al correo del usuario tras
     * registrarse. Si el código es correcto, activa la cuenta
     * ({@code enabled = true}) y consume el código.
     *
     * @param body Mapa con las claves {@code email} y {@code codigo}.
     * @return 200 OK si el código fue válido y la cuenta se activó;
     *         401 Unauthorized si el código es incorrecto;
     *         404 Not Found si no existe el correo;
     *         400 Bad Request si faltan campos.
     */
    @PostMapping("/verifyCode")
    @Operation(summary = "Verificar código de registro",
               description = "Valida el código de 6 dígitos enviado al correo durante el registro y activa la cuenta.")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");
        if (email == null || codigo == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Faltan campos: email o codigo", "success", false));
        }
        int status = userService.verifyRegistrationCode(email, codigo);
        return switch (status) {
            case 0 -> ResponseEntity.ok(Map.of("message", "Cuenta verificada correctamente",
                    "success", true));
            case 7 -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Código de verificación incorrecto", "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No existe ninguna cuenta con ese correo", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error verificando el código", "success", false));
        };
    }

    // =========================================================================
    // Recuperación de contraseña (3 pasos)
    // =========================================================================

    /**
     * Paso 1: solicita el envío de un código de recuperación al correo del
     * usuario. Si el correo está registrado, genera un código de 6 dígitos,
     * lo persiste encriptado y lo envía por SMTP.
     * <p>
     * Por seguridad, la respuesta es genérica: no revela si el correo existe
     * o no en la BD para impedir enumeración de cuentas registradas.
     * </p>
     *
     * @param body Mapa con la clave {@code email}.
     * @return 200 OK con mensaje genérico siempre que la solicitud esté
     *         bien formada; 400 Bad Request si falta el campo {@code email}.
     */
    @PostMapping("/requestRecoveryCode")
    @Operation(summary = "Solicitar código de recuperación de contraseña",
               description = "Envía un código de 6 dígitos al correo del usuario para iniciar el flujo de recuperación.")
    public ResponseEntity<?> requestRecoveryCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Falta campo: email", "success", false));
        }
        // Disparamos el servicio pero no exponemos el resultado real al
        // cliente para no revelar qué correos están registrados.
        userService.requestRecoveryCode(email);
        return ResponseEntity.ok(Map.of(
                "message", "Si el correo está registrado, recibirás un código de recuperación.",
                "success", true));
    }

    /**
     * Paso 2: valida el código que el usuario ingresó pero NO lo consume.
     * Solo confirma que el código coincide con el último que se envió a ese
     * correo. El consumo del código ocurre en el paso 3 al actualizar la
     * contraseña.
     *
     * @param body Mapa con las claves {@code email} y {@code codigo}.
     * @return 200 OK si el código es válido;
     *         401 Unauthorized si el código no coincide;
     *         404 Not Found si no existe el correo;
     *         400 Bad Request si faltan campos.
     */
    @PostMapping("/validateRecoveryCode")
    @Operation(summary = "Validar código de recuperación (sin consumir)",
               description = "Comprueba que el código de 6 dígitos coincide con el que se envió, sin invalidarlo.")
    public ResponseEntity<?> validateRecoveryCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");
        if (email == null || codigo == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Faltan campos: email o codigo", "success", false));
        }
        int status = userService.validateRecoveryCode(email, codigo);
        return switch (status) {
            case 0 -> ResponseEntity.ok(Map.of("message", "Código válido", "success", true));
            case 7 -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Código de recuperación incorrecto", "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No existe ninguna cuenta con ese correo", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error validando el código", "success", false));
        };
    }

    /**
     * Paso 3: actualiza la contraseña del usuario validando una vez más el
     * código y consumiéndolo después del cambio exitoso.
     *
     * @param body Mapa con las claves {@code email}, {@code codigo} y
     *             {@code nuevaContrasena}.
     * @return 202 Accepted si la contraseña se actualizó;
     *         401 Unauthorized si el código no coincide;
     *         404 Not Found si no existe el correo;
     *         409 Conflict si la nueva contraseña no cumple la política;
     *         400 Bad Request si faltan campos.
     */
    @PutMapping("/resetPassword")
    @Operation(summary = "Restablecer contraseña con código de recuperación",
               description = "Valida el código y actualiza la contraseña del usuario; consume el código tras éxito.")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");
        String nueva = body.get("nuevaContrasena");
        if (email == null || codigo == null || nueva == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Faltan campos: email, codigo o nuevaContrasena",
                            "success", false));
        }
        int status = userService.resetPasswordWithCode(email, codigo, nueva);
        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Contraseña actualizada exitosamente", "success", true));
            case 7 -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Código de recuperación incorrecto", "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No existe ninguna cuenta con ese correo", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "La contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula, número y símbolo",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al actualizar la contraseña", "success", false));
        };
    }

    // =========================================================================
    // Endpoint legacy (mantenido para compatibilidad)
    // =========================================================================

    /**
     * Endpoint heredado para actualizar contraseña con username + nueva
     * contraseña, sin pasar por el flujo de código. Se mantiene como utilidad
     * interna pero no es el flujo recomendado para producción.
     *
     * @param username    El username del usuario (sin encriptar).
     * @param newPassword La nueva contraseña.
     * @return Respuesta estándar según el código de retorno del servicio.
     */
    @PutMapping("/rememberPassword")
    @Operation(summary = "[Legacy] Cambiar contraseña por username",
               description = "Cambia la contraseña sin código de verificación. Usar /resetPassword en su lugar.")
    public ResponseEntity<?> rememberPassword(@RequestParam String username,
                                              @RequestParam String newPassword) {
        int status = userService.updatePassword(username, newPassword);
        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Contraseña actualizada exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "La contraseña no cumple la política de seguridad",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al actualizar la contraseña", "success", false));
        };
    }
}