/**
 * Paquete que contiene las clases para el manejo de excepciones y validaciones
 * específicas de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.exception;

/**
 * Excepción lanzada cuando la API externa de datos deportivos no responde
 * correctamente o retorna un error que impide sincronizar los datos del torneo.
 * <p>
 * Casos de uso:
 * <ul>
 *   <li>La API de football-data.org, API-Football o WireMock retorna un
 *       código de error (4xx o 5xx).</li>
 *   <li>La conexión con el proveedor externo supera el timeout de 10 segundos.</li>
 *   <li>La respuesta JSON de la API externa tiene un formato inesperado o
 *       campos faltantes que impiden el parseo.</li>
 *   <li>Se supera el límite de cuota del plan gratuito del proveedor.</li>
 * </ul>
 * </p>
 * <p>
 * Al capturar esta excepción, el {@link GlobalExceptionHandler} retorna una
 * respuesta HTTP 503 Service Unavailable e indica al cliente que se están
 * mostrando los últimos datos confirmados (degradación elegante requerida
 * por el enunciado del proyecto). El servicio también marca los partidos
 * afectados con {@code DataStatus.PENDING_UPDATE} en la base de datos.
 * </p>
 */
public class ExternalApiException extends RuntimeException {

    /**
     * Identificador de serialización.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Código de respuesta HTTP retornado por la API externa, si aplica.
     * Es -1 si el fallo ocurrió antes de recibir respuesta (timeout, error de red).
     */
    private final int httpStatusCode;

    /**
     * URL del endpoint externo que falló.
     */
    private final String endpoint;

    /**
     * Construye la excepción con un mensaje descriptivo del fallo.
     *
     * @param message Descripción del fallo en la API externa.
     */
    public ExternalApiException(String message) {
        super(message);
        this.httpStatusCode = -1;
        this.endpoint = null;
    }

    /**
     * Construye la excepción con detalles completos del fallo.
     *
     * @param message        Descripción del fallo.
     * @param httpStatusCode Código HTTP retornado por la API externa (-1 si
     *                       no hubo respuesta).
     * @param endpoint       URL del endpoint que falló.
     */
    public ExternalApiException(String message, int httpStatusCode, String endpoint) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.endpoint = endpoint;
    }

    /**
     * Construye la excepción con un mensaje y la causa original.
     *
     * @param message Descripción del fallo.
     * @param cause   La excepción original ({@code IOException},
     *                {@code InterruptedException}, etc.).
     */
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = -1;
        this.endpoint = null;
    }

    /**
     * Retorna el código de respuesta HTTP de la API externa.
     *
     * @return El código HTTP, o -1 si el fallo fue de red/timeout.
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Retorna la URL del endpoint externo que falló.
     *
     * @return La URL del endpoint, o {@code null} si no se especificó.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Representación en cadena con los detalles del fallo de API externa.
     *
     * @return Cadena descriptiva del error con endpoint y código HTTP.
     */
    @Override
    public String toString() {
        return "ExternalApiException [httpStatusCode=" + httpStatusCode
                + ", endpoint=" + endpoint
                + ", message=" + getMessage() + "]";
    }
}