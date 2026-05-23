/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.mundial2026.model.Ticket;
import co.edu.unbosque.mundial2026.model.Ticket.TicketStatus;

/**
 * Interfaz de repositorio para la entidad {@link Ticket}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code tickets}. Los métodos personalizados cubren el ciclo de vida
 * completo de una entrada: reserva, pago, transferencia, reembolso y expiración.
 * Incluye consultas para el TTL de reservas (expirar reservas no pagadas a tiempo)
 * y para los límites antifraude de compras por usuario.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Busca una entrada por su ID de correlación único.
     * Se usa en todos los flujos de soporte y auditoría para recuperar
     * una entrada a partir del identificador que aparece en los logs.
     *
     * @param correlationId El ID de correlación de la entrada.
     * @return Un {@link Optional} con la entrada encontrada, o vacío si no existe.
     */
    Optional<Ticket> findByCorrelationId(String correlationId);

    /**
     * Obtiene todas las entradas cuyo titular actual es un usuario específico.
     * Se usa para mostrar las entradas activas del usuario en su perfil.
     *
     * @param holderId El ID del usuario titular.
     * @return Lista de entradas del titular indicado.
     */
    List<Ticket> findByHolderId(Long holderId);

    /**
     * Obtiene las entradas de un usuario filtradas por estado del ciclo de vida.
     * El uso principal es buscar entradas {@code PAID} para mostrar las entradas
     * confirmadas del usuario.
     *
     * @param holderId El ID del usuario titular.
     * @param status   El estado de la entrada.
     * @return Lista de entradas del titular con el estado indicado.
     */
    List<Ticket> findByHolderIdAndStatus(Long holderId, TicketStatus status);

    /**
     * Obtiene todas las entradas del comprador original, incluyendo las que
     * pudieron ser transferidas a otro titular. Se usa para el historial
     * completo de compras del usuario.
     *
     * @param originalBuyerId El ID del usuario comprador original.
     * @return Lista de entradas cuyo comprador original es el usuario indicado.
     */
    List<Ticket> findByOriginalBuyerId(Long originalBuyerId);

    /**
     * Obtiene todas las entradas de un partido específico.
     * Se usa en el panel de administración para consultar la ocupación
     * de un partido.
     *
     * @param matchId El ID del partido.
     * @return Lista de entradas del partido.
     */
    List<Ticket> findByMatchId(Long matchId);

    /**
     * Obtiene las entradas de un partido con un estado específico.
     * Se usa para contar cupos disponibles ({@code AVAILABLE}) y
     * entradas pagadas ({@code PAID}) por partido.
     *
     * @param matchId El ID del partido.
     * @param status  El estado de la entrada.
     * @return Lista de entradas del partido con el estado indicado.
     */
    List<Ticket> findByMatchIdAndStatus(Long matchId, TicketStatus status);

    /**
     * Busca todas las reservas expiradas: estado {@code RESERVED} cuyo tiempo
     * de expiración ya pasó. Se usa en el job de limpieza periódico que libera
     * cupos de reservas no confirmadas (TTL de reservas).
     *
     * @param status    El estado de la entrada (normalmente {@code RESERVED}).
     * @param threshold La fecha y hora actual; se buscan reservas con
     *                  {@code reservationExpiresAt} anterior a este valor.
     * @return Lista de entradas reservadas que ya superaron su ventana de pago.
     */
    List<Ticket> findByStatusAndReservationExpiresAtBefore(TicketStatus status, LocalDateTime threshold);

    /**
     * Cuenta cuántas entradas pagadas o reservadas tiene un usuario para detectar
     * patrones anómalos de compra. Se usa en el módulo antifraude para aplicar
     * los límites parametrizables por usuario.
     *
     * @param originalBuyerId El ID del comprador original.
     * @return Número de entradas activas (RESERVED o PAID) del usuario.
     */
    @Query("SELECT COUNT(t) FROM Ticket t " +
           "WHERE t.originalBuyer.id = :originalBuyerId " +
           "AND t.status IN ('RESERVED', 'PAID')")
    long countActiveTicketsByOriginalBuyerId(@Param("originalBuyerId") Long originalBuyerId);

    /**
     * Busca una entrada por el ID de preference de MercadoPago. Se usa al
     * recibir un webhook para identificar qué ticket actualizar.
     *
     * @param mpPreferenceId El ID de preference asignado por MercadoPago.
     * @return Un {@link Optional} con la entrada, o vacío si no existe.
     */
    Optional<Ticket> findByMpPreferenceId(String mpPreferenceId);

    /**
     * Verifica si ya existe una entrada con el ID de correlación dado.
     * Se usa antes de crear una reserva para garantizar la unicidad del
     * correlationId generado.
     *
     * @param correlationId El ID de correlación a verificar.
     * @return {@code true} si ya existe una entrada con ese correlationId.
     */
    boolean existsByCorrelationId(String correlationId);
}