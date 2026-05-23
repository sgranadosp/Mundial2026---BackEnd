package co.edu.unbosque.mundial2026.dto.trade;

/**
 * DTO de entrada para POST /trades.
 *
 * El cliente envía solo los IDs de las dos láminas (la ofrecida y la pedida).
 * El {@code creatorId} NO se envía: se toma del JWT del usuario autenticado.
 */
public class CreateTradeRequestDTO {

    /** ID de la lámina que el creador OFRECE (debe tenerla como REPETIDA). */
    private Long offeredStickerId;

    /** ID de la lámina que el creador PIDE (debe estar FALTANTE en su álbum). */
    private Long requestedStickerId;

    public CreateTradeRequestDTO() {}

    public CreateTradeRequestDTO(Long offeredStickerId, Long requestedStickerId) {
        this.offeredStickerId = offeredStickerId;
        this.requestedStickerId = requestedStickerId;
    }

    public Long getOfferedStickerId() { return offeredStickerId; }
    public void setOfferedStickerId(Long offeredStickerId) {
        this.offeredStickerId = offeredStickerId;
    }

    public Long getRequestedStickerId() { return requestedStickerId; }
    public void setRequestedStickerId(Long requestedStickerId) {
        this.requestedStickerId = requestedStickerId;
    }
}