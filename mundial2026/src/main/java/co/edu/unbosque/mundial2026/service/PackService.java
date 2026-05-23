package co.edu.unbosque.mundial2026.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.album.StickerSlotDTO;
import co.edu.unbosque.mundial2026.dto.album.StickerSlotDTO.Status;
import co.edu.unbosque.mundial2026.dto.pack.OpenPackResultDTO;
import co.edu.unbosque.mundial2026.dto.pack.PackHistoryEntryDTO;
import co.edu.unbosque.mundial2026.dto.pack.PackInventoryDTO;
import co.edu.unbosque.mundial2026.model.Sticker;
import co.edu.unbosque.mundial2026.model.StickerPack;
import co.edu.unbosque.mundial2026.model.StickerPack.Origin;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.UserSticker;
import co.edu.unbosque.mundial2026.repository.PredictionRepository;
import co.edu.unbosque.mundial2026.repository.StickerPackRepository;
import co.edu.unbosque.mundial2026.repository.StickerRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.repository.UserStickerRepository;

/**
 * Servicio que encapsula la lógica de negocio del módulo de Packs.
 *
 * <h3>Reglas principales</h3>
 * <ul>
 *   <li><b>Primer login</b>: al loguearse por primera vez, el usuario recibe
 *       3 packs WELCOME y se inicializa el contador de pack DAILY.</li>
 *   <li><b>Pack diario</b>: cada 12 horas desde {@code dailyPackTimerStartedAt}
 *       se otorga un pack DAILY y el contador se reinicia. Si pasaron más de
 *       24h (por ejemplo, el usuario no abrió la app), se otorgan varios packs
 *       acumulados (uno por cada bloque completo de 12h).</li>
 *   <li><b>Pack por pronósticos</b>: se otorga 1 pack POLL por cada bloque
 *       completo de 6 predicciones. Si el usuario tiene 14 predicciones y
 *       solo ha recibido 1 pack POLL (de las primeras 6), el sistema otorga
 *       el segundo (por llegar a 12) en el siguiente refresh.</li>
 *   <li><b>Acumulación</b>: todos los packs sin abrir se acumulan en
 *       {@code sticker_packs} con {@code opened=false} y persisten entre
 *       sesiones.</li>
 * </ul>
 *
 * <h3>Patrón de llamada</h3>
 * El front llama a {@link #refreshAndGetInventory(Long)} al cargar
 * {@code /PacksLaminas}. Este método:
 *   1. Si {@code firstLoginAt} es null (primer login O usuario legado sin migrar):
 *      otorga 3 WELCOME e inicializa los timers.
 *   2. Otorga todos los packs DAILY que se hayan ganado desde el último timer.
 *   3. Otorga todos los packs POLL pendientes (basado en {@code totalPredictions / 6}).
 *   4. Devuelve el inventario completo (pendientes por tipo, segundos al siguiente daily, etc.).
 */
@Service
public class PackService {

    /** Número de láminas que entrega cada paquete. */
    public static final int STICKERS_PER_PACKAGE = 5;

    /** Cantidad de packs de bienvenida que recibe un usuario nuevo. */
    public static final int WELCOME_PACKS_AMOUNT = 3;

    /** Cooldown del pack diario en horas. */
    public static final int DAILY_PACK_COOLDOWN_HOURS = 12;

    /** Cada cuántos pronósticos se otorga un pack POLL. */
    public static final int PREDICTIONS_PER_POLL_PACK = 6;

    private static final String IMAGE_URL_PREFIX = "/laminas/";

    private final Random random = new Random();

    @Autowired private StickerPackRepository packRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StickerRepository stickerRepo;
    @Autowired private UserStickerRepository userStickerRepo;
    @Autowired private PredictionRepository predictionRepo;

    public PackService() { }

    // =========================================================================
    // ENTRY POINT principal: refrescar inventario
    // =========================================================================

    /**
     * Punto de entrada principal del módulo. El front llama a este método al
     * cargar la página de Packs. El método aplica todas las reglas de negocio
     * (bienvenida, daily, poll) y devuelve el inventario actualizado.
     *
     * @param userId ID del usuario autenticado.
     * @return Inventario actualizado, o {@code null} si el usuario no existe.
     */
    @Transactional
    public PackInventoryDTO refreshAndGetInventory(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();

        // 1. Primer login (o migración de usuarios legados).
        ensureFirstLoginInitialized(user);

        // 2. Otorgar todos los packs DAILY que correspondan según el cooldown.
        grantPendingDailyPacks(user);

        // 3. Otorgar todos los packs POLL pendientes.
        grantPendingPollPacks(user);

        // 4. Construir y devolver el inventario actualizado.
        return buildInventory(user);
    }

