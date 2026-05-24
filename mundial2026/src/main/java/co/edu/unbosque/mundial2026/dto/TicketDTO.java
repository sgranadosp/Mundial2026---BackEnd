/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.unbosque.mundial2026.model.Ticket.TicketCategory;
import co.edu.unbosque.mundial2026.model.Ticket.TicketStatus;

/**
 * Clase de Transferencia de Datos (DTO) para representar una entrada digital
 * para un partido del Mundial 2026.
 * Expone el estado del ciclo de vida de la entrada, el tiempo restante antes
 * de expirar la reserva ({@code reservationExpiresAt}) y los datos del partido
 * asociado con el mínimo necesario para la UI. El {@code correlationId}
 * se incluye para que soporte y compliance puedan rastrear la trazabilidad
 * completa de la operación en el panel de auditoría.
 */
public class TicketDTO {

    /**
     * Identificador único interno de la entrada.
     */
    private Long id;

    /**
     * ID de correlación único para trazabilidad de todas las operaciones
     * relacionadas con esta entrada.
     */
    private String correlationId;

    /**
     * ID del partido para el que aplica la entrada.
     */
    private Long matchId;

    /**
     * Nombre del equipo local del partido.
     */
    private String homeTeamName;

    /**
     * Nombre del equipo visitante del partido.
     */
    private String awayTeamName;

    /**
     * Fecha y hora programada del partido en UTC.
     */
    private LocalDateTime matchScheduledAt;

    /**
     * Nombre del estadio.
     */
    private String stadiumName;

    /**
     * Ciudad del estadio.
     */
    private String stadiumCity;

    /**
     * Latitud del estadio. Permite renderizar un mapa del estadio en
     * la pantalla "Mis Entradas" sin tener que hacer otra request.
     */
    private Double stadiumLatitude;

    /**
     * Longitud del estadio.
     */
    private Double stadiumLongitude;

    /**
     * ID del usuario titular actual de la entrada.
     */
    private Long holderId;

    /**
     * Nombre de usuario del titular actual.
     */
    private String holderUsername;

    /**
     * ID del usuario que realizó la reserva original.
     */
    private Long originalBuyerId;

    /**
     * Nombre de usuario del comprador original.
     */
    private String originalBuyerUsername;

    /**
     * Estado actual de la entrada en su ciclo de vida.
     */
    private TicketStatus status;

    /**
     * Categoría de zona de la entrada: CATEGORY_1, CATEGORY_2 o CATEGORY_3.
     */
    private TicketCategory category;

    /**
     * Precio simulado de la entrada en USD.
     */
    private Double price;

    /**
     * Fecha y hora de la reserva.
     */
    private LocalDateTime reservedAt;

    /**
     * Fecha y hora límite para completar el pago antes de que expire la reserva.
     */
    private LocalDateTime reservationExpiresAt;

    /**
     * Fecha y hora en que se confirmó el pago. Nulo si no está pagada.
     */
    private LocalDateTime paidAt;

    /**
     * ID de la transacción en el sistema de pagos sandbox (Stripe o WireMock).
     */
    private String paymentTransactionId;

    /**
     * Constructor por defecto de {@code TicketDTO}.
     */
    public TicketDTO() {
    }

