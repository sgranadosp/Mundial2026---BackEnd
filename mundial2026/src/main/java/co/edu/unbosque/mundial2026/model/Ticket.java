/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa una entrada digital para un partido del Mundial 2026.
 * <p>
 * La entrada recorre un ciclo de vida controlado con estados estrictamente secuenciales:
 * {@code AVAILABLE → RESERVED → PAID → (TRANSFERRED | REFUNDED | EXPIRED)}.
 * Cada transición de estado deja trazabilidad completa mediante el campo
 * {@code correlationId} y las fechas de cambio.
 * </p>
 * <p>
 * Las reservas tienen un tiempo máximo (TTL) configurable; si no se confirma el
 * pago dentro de la ventana, el sistema expira la reserva y libera el cupo.
 * Los pagos se procesan en modo sandbox (Stripe test mode o WireMock)
 * ya que el proyecto es académico y no gestiona dinero real.
 * </p>
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    /**
     * Identificador único de la entrada generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador único de correlación para trazabilidad de todas las
     * operaciones relacionadas con esta entrada (reserva, pago, transferencia,
     * reembolso). Se genera al crear la reserva y se mantiene constante.
     */
    @Column(unique = true)
    private String correlationId;

    /**
     * Partido para el que aplica la entrada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    /**
     * Usuario titular actual de la entrada.
     * Cambia si la entrada es transferida a otro usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holder_id")
    private User holder;

    /**
     * Usuario que realizó la reserva original de la entrada.
     * Permanece constante incluso si la entrada es transferida.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_buyer_id")
    private User originalBuyer;

    /**
     * Estado actual de la entrada en su ciclo de vida,
     * definido mediante el enum {@link TicketStatus}.
     */
    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    /**
     * Categoría de la entrada que determina la zona del estadio,
     * definida mediante el enum {@link TicketCategory}.
     */
    @Enumerated(EnumType.STRING)
    private TicketCategory category;

    /**
     * Precio simulado de la entrada en dólares (USD).
     * Se procesa en modo sandbox; no implica transacciones reales.
     */
    private Double price;

    /**
     * Fecha y hora en que la reserva fue creada.
     */
    private LocalDateTime reservedAt;

    /**
     * Fecha y hora límite para confirmar el pago antes de que la reserva expire (TTL).
     * Si el pago no se realiza antes de esta fecha, la entrada pasa a estado {@code EXPIRED}.
     */
    private LocalDateTime reservationExpiresAt;

    /**
     * Fecha y hora en que el pago fue confirmado por el sistema de pagos sandbox.
     * Es nulo si la entrada no ha sido pagada.
     */
    private LocalDateTime paidAt;

    /**
     * Identificador de la transacción en el sistema de pagos sandbox (Stripe o WireMock).
     * Se usa para trazabilidad y manejo de reembolsos.
     */
    private String paymentTransactionId;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el estado como {@code RESERVED}.
     */
    public Ticket() {
        this.status = TicketStatus.RESERVED;
    }

    /**
     * Constructor con los datos principales de la entrada.
     *
     * @param correlationId         ID de correlación único.
     * @param match                 Partido para el que aplica la entrada.
     * @param holder                Usuario titular de la entrada.
     * @param originalBuyer         Usuario que realizó la reserva original.
     * @param category              Categoría de la entrada.
     * @param price                 Precio simulado en USD.
     * @param reservationExpiresAt  Fecha y hora de expiración de la reserva.
     */
    public Ticket(String correlationId, Match match, User holder, User originalBuyer,
                  TicketCategory category, Double price, LocalDateTime reservationExpiresAt) {
        this();
        this.correlationId = correlationId;
        this.match = match;
        this.holder = holder;
        this.originalBuyer = originalBuyer;
        this.category = category;
        this.price = price;
        this.reservedAt = LocalDateTime.now();
        this.reservationExpiresAt = reservationExpiresAt;
    }

    /**
     * Enumeración que define el ciclo de vida de una entrada.
     * Los estados son estrictamente secuenciales y cada transición es auditada.
     */
    public enum TicketStatus {
        /** La entrada está disponible para ser reservada. */
        AVAILABLE,
        /** La entrada fue reservada; el usuario tiene hasta {@code reservationExpiresAt} para pagar. */
        RESERVED,
        /** La entrada fue pagada y confirmada. */
        PAID,
        /** La entrada fue transferida a otro usuario. */
        TRANSFERRED,
        /** La entrada fue reembolsada. */
        REFUNDED,
        /** La reserva expiró antes de ser pagada; el cupo fue liberado. */
        EXPIRED
    }

    /**
     * Enumeración que define la categoría de zona de la entrada.
     */
    public enum TicketCategory {
        /** Categoría 1: tribunas centrales, mejor ubicación. */
        CATEGORY_1,
        /** Categoría 2: tribunas laterales. */
        CATEGORY_2,
        /** Categoría 3: tribunas altas o fondos. */
        CATEGORY_3
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID de la entrada. */
    public Long getId() { return id; }

    /** @param id El nuevo ID de la entrada. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID de correlación de la entrada. */
    public String getCorrelationId() { return correlationId; }

    /** @param correlationId El nuevo ID de correlación. */
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    /** @return El partido para el que aplica la entrada. */
    public Match getMatch() { return match; }

    /** @param match El nuevo partido. */
    public void setMatch(Match match) { this.match = match; }

    /** @return El usuario titular actual de la entrada. */
    public User getHolder() { return holder; }

    /** @param holder El nuevo titular de la entrada. */
    public void setHolder(User holder) { this.holder = holder; }

    /** @return El usuario que realizó la reserva original. */
    public User getOriginalBuyer() { return originalBuyer; }

    /** @param originalBuyer El nuevo comprador original. */
    public void setOriginalBuyer(User originalBuyer) { this.originalBuyer = originalBuyer; }

    /** @return El estado actual de la entrada. */
    public TicketStatus getStatus() { return status; }

    /** @param status El nuevo estado de la entrada. */
    public void setStatus(TicketStatus status) { this.status = status; }

    /** @return La categoría de la entrada. */
    public TicketCategory getCategory() { return category; }

    /** @param category La nueva categoría. */
    public void setCategory(TicketCategory category) { this.category = category; }

    /** @return El precio simulado de la entrada en USD. */
    public Double getPrice() { return price; }

    /** @param price El nuevo precio simulado. */
    public void setPrice(Double price) { this.price = price; }

    /** @return La fecha y hora en que se realizó la reserva. */
    public LocalDateTime getReservedAt() { return reservedAt; }

    /** @param reservedAt La nueva fecha de reserva. */
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }

    /** @return La fecha y hora límite para pagar antes de expirar la reserva. */
    public LocalDateTime getReservationExpiresAt() { return reservationExpiresAt; }

    /** @param reservationExpiresAt La nueva fecha de expiración de la reserva. */
    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }

    /** @return La fecha y hora en que se confirmó el pago. */
    public LocalDateTime getPaidAt() { return paidAt; }

    /** @param paidAt La nueva fecha de pago. */
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    /** @return El ID de la transacción en el sistema de pagos sandbox. */
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
        Ticket other = (Ticket) obj;
        return Objects.equals(id, other.id) && Objects.equals(correlationId, other.correlationId);
    }

    @Override
    public int hashCode() { return Objects.hash(id, correlationId); }

    @Override
    public String toString() {
        return "Ticket [id=" + id + ", correlationId=" + correlationId
                + ", match=" + (match != null ? match.getId() : "null")
                + ", holder=" + (holder != null ? holder.getUsername() : "null")
                + ", status=" + status + ", category=" + category
                + ", price=" + price + ", reservedAt=" + reservedAt
                + ", reservationExpiresAt=" + reservationExpiresAt + "]";
    }
}