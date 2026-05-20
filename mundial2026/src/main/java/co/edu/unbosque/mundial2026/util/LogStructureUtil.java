/**
 * Paquete que contiene las clases de utilidad de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utilidad de logging estructurado para la plataforma Mundial 2026 Hub.
 * <p>
 * Genera logs en formato JSON estructurado con campos estandarizados que
 * facilitan la indexación automática en Splunk o ElasticSearch. Todos los
 * eventos relevantes del sistema (autenticación, operaciones de negocio,
 * errores, llamadas externas) deben registrarse a través de esta clase
 * para garantizar la trazabilidad requerida por el proyecto.
 * </p>
 *
 * <h3>Campos estándar en cada log</h3>
 * <ul>
 *   <li>{@code timestamp} — Fecha y hora UTC en formato ISO 8601.</li>
 *   <li>{@code level} — Nivel del log: INFO, WARN o ERROR.</li>
 *   <li>{@code service} — Nombre del servicio que generó el log.</li>
 *   <li>{@code action} — Acción o evento específico.</li>
 *   <li>{@code userId} — ID del usuario involucrado (puede ser null).</li>
 *   <li>{@code correlationId} — ID de correlación de la operación (puede ser null).</li>
 *   <li>{@code result} — Resultado: SUCCESS, FAILURE o BLOCKED.</li>
 *   <li>{@code message} — Descripción legible del evento.</li>
 *   <li>{@code extra} — Datos adicionales en formato clave=valor (opcional).</li>
 * </ul>
 *
 * <h3>Ejemplo de log generado</h3>
 * <pre>
 * {"timestamp":"2026-06-15T20:00:00Z","level":"INFO","service":"TicketService",
 *  "action":"TICKET_RESERVED","userId":42,"correlationId":"a1b2c3d4",
 *  "result":"SUCCESS","message":"Entrada reservada para partido 7"}
 * </pre>
 */
@Component
public class LogStructureUtil {

    /**
     * Logger SLF4J raíz para todos los logs estructurados de la plataforma.
     * El nombre "mundial2026" permite filtrar todos los logs del sistema
     * en Splunk/Elasticsearch con un solo término de búsqueda.
     */
    private static final Logger logger = LoggerFactory.getLogger("mundial2026");

    /**
     * Nombre de la aplicación incluido en cada log para identificación en
     * entornos con múltiples servicios.
     */
    private static final String APP_NAME = "mundial2026-hub";

    /**
     * Formateador de fechas para los timestamps de los logs en UTC.
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Constructor por defecto requerido por Spring.
     */
    public LogStructureUtil() {
    }

    // =========================================================================
    // Métodos de log por nivel
    // =========================================================================

    /**
     * Registra un evento informativo exitoso.
     * Se usa para operaciones completadas correctamente: reservas, pagos,
     * apertura de paquetes, envío de notificaciones, etc.
     *
     * @param service       El nombre de la clase o servicio que emite el log.
     * @param action        La acción realizada (ej. "TICKET_RESERVED", "LOGIN").
     * @param userId        El ID del usuario involucrado. Puede ser {@code null}.
     * @param correlationId El ID de correlación de la operación. Puede ser {@code null}.
     * @param message       Descripción legible del evento.
     * @param extra         Pares clave=valor adicionales separados por coma
     *                      (ej. "matchId=7,category=CATEGORY_1"). Puede ser {@code null}.
     */
    public void logInfo(String service, String action, Long userId,
                        String correlationId, String message, String extra) {
        logger.info(buildJson("INFO", service, action, userId, correlationId,
                "SUCCESS", message, extra));
    }

    /**
     * Registra un evento de advertencia o resultado inesperado pero no crítico.
     * Se usa para operaciones que completaron con validación fallida, datos
     * no encontrados, o comportamiento anómalo que no interrumpe el flujo.
     *
     * @param service       El nombre del servicio.
     * @param action        La acción realizada.
     * @param userId        El ID del usuario. Puede ser {@code null}.
     * @param correlationId El ID de correlación. Puede ser {@code null}.
     * @param message       Descripción del evento de advertencia.
     * @param extra         Datos adicionales. Puede ser {@code null}.
     */
    public void logWarn(String service, String action, Long userId,
                        String correlationId, String message, String extra) {
        logger.warn(buildJson("WARN", service, action, userId, correlationId,
                "FAILURE", message, extra));
    }

    /**
     * Registra un error del sistema: excepción no controlada, fallo de API
     * externa, error de base de datos, etc.
     *
     * @param service       El nombre del servicio.
     * @param action        La acción que falló.
     * @param userId        El ID del usuario. Puede ser {@code null}.
     * @param correlationId El ID de correlación. Puede ser {@code null}.
     * @param message       Descripción del error.
     * @param errorDetail   Detalle técnico del error (mensaje de excepción).
     */
    public void logError(String service, String action, Long userId,
                         String correlationId, String message, String errorDetail) {
        logger.error(buildJson("ERROR", service, action, userId, correlationId,
                "FAILURE", message, "errorDetail=" + sanitize(errorDetail)));
    }