    /**
     * Constructor con los campos necesarios para crear una reserva.
     *
     * @param matchId    ID del partido.
     * @param holderId   ID del usuario titular.
     * @param category   Categoría de la entrada.
     * @param price      Precio simulado.
     */
    public TicketDTO(Long matchId, Long holderId, TicketCategory category, Double price) {
        this.matchId = matchId;
        this.holderId = holderId;
        this.category = category;
        this.price = price;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID de la entrada. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID de correlación. */
    public String getCorrelationId() { return correlationId; }

    /** @param correlationId El nuevo ID de correlación. */
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    /** @return El ID del partido. */
    public Long getMatchId() { return matchId; }

    /** @param matchId El nuevo ID del partido. */
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    /** @return El nombre del equipo local. */
    public String getHomeTeamName() { return homeTeamName; }

    /** @param homeTeamName El nuevo nombre del equipo local. */
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    /** @return El nombre del equipo visitante. */
    public String getAwayTeamName() { return awayTeamName; }

    /** @param awayTeamName El nuevo nombre del equipo visitante. */
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

    /** @return La fecha y hora del partido en UTC. */
    public LocalDateTime getMatchScheduledAt() { return matchScheduledAt; }

    /** @param matchScheduledAt La nueva fecha del partido. */
    public void setMatchScheduledAt(LocalDateTime matchScheduledAt) {
        this.matchScheduledAt = matchScheduledAt;
    }

    /** @return El nombre del estadio. */
    public String getStadiumName() { return stadiumName; }

    /** @param stadiumName El nuevo nombre del estadio. */
    public void setStadiumName(String stadiumName) { this.stadiumName = stadiumName; }

    /** @return La ciudad del estadio. */
    public String getStadiumCity() { return stadiumCity; }

    /** @param stadiumCity La nueva ciudad. */
    public void setStadiumCity(String stadiumCity) { this.stadiumCity = stadiumCity; }

    /** @return Latitud del estadio. */
    public Double getStadiumLatitude() { return stadiumLatitude; }

    /** @param stadiumLatitude Nueva latitud. */
    public void setStadiumLatitude(Double stadiumLatitude) { this.stadiumLatitude = stadiumLatitude; }

    /** @return Longitud del estadio. */
    public Double getStadiumLongitude() { return stadiumLongitude; }

    /** @param stadiumLongitude Nueva longitud. */
    public void setStadiumLongitude(Double stadiumLongitude) { this.stadiumLongitude = stadiumLongitude; }

    /** @return El ID del titular actual. */
    public Long getHolderId() { return holderId; }

    /** @param holderId El nuevo ID del titular. */
    public void setHolderId(Long holderId) { this.holderId = holderId; }

    /** @return El nombre de usuario del titular actual. */
    public String getHolderUsername() { return holderUsername; }

    /** @param holderUsername El nuevo nombre de usuario del titular. */
    public void setHolderUsername(String holderUsername) { this.holderUsername = holderUsername; }

    /** @return El ID del comprador original. */
    public Long getOriginalBuyerId() { return originalBuyerId; }

    /** @param originalBuyerId El nuevo ID del comprador original. */
    public void setOriginalBuyerId(Long originalBuyerId) { this.originalBuyerId = originalBuyerId; }

    /** @return El nombre de usuario del comprador original. */
    public String getOriginalBuyerUsername() { return originalBuyerUsername; }

    /** @param originalBuyerUsername El nuevo nombre del comprador original. */
    public void setOriginalBuyerUsername(String originalBuyerUsername) {
        this.originalBuyerUsername = originalBuyerUsername;
    }

    /** @return El estado actual de la entrada. */
    public TicketStatus getStatus() { return status; }

    /** @param status El nuevo estado. */
    public void setStatus(TicketStatus status) { this.status = status; }

    /** @return La categoría de la entrada. */
    public TicketCategory getCategory() { return category; }

    /** @param category La nueva categoría. */
    public void setCategory(TicketCategory category) { this.category = category; }

    /** @return El precio simulado en USD. */
    public Double getPrice() { return price; }

    /** @param price El nuevo precio. */
    public void setPrice(Double price) { this.price = price; }

    /** @return La fecha de la reserva. */
    public LocalDateTime getReservedAt() { return reservedAt; }

    /** @param reservedAt La nueva fecha de reserva. */
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }

    /** @return La fecha límite de la reserva. */
    public LocalDateTime getReservationExpiresAt() { return reservationExpiresAt; }

    /** @param reservationExpiresAt La nueva fecha de expiración. */
    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }

    /** @return La fecha de confirmación del pago. */
    public LocalDateTime getPaidAt() { return paidAt; }

    /** @param paidAt La nueva fecha de pago. */
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    /** @return El ID de la transacción sandbox. */
    public String getPaymentTransactionId() { return paymentTransactionId; }

    /** @param paymentTransactionId El nuevo ID de transacción. */
    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TicketDTO other = (TicketDTO) obj;
        return Objects.equals(id, other.id) && Objects.equals(correlationId, other.correlationId);
    }

    @Override
    public int hashCode() { return Objects.hash(id, correlationId); }

    @Override
    public String toString() {
        return "TicketDTO [id=" + id + ", correlationId=" + correlationId
                + ", homeTeamName=" + homeTeamName + ", awayTeamName=" + awayTeamName
                + ", holderUsername=" + holderUsername
                + ", status=" + status + ", category=" + category
                + ", price=" + price + ", reservationExpiresAt=" + reservationExpiresAt + "]";
    }
}