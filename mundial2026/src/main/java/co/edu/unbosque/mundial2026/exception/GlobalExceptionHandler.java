/**
 * Paquete que contiene las clases para el manejo de excepciones y validaciones
 * específicas de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

/**
 * Manejador global de excepciones para la plataforma Mundial 2026 Hub.
 * <p>
 * Usa {@link RestControllerAdvice} para interceptar excepciones no controladas
 * que escapen de los controladores y retornar respuestas JSON estandarizadas
 * con el mismo formato {@code {message, success, timestamp}} que usan todos
 * los controladores del proyecto. Evita que Spring retorne páginas de error
 * HTML o stacktraces en producción.
 * </p>
 * <p>
 * Cubre tres categorías de excepciones:
 * <ul>
 *   <li>Excepciones de seguridad JWT y Spring Security.</li>
 *   <li>Excepciones de validación y parámetros de request.</li>
 *   <li>Excepciones de negocio personalizadas del dominio.</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // Excepciones JWT
    // =========================================================================

    /**
     * Maneja tokens JWT expirados. Ocurre cuando el usuario intenta usar un
     * token que superó su tiempo de validez (8 horas).
     *
     * @param ex La excepción de token expirado.
     * @return 401 Unauthorized con mensaje descriptivo.
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredJwtException(ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError("El token JWT ha expirado. Inicia sesión nuevamente.", false));
    }

    /**
     * Maneja tokens JWT con formato inválido o corruptos.
     *
     * @param ex La excepción de JWT malformado.
     * @return 401 Unauthorized con mensaje descriptivo.
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<?> handleMalformedJwtException(MalformedJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError("Token JWT inválido o malformado.", false));
    }

    /**
     * Maneja tokens JWT con firma inválida (posible manipulación del token).
     *
     * @param ex La excepción de firma inválida.
     * @return 401 Unauthorized con mensaje descriptivo.
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<?> handleSignatureException(SignatureException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError("La firma del token JWT no es válida.", false));
    }

    // =========================================================================
    // Excepciones de Spring Security
    // =========================================================================

    /**
     * Maneja intentos de acceso a recursos sin permisos suficientes.
     * Se lanza cuando un usuario con rol USER intenta acceder a un endpoint
     * protegido solo para ADMIN.
     *
     * @param ex La excepción de acceso denegado.
     * @return 403 Forbidden con mensaje descriptivo.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildError("Acceso denegado. No tienes permisos para esta operación.", false));
    }

    /**
     * Maneja credenciales incorrectas durante el login.
     *
     * @param ex La excepción de credenciales incorrectas.
     * @return 401 Unauthorized con mensaje descriptivo.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError("Credenciales incorrectas. Verifica tu usuario y contraseña.", false));
    }

    /**
     * Maneja el caso en que el usuario no se encuentra durante la autenticación.
     *
     * @param ex La excepción de usuario no encontrado.
     * @return 404 Not Found con mensaje descriptivo.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("Usuario no encontrado en el sistema.", false));
    }

    // =========================================================================
    // Excepciones de validación y parámetros
    // =========================================================================

    /**
     * Maneja parámetros de request obligatorios que no fueron enviados.
     * Por ejemplo, cuando un endpoint requiere {@code @RequestParam String userId}
     * y el cliente no lo envía.
     *
     * @param ex La excepción de parámetro faltante.
     * @return 400 Bad Request con el nombre del parámetro faltante.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(
                        "Parámetro requerido faltante: '" + ex.getParameterName() + "'.", false));
    }

    /**
     * Maneja errores de tipo en parámetros de request.
     * Por ejemplo, cuando se espera un {@code Long} y se recibe una cadena de texto.
     *
     * @param ex La excepción de tipo incorrecto en argumento de método.
     * @return 400 Bad Request con descripción del error de tipo.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Valor inválido para el parámetro '" + ex.getName() + "'. "
                + "Se esperaba: " + (ex.getRequiredType() != null
                        ? ex.getRequiredType().getSimpleName() : "tipo desconocido") + ".";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildError(msg, false));
    }

    /**
     * Maneja argumentos ilegales lanzados desde los servicios cuando los datos
     * de entrada no cumplen las precondiciones de negocio.
     *
     * @param ex La excepción de argumento ilegal.
     * @return 400 Bad Request con el mensaje de la excepción.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(ex.getMessage(), false));
    }

    // =========================================================================
    // Excepciones de negocio personalizadas
    // =========================================================================

    /**
     * Maneja la excepción cuando una operación no está permitida en el estado
     * actual de una entidad (ej. editar un pronóstico bloqueado, pagar una
     * entrada expirada).
     *
     * @param ex La excepción de operación no permitida.
     * @return 403 Forbidden con el mensaje de la excepción.
     */
    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<?> handleOperationNotAllowed(OperationNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildError(ex.getMessage(), false));
    }

    /**
     * Maneja la excepción cuando se detecta un patrón de comportamiento anómalo
     * que activa el módulo antifraude (ej. superar el límite de entradas activas
     * o de intercambios simultáneos).
     *
     * @param ex La excepción de límite de negocio superado.
     * @return 409 Conflict con el mensaje de la excepción.
     */
    @ExceptionHandler(BusinessLimitExceededException.class)
    public ResponseEntity<?> handleBusinessLimitExceeded(BusinessLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError(ex.getMessage(), false));
    }

    /**
     * Maneja la excepción cuando la API externa de datos deportivos no responde
     * o retorna un error. El sistema aplica degradación elegante: muestra el
     * último dato confirmado y registra el evento.
     *
     * @param ex La excepción de fallo de API externa.
     * @return 503 Service Unavailable con mensaje descriptivo.
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<?> handleExternalApiException(ExternalApiException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildError(
                        "La fuente de datos externa no está disponible. "
                        + "Se muestran los últimos datos confirmados.", false));
    }

    // =========================================================================
    // Manejador de última instancia
    // =========================================================================

    /**
     * Captura cualquier excepción no controlada que no haya sido manejada por
     * los métodos anteriores. Evita que Spring retorne páginas de error HTML
     * o stacktraces al cliente en producción.
     *
     * @param ex La excepción genérica no controlada.
     * @return 500 Internal Server Error con mensaje genérico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        "Error interno del servidor. Por favor intenta de nuevo.", false));
    }

    // =========================================================================
    // Helper de respuesta de error
    // =========================================================================

    /**
     * Construye el mapa de respuesta de error estandarizado con los campos
     * {@code message}, {@code success} y {@code timestamp}, consistente con
     * el formato usado en todos los controladores del proyecto.
     *
     * @param message El mensaje de error descriptivo.
     * @param success Siempre {@code false} para respuestas de error.
     * @return Un mapa con las claves {@code message}, {@code success} y
     *         {@code timestamp}.
     */
    private Map<String, Object> buildError(String message, boolean success) {
        return Map.of(
                "message", message,
                "success", success,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}