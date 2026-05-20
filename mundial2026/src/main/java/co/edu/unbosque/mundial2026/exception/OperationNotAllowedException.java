/**
 * Paquete que contiene las clases para el manejo de excepciones y validaciones
 * específicas de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.exception;

/**
 * Excepción lanzada cuando se intenta realizar una operación que no está
 * permitida en el estado actual de una entidad del dominio.
 * <p>
 * Casos de uso:
 * <ul>
 *   <li>Editar un pronóstico con estado {@code LOCKED} o {@code EVALUATED}.</li>
 *   <li>Pagar una entrada cuya reserva ya expiró (estado {@code EXPIRED}).</li>
 *   <li>Transferir una entrada que no está en estado {@code PAID}.</li>
 *   <li>Desactivar un grupo de polla siendo un miembro que no es el creador.</li>
 *   <li>Intentar abrir un paquete ya abierto (estado {@code OPENED}).</li>
 * </ul>
 * </p>
 * <p>
 * El {@link GlobalExceptionHandler} mapea esta excepción a una respuesta
 * HTTP 403 Forbidden con el mensaje de la excepción.
 * </p>
 */
public class OperationNotAllowedException extends RuntimeException {

    /**
     * Identificador de serialización.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construye la excepción con un mensaje descriptivo del motivo por el que
     * la operación no está permitida en el estado actual.
     *
     * @param message Descripción del estado que impide la operación.
     */
    public OperationNotAllowedException(String message) {
        super(message);
    }

    /**
     * Construye la excepción con un mensaje descriptivo y la causa original.
     *
     * @param message Descripción del estado que impide la operación.
     * @param cause   La excepción original que causó este error.
     */
    public OperationNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}