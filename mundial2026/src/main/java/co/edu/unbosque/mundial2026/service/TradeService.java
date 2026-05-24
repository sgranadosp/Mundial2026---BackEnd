package co.edu.unbosque.mundial2026.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.NotificationDTO;
import co.edu.unbosque.mundial2026.dto.trade.SimpleStickerDTO;
import co.edu.unbosque.mundial2026.dto.trade.TradeRequestDTO;
import co.edu.unbosque.mundial2026.model.Sticker;
import co.edu.unbosque.mundial2026.model.TradeRejection;
import co.edu.unbosque.mundial2026.model.TradeRequest;
import co.edu.unbosque.mundial2026.model.TradeRequest.Status;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.User.Role;
import co.edu.unbosque.mundial2026.model.UserSticker;
import co.edu.unbosque.mundial2026.repository.StickerRepository;
import co.edu.unbosque.mundial2026.repository.TradeRejectionRepository;
import co.edu.unbosque.mundial2026.repository.TradeRequestRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.repository.UserStickerRepository;

/**
 * Servicio que encapsula toda la lógica de negocio de los intercambios
 * de láminas entre usuarios del Mundial 2026 Hub.
 *
 * <h3>Reglas críticas del proyecto</h3>
 * <ul>
 *   <li>Una solicitud OFRECE una lámina REPETIDA del creador y PIDE una
 *       lámina FALTANTE en su álbum.</li>
 *   <li>Solo otros usuarios con rol USER (no admins) ven la solicitud y
 *       pueden aceptarla.</li>
 *   <li>Para aceptar, el usuario que acepta DEBE tener la lámina pedida
 *       como REPETIDA. Si no, retorna 4 (no permitido).</li>
 *   <li>Si el creador ya no posee la ofrecida como REPETIDA (por otro
 *       intercambio o evento), la solicitud se auto-cancela.</li>
 *   <li>Si el creador obtuvo la pedida por otra vía (pack u otro
 *       intercambio), la solicitud se auto-cancela.</li>
 *   <li>Un usuario puede tener máximo 5 solicitudes activas a la vez.</li>
 *   <li>Al completar el intercambio, se notifica al creador por email
 *       (HTML) e in-app (NotificationService).</li>
 * </ul>
 *
 * <h3>Códigos de retorno</h3>
 * <ul>
 *   <li>0 — éxito.</li>
 *   <li>1 — duplicado / restricción de negocio (ya rechazó, ya tiene 5 activas).</li>
 *   <li>2 — entidad no encontrada (lámina, usuario, solicitud).</li>
 *   <li>3 — datos inválidos.</li>
 *   <li>4 — operación no permitida en el estado actual (no eres elegible).</li>
 * </ul>
 */
@Service
public class TradeService {

    /** Máximo de solicitudes activas por usuario simultáneamente. */
    public static final int MAX_ACTIVE_REQUESTS_PER_USER = 5;

    private static final String IMAGE_URL_PREFIX = "/laminas/";

    @Autowired private TradeRequestRepository tradeRepo;
    @Autowired private TradeRejectionRepository rejectionRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StickerRepository stickerRepo;
    @Autowired private UserStickerRepository userStickerRepo;
    @Autowired private NotificationService notificationService;
    @Autowired private EmailService emailService;

    public TradeService() {}

    // =========================================================================
    // Crear solicitud
    // =========================================================================

