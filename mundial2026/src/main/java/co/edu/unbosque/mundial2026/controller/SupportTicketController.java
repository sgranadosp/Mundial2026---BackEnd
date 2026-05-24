package co.edu.unbosque.mundial2026.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.SupportTicketDTO;
import co.edu.unbosque.mundial2026.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller REST de los tickets de soporte.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST   /support/tickets}        — USER crea un ticket nuevo.</li>
 *   <li>{@code GET    /support/tickets/mine}   — USER lista sus tickets.</li>
 *   <li>{@code DELETE /support/tickets/{id}}   — USER elimina un ticket
 *       cerrado propio.</li>
 *   <li>{@code GET    /support/tickets}        — ADMIN lista tickets OPEN.</li>
 *   <li>{@code GET    /support/tickets/all}    — ADMIN lista todos.</li>
 *   <li>{@code PUT    /support/tickets/{id}/respond} — ADMIN responde y
 *       cierra; dispara push automática al usuario.</li>
 * </ul></p>
 */
@RestController
@RequestMapping("/support/tickets")
@CrossOrigin(origins = "*")
@Tag(name = "Soporte", description = "Tickets de soporte usuario-admin")
public class SupportTicketController {

    @Autowired
    private SupportTicketService supportService;

    /**
     * Crea un nuevo ticket para el usuario indicado.
     *
     * @param userId  ID del usuario que abre el ticket.
     * @param payload Mapa con la clave {@code message}.
     * @return 201 Created si fue creado; 400 si el mensaje está vacío;
     *         404 si el usuario no existe.
     */
    @PostMapping
    @Operation(summary = "Crear ticket de soporte",
               description = "El usuario abre un ticket nuevo con un mensaje libre.")
    public ResponseEntity<?> createTicket(@RequestParam Long userId,
                                           @RequestBody Map<String, String> payload) {
        String message = payload != null ? payload.get("message") : null;
        int status = supportService.createTicket(userId, message);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Ticket creado exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado", "success", false));
            case 6 -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "El mensaje es obligatorio y debe tener entre 1 y 2000 caracteres",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al crear el ticket", "success", false));
        };
    }

    /**
     * Lista los tickets del usuario actual.
     *
     * @param userId ID del usuario.
     * @return 202 con la lista (puede estar vacía).
     */
    @GetMapping("/mine")
    @Operation(summary = "Listar mis tickets",
               description = "Lista los tickets de soporte del usuario.")
    public ResponseEntity<List<SupportTicketDTO>> getMine(@RequestParam Long userId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(supportService.getTicketsForUser(userId));
    }

    /**
     * Lista los tickets abiertos pendientes de respuesta. Vista admin.
     *
     * @return 202 con la lista de tickets OPEN.
     */
    @GetMapping
    @Operation(summary = "Listar tickets abiertos (admin)",
               description = "Lista los tickets en estado OPEN para que el admin los responda.")
    public ResponseEntity<List<SupportTicketDTO>> getOpen() {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(supportService.getOpenTickets());
    }

    /**
     * Lista todos los tickets de soporte (incluyendo cerrados). Vista admin.
     *
     * @return 202 con la lista completa.
     */
    @GetMapping("/all")
    @Operation(summary = "Listar todos los tickets (admin)",
               description = "Lista todos los tickets de soporte de la plataforma.")
    public ResponseEntity<List<SupportTicketDTO>> getAll() {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(supportService.getAllTickets());
    }

    /**
     * Responde un ticket. Solo admin. Marca como CLOSED y dispara push.
     *
     * @param ticketId ID del ticket.
     * @param adminId  ID del admin que responde.
     * @param payload  Cuerpo con clave {@code response}.
     * @return 202 si fue respondido; 404 si no existe; 400 si ya estaba
     *         cerrado o respuesta inválida.
     */
    @PutMapping("/{ticketId}/respond")
    @Operation(summary = "Responder ticket (admin)",
               description = "El admin responde un ticket abierto. Dispara push automática al usuario.")
    public ResponseEntity<?> respond(@PathVariable Long ticketId,
                                      @RequestParam Long adminId,
                                      @RequestBody Map<String, String> payload) {
        String response = payload != null ? payload.get("response") : null;
        int status = supportService.respondTicket(ticketId, adminId, response);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Ticket respondido exitosamente", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Ticket no encontrado", "success", false));
            case 5 -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El ticket ya está cerrado", "success", false));
            case 6 -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "La respuesta es obligatoria y debe tener entre 1 y 2000 caracteres",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al responder el ticket", "success", false));
        };
    }

    /**
     * Elimina un ticket cerrado del propio usuario. No es soft delete:
     * el registro se borra de la BD definitivamente. Solo aplica a tickets
     * en estado CLOSED.
     *
     * @param ticketId ID del ticket.
     * @param userId   ID del usuario propietario.
     * @return 202 si fue eliminado; 404 si no existe; 403 si no es el
     *         dueño o el ticket sigue OPEN.
     */
    @DeleteMapping("/{ticketId}")
    @Operation(summary = "Eliminar ticket cerrado",
               description = "El usuario elimina un ticket propio ya respondido.")
    public ResponseEntity<?> delete(@PathVariable Long ticketId,
                                     @RequestParam Long userId) {
        int status = supportService.deleteTicket(ticketId, userId);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Ticket eliminado", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Ticket no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Solo puedes eliminar tus tickets cuando ya han sido respondidos",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al eliminar el ticket", "success", false));
        };
    }
}