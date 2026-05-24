/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.Objects;

/**
 * DTO con todas las métricas agregadas del Dashboard administrativo
 * (HU16/HU20). Se devuelve desde {@code GET /admin/dashboard/summary} en
 * un solo round-trip para evitar 4-5 llamadas separadas desde el frontend.
 *
 * <p>Cada par de campos refleja una tarjeta del panel:</p>
 * <ul>
 *   <li>Usuarios registrados — {@code totalUsers} / {@code newUsersThisWeek}.</li>
 *   <li>Partidos registrados — {@code totalMatches} / {@code liveMatches}.</li>
 *   <li>Grupos de polla — {@code totalPollGroups} / {@code totalPredictions}.</li>
 *   <li>Entradas activas — {@code activeTickets} / {@code ticketsExpiringSoon}.</li>
 * </ul>
 *
 * <p>El frontend hace polling de este endpoint cada 15 segundos para mantener
 * el dashboard actualizado sin necesidad de WebSockets.</p>
 */
public class DashboardSummaryDTO {

    // ─── Usuarios ─────────────────────────────────────────────────────────

    /**
     * Número total de usuarios registrados en el sistema, incluyendo
     * bloqueados y no verificados.
     */
    private long totalUsers;

    /**
     * Número de usuarios registrados en los últimos 7 días. Se calcula
     * contando eventos {@code USER_REGISTERED} en la tabla de auditoría
     * dentro de la ventana temporal correspondiente.
     */
    private long newUsersThisWeek;

    // ─── Partidos ─────────────────────────────────────────────────────────

    /**
     * Número total de partidos sincronizados desde la API externa de fútbol
     * (football-data.org u OpenFootball) y persistidos en la tabla
     * {@code matches}.
     */
    private long totalMatches;

    /**
     * Número de partidos actualmente en estado {@code LIVE}.
     */
    private long liveMatches;

    // ─── Pollas ───────────────────────────────────────────────────────────

    /**
     * Número total de grupos de polla creados en el sistema.
     */
    private long totalPollGroups;

    /**
     * Número total de pronósticos registrados por los usuarios (todos los
     * estados: OPEN, LOCKED, EVALUATED).
     */
    private long totalPredictions;

    // ─── Entradas ─────────────────────────────────────────────────────────

    /**
     * Número de entradas en estado activo (RESERVED + PAID). Las
     * transferidas, reembolsadas y expiradas no se cuentan.
     */
    private long activeTickets;

    /**
     * Número de reservas (status RESERVED) cuyo {@code reservationExpiresAt}
     * cae dentro de los próximos 15 minutos. Le indica al admin si debe
     * disparar el job de expiración manualmente.
     */
    private long ticketsExpiringSoon;

    /**
     * Constructor por defecto.
     */
    public DashboardSummaryDTO() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param totalUsers            Total de usuarios.
     * @param newUsersThisWeek      Usuarios nuevos en últimos 7 días.
     * @param totalMatches          Total de partidos.
     * @param liveMatches           Partidos en vivo.
     * @param totalPollGroups       Total de grupos de polla.
     * @param totalPredictions      Total de pronósticos.
     * @param activeTickets         Entradas activas.
     * @param ticketsExpiringSoon   Reservas que expiran en 15 minutos.
     */
    public DashboardSummaryDTO(long totalUsers, long newUsersThisWeek,
            long totalMatches, long liveMatches,
            long totalPollGroups, long totalPredictions,
            long activeTickets, long ticketsExpiringSoon) {
        this.totalUsers = totalUsers;
        this.newUsersThisWeek = newUsersThisWeek;
        this.totalMatches = totalMatches;
        this.liveMatches = liveMatches;
        this.totalPollGroups = totalPollGroups;
        this.totalPredictions = totalPredictions;
        this.activeTickets = activeTickets;
        this.ticketsExpiringSoon = ticketsExpiringSoon;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getNewUsersThisWeek() { return newUsersThisWeek; }
    public void setNewUsersThisWeek(long newUsersThisWeek) { this.newUsersThisWeek = newUsersThisWeek; }

    public long getTotalMatches() { return totalMatches; }
    public void setTotalMatches(long totalMatches) { this.totalMatches = totalMatches; }

    public long getLiveMatches() { return liveMatches; }
    public void setLiveMatches(long liveMatches) { this.liveMatches = liveMatches; }

    public long getTotalPollGroups() { return totalPollGroups; }
    public void setTotalPollGroups(long totalPollGroups) { this.totalPollGroups = totalPollGroups; }

    public long getTotalPredictions() { return totalPredictions; }
    public void setTotalPredictions(long totalPredictions) { this.totalPredictions = totalPredictions; }

    public long getActiveTickets() { return activeTickets; }
    public void setActiveTickets(long activeTickets) { this.activeTickets = activeTickets; }

    public long getTicketsExpiringSoon() { return ticketsExpiringSoon; }
    public void setTicketsExpiringSoon(long ticketsExpiringSoon) { this.ticketsExpiringSoon = ticketsExpiringSoon; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DashboardSummaryDTO other = (DashboardSummaryDTO) obj;
        return totalUsers == other.totalUsers
                && newUsersThisWeek == other.newUsersThisWeek
                && totalMatches == other.totalMatches
                && liveMatches == other.liveMatches
                && totalPollGroups == other.totalPollGroups
                && totalPredictions == other.totalPredictions
                && activeTickets == other.activeTickets
                && ticketsExpiringSoon == other.ticketsExpiringSoon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalUsers, newUsersThisWeek, totalMatches, liveMatches,
                totalPollGroups, totalPredictions, activeTickets, ticketsExpiringSoon);
    }

    @Override
    public String toString() {
        return "DashboardSummaryDTO [totalUsers=" + totalUsers
                + ", newUsersThisWeek=" + newUsersThisWeek
                + ", totalMatches=" + totalMatches
                + ", liveMatches=" + liveMatches
                + ", totalPollGroups=" + totalPollGroups
                + ", totalPredictions=" + totalPredictions
                + ", activeTickets=" + activeTickets
                + ", ticketsExpiringSoon=" + ticketsExpiringSoon + "]";
    }
}
