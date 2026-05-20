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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.TicketDTO;
import co.edu.unbosque.mundial2026.model.Ticket.TicketCategory;
import co.edu.unbosque.mundial2026.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para el módulo de entradas digitales en la plataforma
 * Mundial 2026 Hub.
 * Gestiona el ciclo de vida completo de una entrada:
 * {@code RESERVED → PAID → (TRANSFERRED | REFUNDED | EXPIRED)}.
 * Todos los pagos se procesan en modo sandbox (Stripe test o WireMock).
 * Las reservas tienen un TTL configurable; el job de expiración se expone
 * como endpoint administrativo para demo y pruebas.
 */
@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Transactional
@Tag(name = "Entradas", description = "Reserva, pago, transferencia y reembolso de entradas (modo sandbox)")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    /**
     * Servicio de lógica de negocio de entradas.
     */
    @Autowired
    private TicketService ticketService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public TicketController() {
    }

    // =========================================================================
    // Ciclo de vida de la entrada
    // =========================================================================

    /**
     * Reserva una entrada para un partido (estado inicial: RESERVED).
     * Genera un correlationId único para toda la trazabilidad de la operación.
     * Aplica el límite antifraude de entradas activas por usuario.
     *
     * @param matchId  El ID del partido.
     * @param userId   El ID del usuario que reserva.
     * @param category La categoría de la entrada: CATEGORY_1, CATEGORY_2 o CATEGORY_3.
     * @param price    El precio simulado en USD.
     * @return 201 Created con el {@link TicketDTO} de la reserva (incluye TTL);
     *         404 si el partido o usuario no existen; 403 si el partido no acepta
     *         reservas; 409 si se superó el límite antifraude.
     */
    @PostMapping("/reserve")
    @Operation(summary = "Reservar entrada",
               description = "Crea una reserva. El pago debe confirmarse antes de que expire el TTL (15 minutos).")
    public ResponseEntity<?> reserve(@RequestParam Long matchId,
                                      @RequestParam Long userId,
                                      @RequestParam TicketCategory category,
                                      @RequestParam Double price) {
        TicketDTO ticket = ticketService.reserveTicket(matchId, userId, category, price);
        if (ticket != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message",
                        "No se pudo crear la reserva. Verifica que el partido esté disponible o que no hayas superado el límite de entradas.",
                        "success", false));
    }

    /**
     * Confirma el pago de una entrada reservada (RESERVED → PAID).
     * Registra el ID de la transacción sandbox (Stripe o WireMock).
     * Si el TTL de la reserva ya venció, la marca como EXPIRED y retorna error.
     *
     * @param ticketId             El ID de la entrada reservada.
     * @param userId               El ID del usuario titular.
     * @param paymentTransactionId El ID de la transacción del sistema de pagos sandbox.
     * @return 202 Accepted si el pago fue confirmado; 404 si no existe;
     *         403 si la reserva expiró, ya fue pagada o el usuario no es el titular.
     */
    @PutMapping("/{ticketId}/pay")
    @Operation(summary = "Confirmar pago",
               description = "Registra el ID de transacción sandbox y confirma el pago de la reserva.")
    public ResponseEntity<?> confirmPayment(@PathVariable Long ticketId,
                                             @RequestParam Long userId,
                                             @RequestParam String paymentTransactionId) {
        int status = ticketService.confirmPayment(ticketId, userId, paymentTransactionId);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Pago confirmado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Entrada no encontrada", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "La reserva expiró, ya fue pagada o no eres el titular",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al confirmar el pago", "success", false));
        };
    }

    /**
     * Transfiere una entrada pagada a otro usuario (PAID → TRANSFERRED).
     * Registra quién transfiere, quién recibe y cuándo (no repudio).
     *
     * @param ticketId   El ID de la entrada.
     * @param fromUserId El ID del usuario que transfiere (debe ser el titular).
     * @param toUserId   El ID del usuario que recibe la entrada.
     * @return 202 Accepted si fue transferida; 404 si no existe;
     *         403 si la entrada no está pagada o el usuario no es el titular.
     */
    @PutMapping("/{ticketId}/transfer")
    @Operation(summary = "Transferir entrada",
               description = "Transfiere una entrada pagada a otro usuario. Requiere que esté en estado PAID.")
    public ResponseEntity<?> transfer(@PathVariable Long ticketId,
                                       @RequestParam Long fromUserId,
                                       @RequestParam Long toUserId) {
        int status = ticketService.transferTicket(ticketId, fromUserId, toUserId);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Entrada transferida exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Entrada o usuario destino no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "La entrada no está pagada o no eres el titular actual",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al transferir la entrada", "success", false));
        };
    }

    /**
     * Procesa el reembolso de una entrada pagada (PAID → REFUNDED).
     * Solo aplicable a entradas en estado PAID. Registra el ID de la transacción
     * de reembolso del sistema sandbox.
     *
     * @param ticketId            El ID de la entrada.
     * @param userId              El ID del titular de la entrada.
     * @param refundTransactionId El ID de la transacción de reembolso sandbox.
     * @return 202 Accepted si fue procesado; 404 si no existe; 403 si no es elegible.
     */
    @PutMapping("/{ticketId}/refund")
    @Operation(summary = "Solicitar reembolso",
               description = "Procesa el reembolso en modo sandbox. Solo para entradas en estado PAID.")
    public ResponseEntity<?> refund(@PathVariable Long ticketId,
                                     @RequestParam Long userId,
                                     @RequestParam String refundTransactionId) {
        int status = ticketService.refundTicket(ticketId, userId, refundTransactionId);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Reembolso procesado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Entrada no encontrada", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "La entrada no es elegible para reembolso o no eres el titular",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al procesar el reembolso", "success", false));
        };
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    /**
     * Lista las entradas pagadas activas del usuario titular.
     *
     * @param userId El ID del usuario.
     * @return 202 Accepted con la lista de entradas pagadas; 204 si no tiene.
     */
    @GetMapping("/user/{userId}/paid")
    @Operation(summary = "Entradas pagadas del usuario",
               description = "Retorna las entradas en estado PAID del usuario titular.")
    public ResponseEntity<List<TicketDTO>> getPaidTickets(@PathVariable Long userId) {
        List<TicketDTO> tickets = ticketService.getPaidTicketsByUser(userId);
        if (tickets.isEmpty()) {
            return new ResponseEntity<>(tickets, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(tickets, HttpStatus.ACCEPTED);
    }

    /**
     * Lista el historial completo de entradas de un usuario (todos los estados).
     *
     * @param userId El ID del usuario.
     * @return 202 Accepted con el historial; 204 si no hay registros.
     */
    @GetMapping("/user/{userId}/history")
    @Operation(summary = "Historial de entradas",
               description = "Retorna todas las entradas del usuario en todos los estados del ciclo de vida.")
    public ResponseEntity<List<TicketDTO>> getHistory(@PathVariable Long userId) {
        List<TicketDTO> tickets = ticketService.getTicketHistoryByUser(userId);
        if (tickets.isEmpty()) {
            return new ResponseEntity<>(tickets, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(tickets, HttpStatus.ACCEPTED);
    }

    /**
     * Busca una entrada por su correlationId. Endpoint de soporte y auditoría.
     *
     * @param correlationId El ID de correlación único de la entrada.
     * @return 202 Accepted con la entrada; 404 si no existe.
     */
    @GetMapping("/correlation/{correlationId}")
    @Operation(summary = "Buscar por correlationId",
               description = "Retorna la entrada y su estado actual. Usado por soporte para trazabilidad.")
    public ResponseEntity<?> getByCorrelationId(@PathVariable String correlationId) {
        TicketDTO ticket = ticketService.getByCorrelationId(correlationId);
        if (ticket != null) {
            return new ResponseEntity<>(ticket, HttpStatus.ACCEPTED);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Entrada no encontrada con ese correlationId", "success", false));
    }

    // =========================================================================
    // Endpoint administrativo
    // =========================================================================

    /**
     * Ejecuta el job de expiración de reservas. Marca como EXPIRED todas las
     * entradas en estado RESERVED cuyo TTL ya venció y libera el cupo.
     * Solo ADMIN. Puede configurarse como {@code @Scheduled} en producción.
     *
     * @return 200 OK con el número de reservas expiradas.
     */
    @PostMapping("/admin/expire")
    @Operation(summary = "Ejecutar expiración de reservas",
               description = "Solo ADMIN. Marca como EXPIRED las reservas vencidas y libera cupos.")
    public ResponseEntity<?> expireReservations() {
        int expired = ticketService.expireReservations();
        return ResponseEntity.ok(
                Map.of("message", "Job de expiración ejecutado", "expired", expired, "success", true));
    }
}