package co.edu.unbosque.mundial2026.dto.pack;

/**
 * DTO de respuesta del endpoint GET /packs/me/inventory.
 *
 * Resume el estado de los packs del usuario autenticado:
 *  - cuántos pendientes tiene de cada origen (para los 3 paneles)
 *  - cuántos segundos faltan para el próximo pack diario
 *  - cuántos pronósticos lleva y cuántos le faltan para el próximo pack
 *    por pronósticos
 */
public class PackInventoryDTO {

    /** Packs de bienvenida pendientes (típicamente 0 ó 3). */
    private long pendingWelcome;

    /** Packs diarios pendientes (acumulables). */
    private long pendingDaily;

    /** Packs por pronósticos pendientes (acumulables). */
    private long pendingPoll;

    /**
     * Segundos restantes hasta que se otorgue el próximo pack diario.
     * 0 si ya está disponible (el back lo va a otorgar al refrescar
     * el endpoint) o si el usuario no ha hecho su primer login todavía.
     */
    private long secondsUntilNextDaily;

    /** Total de pronósticos registrados por el usuario. */
    private long totalPredictions;

    /**
     * Cuántos pronósticos faltan para alcanzar el próximo múltiplo de 6
     * y desbloquear otro pack. Vale 6 si está en múltiplo exacto (0, 6, 12...).
     */
    private long predictionsUntilNextPoll;

    public PackInventoryDTO() {}

    public PackInventoryDTO(long pendingWelcome, long pendingDaily, long pendingPoll,
                             long secondsUntilNextDaily, long totalPredictions,
                             long predictionsUntilNextPoll) {
        this.pendingWelcome = pendingWelcome;
        this.pendingDaily = pendingDaily;
        this.pendingPoll = pendingPoll;
        this.secondsUntilNextDaily = secondsUntilNextDaily;
        this.totalPredictions = totalPredictions;
        this.predictionsUntilNextPoll = predictionsUntilNextPoll;
    }

    public long getPendingWelcome() { return pendingWelcome; }
    public void setPendingWelcome(long pendingWelcome) { this.pendingWelcome = pendingWelcome; }

    public long getPendingDaily() { return pendingDaily; }
    public void setPendingDaily(long pendingDaily) { this.pendingDaily = pendingDaily; }

    public long getPendingPoll() { return pendingPoll; }
    public void setPendingPoll(long pendingPoll) { this.pendingPoll = pendingPoll; }

    public long getSecondsUntilNextDaily() { return secondsUntilNextDaily; }
    public void setSecondsUntilNextDaily(long secondsUntilNextDaily) {
        this.secondsUntilNextDaily = secondsUntilNextDaily;
    }

    public long getTotalPredictions() { return totalPredictions; }
    public void setTotalPredictions(long totalPredictions) { this.totalPredictions = totalPredictions; }

    public long getPredictionsUntilNextPoll() { return predictionsUntilNextPoll; }
    public void setPredictionsUntilNextPoll(long predictionsUntilNextPoll) {
        this.predictionsUntilNextPoll = predictionsUntilNextPoll;
    }
}