    /**
     * Crea una nueva solicitud de intercambio del usuario autenticado.
     *
     * Validaciones:
     *  1. Las dos láminas existen.
     *  2. El usuario tiene la {@code offeredStickerId} con quantity >= 2 (REPETIDA).
     *  3. El usuario NO tiene la {@code requestedStickerId} en su álbum (FALTANTE).
     *  4. Las dos láminas son distintas.
     *  5. El usuario tiene menos de 5 solicitudes activas.
     *
     * @return código 0 si éxito; 1 si ya tiene 5; 2 si láminas/usuario no existen;
     *         3 si láminas iguales; 4 si no cumple precondiciones (no tiene
     *         repetida la ofrecida o ya tiene la pedida).
     */
    @Transactional
    public int createRequest(Long creatorId, Long offeredStickerId, Long requestedStickerId) {
        if (offeredStickerId == null || requestedStickerId == null) {
            return 3;
        }
        if (offeredStickerId.equals(requestedStickerId)) {
            return 3;
        }

        Optional<User> creatorOpt = userRepo.findById(creatorId);
        Optional<Sticker> offeredOpt = stickerRepo.findById(offeredStickerId);
        Optional<Sticker> requestedOpt = stickerRepo.findById(requestedStickerId);
        if (creatorOpt.isEmpty() || offeredOpt.isEmpty() || requestedOpt.isEmpty()) {
            return 2;
        }
        User creator = creatorOpt.get();

        // Validar límite de 5 activas.
        long active = tradeRepo.countByCreatorIdAndStatus(creatorId, Status.PENDING);
        if (active >= MAX_ACTIVE_REQUESTS_PER_USER) {
            return 1;
        }

        // Validar que el creador tiene la ofrecida como REPETIDA (quantity >= 2).
        Optional<UserSticker> offeredOwnedOpt =
                userStickerRepo.findByUserAndSticker(creator, offeredOpt.get());
        if (offeredOwnedOpt.isEmpty() || offeredOwnedOpt.get().getQuantity() < 2) {
            return 4;
        }

        // Validar que el creador NO tiene la pedida (FALTANTE).
        Optional<UserSticker> requestedOwnedOpt =
                userStickerRepo.findByUserAndSticker(creator, requestedOpt.get());
        if (requestedOwnedOpt.isPresent()) {
            return 4;
        }

        TradeRequest tr = new TradeRequest(creator, offeredOpt.get(), requestedOpt.get());
        tradeRepo.save(tr);
        return 0;
    }

    // =========================================================================
    // Listar solicitudes
    // =========================================================================

