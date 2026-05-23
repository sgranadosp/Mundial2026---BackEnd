package co.edu.unbosque.mundial2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.mundial2026.model.TradeRejection;

/**
 * Repositorio JPA para {@link TradeRejection}.
 * <p>
 * La mayor parte de las consultas se hacen embebidas en el query de
 * {@link TradeRequestRepository#findReceivedForUser(Long)} (con un NOT IN),
 * así que aquí solo se exponen operaciones puntuales de existencia.
 * </p>
 */
@Repository
public interface TradeRejectionRepository extends JpaRepository<TradeRejection, Long> {

    /**
     * Devuelve {@code true} si el usuario ya rechazó la solicitud indicada.
     */
    boolean existsByUserIdAndTradeRequestId(Long userId, Long tradeRequestId);
}