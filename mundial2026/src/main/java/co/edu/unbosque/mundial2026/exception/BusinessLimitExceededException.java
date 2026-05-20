/**
 * Paquete que contiene las clases para el manejo de excepciones y validaciones
 * específicas de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.exception;

/**
 * Excepción lanzada cuando un usuario supera un límite de negocio
 * parametrizable configurado para prevenir abuso o comportamiento anómalo
 * en la plataforma Mundial 2026 Hub.
 * <p>
 * Casos de uso:
 * <ul>
 *   <li>Usuario supera el límite de entradas activas simultáneas
 *       ({@code TicketService#MAX_ACTIVE_TICKETS = 4}).</li>
 *   <li>Usuario supera el límite de intercambios de láminas activos
 *       ({@code AlbumService#MAX_ACTIVE_EXCHANGES = 5}).</li>
 *   <li>Intento de canjear un código promocional que ya fue usado
 *       por el mismo u otro usuario.</li>
 * </ul>
 * </p>
 * <p>
 * El {@link GlobalExceptionHandler} mapea esta excepción a una respuesta
 * HTTP 409 Conflict, indicando que el límite de negocio fue violado.
 * El evento también se registra en auditoría como posible patrón de fraude.
 * </p>
 */
public class BusinessLimitExceededException extends RuntimeException {

    /**
     * Identificador de serialización.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construye la excepción con un mensaje descriptivo del límite superado.
     *
     * @param message Descripción del límite de negocio que fue superado.
     */
    public BusinessLimitExceededException(String message) {
        super(message);
    }

    /**
     * Construye la excepción con un mensaje descriptivo y la causa original.
     *
     * @param message Descripción del límite superado.
     * @param cause   La excepción original que causó este error.
     */
    public BusinessLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}