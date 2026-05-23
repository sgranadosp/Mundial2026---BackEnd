package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidad JPA que registra qué solicitudes de intercambio ha rechazado
 * cada usuario.
 * <p>
 * Como una solicitud {@link TradeRequest} es un broadcast (la ven todos los
 * usuarios con rol USER), un rechazo NO la elimina globalmente. Solo la
 * oculta para el usuario que rechazó. Esta tabla guarda esa relación.
 * </p>
 * <p>
 * Cuando el usuario hace GET /trades/received, el back filtra todas las
 * solicitudes PENDING y excluye las que tienen una fila aquí con su userId.
 * </p>
 */
@Entity
@Table(name = "trade_rejections",
       indexes = {
           @Index(name = "idx_trade_rejections_user", columnList = "user_id"),
           @Index(name = "idx_trade_rejections_trade", columnList = "trade_request_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_trade_rejection",
                             columnNames = { "user_id", "trade_request_id" })
       })
public class TradeRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que rechazó la solicitud (la oculta de su vista). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Solicitud que fue rechazada. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_request_id", nullable = false)
    private TradeRequest tradeRequest;

    /** Fecha en que el usuario rechazó la solicitud. */
    @Column(name = "rejected_at", nullable = false)
    private LocalDateTime rejectedAt;

    public TradeRejection() {}

    public TradeRejection(User user, TradeRequest tradeRequest) {
        this.user = user;
        this.tradeRequest = tradeRequest;
        this.rejectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TradeRequest getTradeRequest() { return tradeRequest; }
    public void setTradeRequest(TradeRequest tradeRequest) { this.tradeRequest = tradeRequest; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TradeRejection other = (TradeRejection) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}