    /**
     * Registra un evento de seguridad bloqueado: intento de login fallido
     * múltiple, patrón antifraude detectado, acceso no autorizado a recurso.
     *
     * @param service       El nombre del servicio.
     * @param action        La acción que fue bloqueada.
     * @param userId        El ID del usuario. Puede ser {@code null}.
     * @param correlationId El ID de correlación. Puede ser {@code null}.
     * @param message       Descripción del evento de seguridad.
     * @param sourceIp      La IP de origen de la petición. Puede ser {@code null}.
     */
    public void logSecurity(String service, String action, Long userId,
                             String correlationId, String message, String sourceIp) {
        String extra = sourceIp != null ? "sourceIp=" + sourceIp : null;
        logger.warn(buildJson("WARN", service, action, userId, correlationId,
                "BLOCKED", message, extra));
    }

    /**
     * Registra una llamada a la API externa de datos deportivos.
     * Se usa para monitorear la disponibilidad del proveedor y detectar
     * cuándo el sistema debe activar el modo de degradación elegante.
     *
     * @param endpoint     La URL del endpoint llamado.
     * @param statusCode   El código de respuesta HTTP recibido.
     * @param durationMs   El tiempo de respuesta en milisegundos.
     * @param success      {@code true} si la llamada fue exitosa.
     */
    public void logExternalApiCall(String endpoint, int statusCode,
                                    long durationMs, boolean success) {
        String action = "EXTERNAL_API_CALL";
        String result = success ? "SUCCESS" : "FAILURE";
        String extra = "endpoint=" + sanitize(endpoint)
                + ",statusCode=" + statusCode
                + ",durationMs=" + durationMs;
        String json = buildJson("INFO", "ExternalMatchService", action,
                null, null, result,
                "Llamada a API externa completada", extra);
        if (success) {
            logger.info(json);
        } else {
            logger.warn(json);
        }
    }

    // =========================================================================
    // Helpers de construcción de JSON
    // =========================================================================

    /**
     * Construye el JSON estructurado del log con todos los campos estándar.
     * Los campos con valor {@code null} se omiten para no contaminar el índice
     * de Splunk/Elasticsearch con campos vacíos innecesarios.
     *
     * @param level         Nivel del log: INFO, WARN o ERROR.
     * @param service       Nombre del servicio.
     * @param action        Acción realizada.
     * @param userId        ID del usuario (puede ser null).
     * @param correlationId ID de correlación (puede ser null).
     * @param result        Resultado: SUCCESS, FAILURE o BLOCKED.
     * @param message       Descripción del evento.
     * @param extra         Datos adicionales en formato clave=valor (puede ser null).
     * @return La cadena JSON lista para ser escrita por el logger.
     */
    private String buildJson(String level, String service, String action,
                              Long userId, String correlationId, String result,
                              String message, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"timestamp\":\"").append(nowUtcString()).append("\",");
        sb.append("\"app\":\"").append(APP_NAME).append("\",");
        sb.append("\"level\":\"").append(level).append("\",");
        sb.append("\"service\":\"").append(sanitize(service)).append("\",");
        sb.append("\"action\":\"").append(sanitize(action)).append("\",");

        if (userId != null) {
            sb.append("\"userId\":").append(userId).append(",");
        }
        if (correlationId != null && !correlationId.isEmpty()) {
            sb.append("\"correlationId\":\"").append(sanitize(correlationId)).append("\",");
        }

        sb.append("\"result\":\"").append(result).append("\",");
        sb.append("\"message\":\"").append(sanitize(message)).append("\"");

        if (extra != null && !extra.isEmpty()) {
            sb.append(",\"extra\":\"").append(sanitize(extra)).append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Retorna la marca de tiempo actual en UTC formateada para los logs.
     *
     * @return La fecha y hora UTC en formato "yyyy-MM-dd'T'HH:mm:ss'Z'".
     */
    private String nowUtcString() {
        return LocalDateTime.now(ZoneId.of("UTC")).format(TIMESTAMP_FORMATTER);
    }

    /**
     * Sanitiza un valor de cadena para incluirlo de forma segura en el JSON
     * del log. Escapa comillas dobles y elimina saltos de línea que podrían
     * romper el formato JSON y confundir al indexador de Splunk/Elasticsearch.
     *
     * @param value La cadena a sanitizar.
     * @return La cadena sanitizada, o una cadena vacía si el valor es {@code null}.
     */
    private String sanitize(String value) {
        if (value == null) return "";
        return value.replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }
}