    // =========================================================================
    // Otorgar packs (idempotente, lo llama el refresh)
    // =========================================================================

    /**
     * Detecta el primer login del usuario y, si aplica, otorga los 3 packs
     * de bienvenida e inicializa el timer del pack diario.
     *
     * Para usuarios legados (que ya estaban en BD antes de añadir el campo
     * {@code firstLoginAt}), también se ejecuta una sola vez: la primera
     * vez que entren a la página de Packs reciben los 3 WELCOME.
     */
    private void ensureFirstLoginInitialized(User user) {
        if (user.getFirstLoginAt() != null) {
            return; // ya inicializado
        }
        // Validación adicional: evitar duplicación si por algún motivo
        // hay ya packs WELCOME registrados (defensivo, no debería pasar).
        long existingWelcome = packRepo.countByUserIdAndOrigin(user.getId(), Origin.WELCOME);
        if (existingWelcome >= WELCOME_PACKS_AMOUNT) {
            // Caso raro: usuario tiene los packs pero por algún motivo no se
            // guardó firstLoginAt. Reparamos seteando solo las fechas.
            LocalDateTime now = LocalDateTime.now();
            user.setFirstLoginAt(now);
            user.setDailyPackTimerStartedAt(now);
            userRepo.save(user);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        user.setFirstLoginAt(now);
        user.setDailyPackTimerStartedAt(now);
        userRepo.save(user);

        for (int i = 0; i < WELCOME_PACKS_AMOUNT; i++) {
            packRepo.save(new StickerPack(user, Origin.WELCOME));
        }
    }

    /**
     * Otorga todos los packs DAILY que el usuario haya "ganado" desde su
     * {@code dailyPackTimerStartedAt}. Si el usuario no abrió la app por
     * muchas horas, puede recibir varios packs acumulados (uno por cada
     * bloque completo de 12h).
     *
     * Tras otorgar N packs, el timer avanza N*12 horas (no se "pierden"
     * fracciones), de modo que el contador siempre es preciso.
     */
    private void grantPendingDailyPacks(User user) {
        if (user.getDailyPackTimerStartedAt() == null) {
            return; // por seguridad
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timerStart = user.getDailyPackTimerStartedAt();
        long hoursElapsed = Duration.between(timerStart, now).toHours();

        if (hoursElapsed < DAILY_PACK_COOLDOWN_HOURS) {
            return; // aún en cooldown
        }

        long packsToGrant = hoursElapsed / DAILY_PACK_COOLDOWN_HOURS;
        for (int i = 0; i < packsToGrant; i++) {
            packRepo.save(new StickerPack(user, Origin.DAILY));
        }

        // Avanzar el timer exactamente N*12 horas (sin perder fracciones).
        LocalDateTime newTimer = timerStart.plusHours(packsToGrant * DAILY_PACK_COOLDOWN_HOURS);
        user.setDailyPackTimerStartedAt(newTimer);
        userRepo.save(user);
    }

    /**
     * Otorga todos los packs POLL pendientes según el total de pronósticos
     * del usuario. La regla es: 1 pack por cada bloque completo de 6
     * pronósticos. Si el usuario tiene N pronósticos pero solo ha recibido
     * M packs POLL, el sistema otorga {@code (N/6) - M} packs.
     */
    private void grantPendingPollPacks(User user) {
        long totalPredictions = predictionRepo.findByUserId(user.getId()).size();
        long expectedPackCount = totalPredictions / PREDICTIONS_PER_POLL_PACK;
        long alreadyGranted = packRepo.countPollPacksByUser(user.getId());
        long packsToGrant = expectedPackCount - alreadyGranted;
        if (packsToGrant <= 0) {
            return;
        }
        for (int i = 0; i < packsToGrant; i++) {
            packRepo.save(new StickerPack(user, Origin.POLL));
        }
    }

    // =========================================================================
    // Hook para que PollService llame al registrar una predicción
    // =========================================================================

    /**
     * Hook que el PollService invoca cada vez que se registra exitosamente
     * un pronóstico nuevo. Verifica si el usuario alcanzó un nuevo múltiplo
     * de 6 y, en ese caso, otorga el pack POLL correspondiente.
     *
     * Es idempotente: si por alguna razón se llama dos veces para el mismo
     * pronóstico, no se otorgan packs extra (la comparación es con
     * {@code totalPredictions / 6} vs packs ya otorgados).
     *
     * @param userId ID del usuario que acaba de registrar la predicción.
     */
    @Transactional
    public void onPredictionSubmitted(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return;
        grantPendingPollPacks(userOpt.get());
    }

    // =========================================================================
    // Abrir un pack
    // =========================================================================

    /**
     * Abre un pack del origen indicado.
     *
     * Selecciona el pack PENDIENTE más antiguo (FIFO) de ese origen, lo marca
     * como abierto, genera 5 láminas aleatorias del catálogo y las acumula
     * en la colección del usuario (creando UserSticker o incrementando
     * quantity según corresponda).
     *
     * @param userId ID del usuario.
     * @param origin Origen del pack a abrir (WELCOME / DAILY / POLL).
     * @return Resultado con las láminas obtenidas y el inventario actualizado,
     *         o {@code null} si el usuario no existe o no tiene packs
     *         pendientes de ese origen.
     */
    @Transactional
    public OpenPackResultDTO openPack(Long userId, Origin origin) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();

        Optional<StickerPack> packOpt = packRepo
                .findFirstByUserIdAndOriginAndOpenedFalseOrderByGrantedAtAsc(userId, origin);
        if (packOpt.isEmpty()) {
            return null; // no hay packs pendientes de ese origen
        }
        StickerPack pack = packOpt.get();

        // 1. Marcar como abierto.
        pack.markOpened();
        packRepo.save(pack);

        // 2. Sortear y entregar 5 láminas.
        List<Sticker> catalog = stickerRepo.findAll();
        if (catalog.isEmpty()) {
            // No hay catálogo: devolver vacío con inventario actualizado.
            return new OpenPackResultDTO(origin, new ArrayList<>(), buildInventory(user));
        }

        List<StickerSlotDTO> obtained = new ArrayList<>();
        for (int i = 0; i < STICKERS_PER_PACKAGE; i++) {
            Sticker drawn = catalog.get(random.nextInt(catalog.size()));

            // Aplicar acumulación con la misma lógica del AlbumService.
            Optional<UserSticker> existingOpt =
                    userStickerRepo.findByUserAndSticker(user, drawn);
            UserSticker record;
            if (existingOpt.isPresent()) {
                record = existingOpt.get();
                record.incrementarCantidad();
            } else {
                record = new UserSticker(user, drawn);
            }
            record = userStickerRepo.save(record);

            Status status = record.getQuantity() >= 2 ? Status.REPETIDA : Status.PEGADA;
            StickerSlotDTO slot = new StickerSlotDTO(
                    drawn.getCode(), drawn.getPosition(),
                    IMAGE_URL_PREFIX + drawn.getCode() + ".png",
                    status, record.getQuantity());
            obtained.add(slot);
        }

        return new OpenPackResultDTO(origin, obtained, buildInventory(user));
    }

    // =========================================================================
    // Historial
    // =========================================================================

    /**
     * Lista todos los packs (abiertos y pendientes) del usuario, ordenados
     * por fecha de otorgamiento descendente (los más recientes primero).
     */
    public List<PackHistoryEntryDTO> getHistory(Long userId) {
        List<StickerPack> all = packRepo.findByUserIdOrderByGrantedAtDesc(userId);
        List<PackHistoryEntryDTO> result = new ArrayList<>();
        for (StickerPack p : all) {
            result.add(new PackHistoryEntryDTO(
                    p.getId(), p.getOrigin(), p.getGrantedAt(),
                    p.isOpened(), p.getOpenedAt()));
        }
        return result;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Construye el DTO de inventario con los contadores actuales y el
     * tiempo restante para el siguiente pack diario.
     */
    private PackInventoryDTO buildInventory(User user) {
        long pendingWelcome = packRepo.countByUserIdAndOriginAndOpenedFalse(
                user.getId(), Origin.WELCOME);
        long pendingDaily = packRepo.countByUserIdAndOriginAndOpenedFalse(
                user.getId(), Origin.DAILY);
        long pendingPoll = packRepo.countByUserIdAndOriginAndOpenedFalse(
                user.getId(), Origin.POLL);

        // Tiempo restante para el próximo pack diario.
        long secondsUntilDaily = 0;
        if (user.getDailyPackTimerStartedAt() != null) {
            LocalDateTime nextDailyAt = user.getDailyPackTimerStartedAt()
                    .plusHours(DAILY_PACK_COOLDOWN_HOURS);
            long seconds = Duration.between(LocalDateTime.now(), nextDailyAt).getSeconds();
            secondsUntilDaily = Math.max(0, seconds);
        }

        // Pronósticos totales y los que faltan para el próximo pack POLL.
        long totalPredictions = predictionRepo.findByUserId(user.getId()).size();
        long remainder = totalPredictions % PREDICTIONS_PER_POLL_PACK;
        long predictionsUntilNextPoll =
                (remainder == 0) ? PREDICTIONS_PER_POLL_PACK : (PREDICTIONS_PER_POLL_PACK - remainder);

        return new PackInventoryDTO(
                pendingWelcome, pendingDaily, pendingPoll,
                secondsUntilDaily, totalPredictions, predictionsUntilNextPoll);
    }
}