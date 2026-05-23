/**
 * Paquete con los DTOs de pagos consumidos por la pasarela MercadoPago.
 */
package co.edu.unbosque.mundial2026.dto;

/**
 * DTO recibido en {@code POST /payments/create-preference}.
 * <p>
 * Representa la intención de un usuario de comprar {@code quantity} tickets
 * para el partido {@code matchId}. La categoría queda fija como CATEGORY_3
 * (la más común) en esta primera iteración; cuando se quiera permitir
 * múltiples categorías se agrega un campo {@code category} acá.
 * </p>
 */
public class CreatePreferenceRequest {

    /** ID del partido para el cual se comprarán los tickets. */
    private Long matchId;

    /** Cantidad de tickets a comprar (entre 1 y 4). */
    private Integer quantity;

    /** Constructor por defecto requerido por Jackson. */
    public CreatePreferenceRequest() { }

    /** @return ID del partido. */
    public Long getMatchId() { return matchId; }

    /** @param matchId ID del partido. */
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    /** @return Cantidad de tickets. */
    public Integer getQuantity() { return quantity; }

    /** @param quantity Cantidad de tickets. */
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}