    /**
     * Lista las solicitudes activas que el usuario PUEDE ver en su bandeja
     * de "recibidas" (excluye sus propias, y las que rechazó).
     */
    public List<TradeRequestDTO> getReceivedByUser(Long userId) {
        List<TradeRequest> list = tradeRepo.findReceivedForUser(userId);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Lista las solicitudes activas creadas por el usuario.
     */
    public List<TradeRequestDTO> getMyRequests(Long userId) {
        List<TradeRequest> list =
                tradeRepo.findByCreatorIdAndStatus(userId, Status.PENDING);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // =========================================================================
    // Rechazar solicitud (oculta solo para ese usuario)
    // =========================================================================

    /**
     * Marca como rechazada una solicitud para UN usuario concreto.
     * La solicitud sigue activa para los demás.
     *
     * @return 0 si éxito; 1 si ya estaba rechazada; 2 si no existe;
     *         4 si está cerrada (COMPLETED/CANCELLED) o si el usuario es el creador.
     */
    @Transactional
    public int rejectRequest(Long tradeRequestId, Long userId) {
        Optional<TradeRequest> trOpt = tradeRepo.findById(tradeRequestId);
        Optional<User> userOpt = userRepo.findById(userId);
        if (trOpt.isEmpty() || userOpt.isEmpty()) {
            return 2;
        }
        TradeRequest tr = trOpt.get();
        if (tr.getStatus() != Status.PENDING) {
            return 4;
        }
        if (tr.getCreator().getId().equals(userId)) {
            return 4; // el creador no puede rechazar su propia solicitud
        }
        if (rejectionRepo.existsByUserIdAndTradeRequestId(userId, tradeRequestId)) {
            return 1;
        }
        rejectionRepo.save(new TradeRejection(userOpt.get(), tr));
        return 0;
    }

    // =========================================================================
    // Aceptar solicitud (TRANSACCIÓN COMPLETA)
    // =========================================================================

    /**
     * Acepta una solicitud y ejecuta el intercambio atómicamente.
     *
     * <h3>Validaciones secuenciales</h3>
     * <ol>
     *   <li>La solicitud existe y está PENDING.</li>
     *   <li>El usuario que acepta NO es el creador.</li>
     *   <li>El usuario que acepta tiene la lámina PEDIDA como REPETIDA
     *       (quantity ≥ 2). Si no → retorna 4.</li>
     *   <li>El creador AÚN tiene la lámina OFRECIDA como REPETIDA
     *       (quantity ≥ 2). Si no → auto-cancela la solicitud y retorna 4.</li>
     *   <li>El creador AÚN NO tiene la lámina PEDIDA en su álbum.
     *       Si la obtuvo por otra vía → auto-cancela y retorna 4.</li>
     * </ol>
     *
     * <h3>Transferencia atómica</h3>
     * <ul>
     *   <li>Creador: quantity de OFRECIDA --, quantity de PEDIDA ++.</li>
     *   <li>Aceptante: quantity de PEDIDA --, quantity de OFRECIDA ++.</li>
     *   <li>Se hacen los UPSERTs respetando si la fila ya existía o no.</li>
     * </ul>
     *
     * <h3>Post-procesamiento</h3>
     * <ul>
     *   <li>Notificar al creador por email + in-app.</li>
     *   <li>Revisar otras solicitudes activas del creador que ofrecían la
     *       misma lámina o que pedían la misma lámina, y auto-cancelar si
     *       ya no se cumplen las precondiciones.</li>
     *   <li>Aplicar el mismo chequeo para el aceptante.</li>
     * </ul>
     *
     * @return código 0 si éxito; 2 si no existe; 4 si no permitido (con
     *         efectos colaterales de auto-cancelación según el caso).
     */
    @Transactional
    public int acceptRequest(Long tradeRequestId, Long accepterUserId) {
        Optional<TradeRequest> trOpt = tradeRepo.findById(tradeRequestId);
        Optional<User> accepterOpt = userRepo.findById(accepterUserId);
        if (trOpt.isEmpty() || accepterOpt.isEmpty()) {
            return 2;
        }
        TradeRequest tr = trOpt.get();
        User accepter = accepterOpt.get();

        if (tr.getStatus() != Status.PENDING) {
            return 4;
        }
        User creator = tr.getCreator();
        if (creator.getId().equals(accepterUserId)) {
            return 4; // no puedes aceptar tu propia
        }

        Sticker offered = tr.getOfferedSticker();    // lo da el creador, lo recibe el aceptante
        Sticker requested = tr.getRequestedSticker(); // lo da el aceptante, lo recibe el creador

        // Validación 1: aceptante tiene la pedida como REPETIDA.
        Optional<UserSticker> accReqOpt =
                userStickerRepo.findByUserAndSticker(accepter, requested);
        if (accReqOpt.isEmpty() || accReqOpt.get().getQuantity() < 2) {
            return 4; // no cumple precondición; la solicitud sigue activa para otros
        }

        // Validación 2: el creador aún tiene la ofrecida como REPETIDA.
        Optional<UserSticker> creatorOffOpt =
                userStickerRepo.findByUserAndSticker(creator, offered);
        if (creatorOffOpt.isEmpty() || creatorOffOpt.get().getQuantity() < 2) {
            // Auto-cancelar: el creador ya no puede ofrecer esa lámina.
            tr.markCancelled("El creador ya no posee la lámina ofrecida como repetida");
            tradeRepo.save(tr);
            return 4;
        }

        // Validación 3: el creador todavía NO tiene la pedida.
        Optional<UserSticker> creatorReqOpt =
                userStickerRepo.findByUserAndSticker(creator, requested);
        if (creatorReqOpt.isPresent()) {
            // Auto-cancelar: el creador ya obtuvo la lámina pedida por otra vía.
            tr.markCancelled("El creador ya obtuvo la lámina pedida por otra vía");
            tradeRepo.save(tr);
            return 4;
        }

        // ─── TRANSFERENCIA ATÓMICA ───────────────────────────────────────────
        // Creador entrega "offered" y recibe "requested".
        UserSticker creatorOff = creatorOffOpt.get();
        creatorOff.setQuantity(creatorOff.getQuantity() - 1); // si era 2 queda 1; si era 3 queda 2; ...
        userStickerRepo.save(creatorOff);
        // El creador recibe la "requested" (no la tenía antes): crear fila con quantity=1.
        userStickerRepo.save(new UserSticker(creator, requested));

        // Aceptante entrega "requested" y recibe "offered".
        UserSticker accReq = accReqOpt.get();
        accReq.setQuantity(accReq.getQuantity() - 1);
        userStickerRepo.save(accReq);
        // El aceptante recibe la "offered". Si ya la tenía, incrementa; si no, crea.
        Optional<UserSticker> accOffOpt =
                userStickerRepo.findByUserAndSticker(accepter, offered);
        if (accOffOpt.isPresent()) {
            UserSticker accOff = accOffOpt.get();
            accOff.incrementarCantidad();
            userStickerRepo.save(accOff);
        } else {
            userStickerRepo.save(new UserSticker(accepter, offered));
        }

        // Marcar la solicitud como completada.
        tr.markCompleted(accepter);
        tradeRepo.save(tr);

        // ─── Post-procesamiento: notificaciones y auto-cancelación ──────────
        notifyTradeCompleted(creator, accepter, offered, requested);
        cleanupAffectedRequests(creator, offered, requested);
        cleanupAffectedRequests(accepter, requested, offered);

        return 0;
    }

    // =========================================================================
    // Auto-cancelación de solicitudes afectadas por un evento
    // =========================================================================

    /**
     * Revisa las solicitudes activas del usuario y las cancela si dejaron
     * de cumplir las precondiciones tras un evento (intercambio completado,
     * pack abierto, etc.).
     *
     * @param user           Usuario cuyas solicitudes activas se revisan.
     * @param stickerLost    Lámina que el usuario perdió/disminuyó en quantity
     *                       (puede ser null si no aplica).
     * @param stickerGained  Lámina que el usuario ganó/obtuvo
     *                       (puede ser null si no aplica).
     */
    @Transactional
    public void cleanupAffectedRequests(User user, Sticker stickerLost, Sticker stickerGained) {
        // Caso 1: la lámina que perdió era la que ofrecía en alguna solicitud.
        //         Si ahora no la tiene como repetida → cancelar.
        if (stickerLost != null) {
            List<TradeRequest> offering =
                    tradeRepo.findActiveByCreatorAndOfferedSticker(user.getId(), stickerLost.getId());
            for (TradeRequest tr : offering) {
                Optional<UserSticker> us = userStickerRepo.findByUserAndSticker(user, stickerLost);
                if (us.isEmpty() || us.get().getQuantity() < 2) {
                    tr.markCancelled("Ya no posees la lámina ofrecida como repetida");
                    tradeRepo.save(tr);
                }
            }
        }

        // Caso 2: la lámina que ganó era la que pedía en alguna solicitud.
        //         Si ya la tiene en su álbum → cancelar.
        if (stickerGained != null) {
            List<TradeRequest> requesting =
                    tradeRepo.findActiveByCreatorAndRequestedSticker(user.getId(), stickerGained.getId());
            for (TradeRequest tr : requesting) {
                Optional<UserSticker> us = userStickerRepo.findByUserAndSticker(user, stickerGained);
                if (us.isPresent()) {
                    tr.markCancelled("Ya obtuviste la lámina pedida por otra vía");
                    tradeRepo.save(tr);
                }
            }
        }
    }

    /**
     * Hook genérico para invocar después de eventos que modifican la
     * colección del usuario (apertura de packs, intercambios externos).
     * Cancela cualquier solicitud activa que haya quedado inválida.
     */
    @Transactional
    public void onUserCollectionChanged(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();

        List<TradeRequest> mine =
                tradeRepo.findByCreatorIdAndStatus(userId, Status.PENDING);
        for (TradeRequest tr : mine) {
            // Si ya no tiene la ofrecida como repetida → cancelar.
            Optional<UserSticker> off =
                    userStickerRepo.findByUserAndSticker(user, tr.getOfferedSticker());
            if (off.isEmpty() || off.get().getQuantity() < 2) {
                tr.markCancelled("Ya no posees la lámina ofrecida como repetida");
                tradeRepo.save(tr);
                continue;
            }
            // Si ya obtuvo la pedida → cancelar.
            Optional<UserSticker> req =
                    userStickerRepo.findByUserAndSticker(user, tr.getRequestedSticker());
            if (req.isPresent()) {
                tr.markCancelled("Ya obtuviste la lámina pedida por otra vía");
                tradeRepo.save(tr);
            }
        }
    }

    // =========================================================================
    // Autocompletes: páginas con FALTANTE / REPETIDA del usuario
    // =========================================================================

    /**
     * Devuelve los códigos de país DISTINTOS donde el usuario tiene al
     * menos una lámina REPETIDA. Sirve para alimentar el primer autocomplete
     * de "Lámina a ofrecer".
     *
     * Estructura de la respuesta: { code → name }.
     */
    public Map<String, String> getCountriesWithRepetidas(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return new HashMap<>();
        User user = userOpt.get();

        List<UserSticker> all = userStickerRepo.findByUser(user);
        Map<String, String> result = new HashMap<>();
        for (UserSticker us : all) {
            if (us.getQuantity() >= 2) {
                result.putIfAbsent(us.getSticker().getCountryCode(),
                                   us.getSticker().getCountryName());
            }
        }
        return result;
    }

    /**
     * Devuelve los códigos de país donde el usuario tiene al menos una
     * lámina FALTANTE. Sirve para el primer autocomplete de "Lámina deseada".
     *
     * En la práctica casi todos los países tendrán algo faltante (a menos
     * que el usuario haya completado una página entera), así que la query
     * devuelve todos menos los completamente llenos.
     */
    public Map<String, String> getCountriesWithFaltantes(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return new HashMap<>();
        User user = userOpt.get();

        // Catálogo completo agrupado por país (con su nombre).
        Map<String, String> allCountries = new HashMap<>();
        Map<String, Integer> catalogCount = new HashMap<>();
        for (Sticker s : stickerRepo.findAll()) {
            allCountries.put(s.getCountryCode(), s.getCountryName());
            catalogCount.merge(s.getCountryCode(), 1, Integer::sum);
        }

        // Conteo de láminas distintas que el usuario tiene por país.
        Map<String, Integer> userCount = new HashMap<>();
        for (UserSticker us : userStickerRepo.findByUser(user)) {
            userCount.merge(us.getSticker().getCountryCode(), 1, Integer::sum);
        }

        // Quedarse con los países donde tiene MENOS láminas que el catálogo.
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Integer> e : catalogCount.entrySet()) {
            int total = e.getValue();
            int owned = userCount.getOrDefault(e.getKey(), 0);
            if (owned < total) {
                result.put(e.getKey(), allCountries.get(e.getKey()));
            }
        }
        return result;
    }

    /**
     * Devuelve las láminas REPETIDAS del usuario en una selección específica
     * (las que tiene con quantity ≥ 2). Para el segundo autocomplete.
     */
    public List<SimpleStickerDTO> getRepetidasInCountry(Long userId, String countryCode) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return new ArrayList<>();
        User user = userOpt.get();

        List<UserSticker> all =
                userStickerRepo.findByUserAndSticker_CountryCode(user, countryCode);
        List<SimpleStickerDTO> result = new ArrayList<>();
        for (UserSticker us : all) {
            if (us.getQuantity() >= 2) {
                Sticker s = us.getSticker();
                result.add(new SimpleStickerDTO(
                        s.getId(), s.getCode(), s.getCountryCode(), s.getCountryName(),
                        s.getPosition(),
                        IMAGE_URL_PREFIX + s.getCode() + ".png",
                        us.getQuantity()));
            }
        }
        result.sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
        return result;
    }

    /**
     * Devuelve las láminas FALTANTES del usuario en una selección específica.
     */
    public List<SimpleStickerDTO> getFaltantesInCountry(Long userId, String countryCode) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return new ArrayList<>();
        User user = userOpt.get();

        List<Sticker> catalog =
                stickerRepo.findByCountryCodeOrderByPositionAsc(countryCode);
        List<UserSticker> owned =
                userStickerRepo.findByUserAndSticker_CountryCode(user, countryCode);
        Map<Long, UserSticker> ownedById = new HashMap<>();
        for (UserSticker us : owned) {
            ownedById.put(us.getSticker().getId(), us);
        }

        List<SimpleStickerDTO> result = new ArrayList<>();
        for (Sticker s : catalog) {
            if (!ownedById.containsKey(s.getId())) {
                result.add(new SimpleStickerDTO(
                        s.getId(), s.getCode(), s.getCountryCode(), s.getCountryName(),
                        s.getPosition(),
                        IMAGE_URL_PREFIX + s.getCode() + ".png",
                        0));
            }
        }
        return result;
    }

    // =========================================================================
    // Notificaciones al completar
    // =========================================================================

    /**
     * Notifica al creador de la solicitud que su intercambio se completó.
     * - In-app: vía {@link NotificationService#sendToUser(NotificationDTO)}.
     * - Email: vía {@link EmailService#enviarIntercambioCompletado}.
     */
    private void notifyTradeCompleted(User creator, User accepter,
                                       Sticker offered, Sticker requested) {
        // Notif al CREADOR del intercambio: alguien aceptó su propuesta.
        try {
            String title = "¡Intercambio completado!";
            String body = accepter.getUsername()
                    + " aceptó tu solicitud. Recibiste \""
                    + requested.getCountryName() + " — posición " + requested.getPosition()
                    + "\" y entregaste \""
                    + offered.getCountryName() + " — posición " + offered.getPosition() + "\".";

            NotificationDTO dto = new NotificationDTO(
                    creator.getId(), title, body, "IN_APP", "EXCHANGE_COMPLETED");
            notificationService.sendToUser(dto);
        } catch (Exception e) {
            System.err.println("[TradeService] Error notif in-app creator: " + e.getMessage());
        }

        // Notif al ACCEPTER del intercambio: el cambio se completó por su lado.
        // Se le informa qué lámina entregó y cuál recibió, en su propia
        // perspectiva (entregó "requested" y recibió "offered" — es el
        // espejo del creator).
        try {
            String title = "¡Intercambio completado!";
            String body = "Completaste el intercambio con "
                    + creator.getUsername() + ". Recibiste \""
                    + offered.getCountryName() + " — posición " + offered.getPosition()
                    + "\" y entregaste \""
                    + requested.getCountryName() + " — posición " + requested.getPosition() + "\".";

            NotificationDTO dto = new NotificationDTO(
                    accepter.getId(), title, body, "IN_APP", "EXCHANGE_COMPLETED");
            notificationService.sendToUser(dto);
        } catch (Exception e) {
            System.err.println("[TradeService] Error notif in-app accepter: " + e.getMessage());
        }

        try {
            if (creator.isEmailNotificationsEnabled() && creator.getEmail() != null) {
                emailService.enviarIntercambioCompletado(
                        creator.getEmail(),
                        creator.getName() != null ? creator.getName() : creator.getUsername(),
                        accepter.getUsername(),
                        offered.getCountryName() + " — posición " + offered.getPosition(),
                        requested.getCountryName() + " — posición " + requested.getPosition()
                );
            }
        } catch (Exception e) {
            System.err.println("[TradeService] Error enviando email: " + e.getMessage());
        }
    }

    // =========================================================================
    // Mapper privado
    // =========================================================================

    private TradeRequestDTO toDTO(TradeRequest tr) {
        TradeRequestDTO dto = new TradeRequestDTO();
        dto.setId(tr.getId());
        dto.setCreatorId(tr.getCreator().getId());
        dto.setCreatorUsername(tr.getCreator().getUsername());

        Sticker off = tr.getOfferedSticker();
        dto.setOfferedStickerId(off.getId());
        dto.setOfferedStickerCode(off.getCode());
        dto.setOfferedStickerCountryName(off.getCountryName());
        dto.setOfferedStickerPosition(off.getPosition());
        dto.setOfferedStickerImageUrl(IMAGE_URL_PREFIX + off.getCode() + ".png");

        Sticker req = tr.getRequestedSticker();
        dto.setRequestedStickerId(req.getId());
        dto.setRequestedStickerCode(req.getCode());
        dto.setRequestedStickerCountryName(req.getCountryName());
        dto.setRequestedStickerPosition(req.getPosition());
        dto.setRequestedStickerImageUrl(IMAGE_URL_PREFIX + req.getCode() + ".png");

        dto.setStatus(tr.getStatus());
        dto.setCreatedAt(tr.getCreatedAt());
        return dto;
    }
}