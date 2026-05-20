/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.AlbumDTO;
import co.edu.unbosque.mundial2026.dto.StickerDTO;
import co.edu.unbosque.mundial2026.dto.StickerExchangeDTO;
import co.edu.unbosque.mundial2026.dto.StickerPackageDTO;
import co.edu.unbosque.mundial2026.model.Album;
import co.edu.unbosque.mundial2026.model.Sticker;
import co.edu.unbosque.mundial2026.model.Sticker.StickerStatus;
import co.edu.unbosque.mundial2026.model.StickerExchange;
import co.edu.unbosque.mundial2026.model.StickerExchange.ExchangeStatus;
import co.edu.unbosque.mundial2026.model.StickerPackage;
import co.edu.unbosque.mundial2026.model.StickerPackage.PackageSource;
import co.edu.unbosque.mundial2026.model.StickerPackage.PackageStatus;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.AlbumRepository;
import co.edu.unbosque.mundial2026.repository.StickerExchangeRepository;
import co.edu.unbosque.mundial2026.repository.StickerPackageRepository;
import co.edu.unbosque.mundial2026.repository.StickerRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Servicio encargado de la lógica de negocio del módulo de álbum digital
 * en la plataforma Mundial 2026 Hub.
 * Gestiona la colección de láminas de cada usuario: apertura de paquetes,
 * registro de láminas obtenidas, identificación de repetidas, solicitudes
 * de intercambio con confirmación mutua y control de límites antiabusos.
 * No se vende contenido; los paquetes se obtienen solo por actividad o premios.
 * Límite máximo de intercambios activos simultáneos por usuario: {@value #MAX_ACTIVE_EXCHANGES}.
 */
@Service
public class AlbumService {

    /**
     * Número máximo de intercambios activos (PENDING o ACCEPTED) simultáneos
     * permitidos por usuario para prevenir automatización masiva.
     */
    private static final int MAX_ACTIVE_EXCHANGES = 5;

    /**
     * Número total de láminas únicas en el catálogo del álbum 2026.
     * Se usa para calcular el porcentaje de completitud.
     */
    private static final int TOTAL_CATALOG_STICKERS = 300;

    @Autowired
    private AlbumRepository albumRepo;

    @Autowired
    private StickerRepository stickerRepo;

    @Autowired
    private StickerPackageRepository packageRepo;

    @Autowired
    private StickerExchangeRepository exchangeRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public AlbumService() {
    }

    // =========================================================================
    // Álbum
    // =========================================================================

    /**
     * Inicializa un álbum vacío para un usuario recién registrado.
     * Se llama automáticamente en {@link UserService#create} tras el registro.
     *
     * @param userId El ID del nuevo usuario.
     * @return 0 si fue creado; 1 si el usuario ya tiene álbum; 2 si no existe.
     */
    public int initializeAlbum(Long userId) {
        if (albumRepo.existsByUserId(userId)) {
            return 1;
        }
        Optional<User> user = userRepo.findById(userId);
        if (user.isEmpty()) {
            return 2;
        }
        Album album = new Album(user.get());
        albumRepo.save(album);
        return 0;
    }

    /**
     * Obtiene el resumen del álbum de un usuario (sin lista de láminas).
     *
     * @param userId El ID del usuario.
     * @return El {@link AlbumDTO} de resumen, o {@code null} si no existe.
     */
    public AlbumDTO getAlbumSummary(Long userId) {
        Optional<Album> found = albumRepo.findByUserId(userId);
        if (found.isEmpty()) {
            return null;
        }
        Album album = found.get();
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getId());
        dto.setUserId(userId);
        dto.setUsername(album.getUser().getUsername());
        dto.setUniqueStickersCount(album.getUniqueStickersCount());
        dto.setDuplicateStickersCount(album.getDuplicateStickersCount());
        dto.setCompletionPercentage(album.getCompletionPercentage());
        return dto;
    }

    /**
     * Obtiene el álbum completo de un usuario incluyendo todas sus láminas.
     *
     * @param userId El ID del usuario.
     * @return El {@link AlbumDTO} con la lista de láminas, o {@code null} si no existe.
     */
    public AlbumDTO getFullAlbum(Long userId) {
        AlbumDTO dto = getAlbumSummary(userId);
        if (dto == null) return null;

        List<Sticker> stickers = stickerRepo.findByAlbumId(dto.getId());
        List<StickerDTO> stickerDTOs = new ArrayList<>();
        stickers.forEach(s -> stickerDTOs.add(toStickerDTO(s)));
        dto.setStickers(stickerDTOs);
        return dto;
    }

    // =========================================================================
    // Paquetes
    // =========================================================================

    /**
     * Otorga un paquete de láminas a un usuario por una acción específica.
     * Registra el paquete en estado {@code PENDING}.
     *
     * @param userId El ID del usuario.
     * @param source El origen del paquete (acción que lo generó).
     * @return El {@link StickerPackageDTO} creado, o {@code null} si el usuario no existe.
     */
    public StickerPackageDTO grantPackage(Long userId, PackageSource source) {
        Optional<User> user = userRepo.findById(userId);
        if (user.isEmpty()) {
            return null;
        }
        StickerPackage pkg = new StickerPackage(user.get(), source, 5);
        packageRepo.save(pkg);

        StickerPackageDTO dto = new StickerPackageDTO();
        dto.setId(pkg.getId());
        dto.setUserId(userId);
        dto.setSource(source);
        dto.setStatus(PackageStatus.PENDING);
        dto.setStickersCount(5);
        dto.setGrantedAt(pkg.getGrantedAt());
        return dto;
    }

    /**
     * Otorga un paquete usando un código promocional. Valida que el código
     * no haya sido canjeado previamente.
     *
     * @param userId    El ID del usuario.
     * @param promoCode El código promocional.
     * @return El {@link StickerPackageDTO} creado; {@code null} si no existe el usuario;
     *         o un DTO con {@code status = null} si el código ya fue usado.
     */
    public StickerPackageDTO grantPackageByPromoCode(Long userId, String promoCode) {
        if (packageRepo.existsByPromoCode(promoCode)) {
            return null;
        }
        Optional<User> user = userRepo.findById(userId);
        if (user.isEmpty()) {
            return null;
        }
        StickerPackage pkg = new StickerPackage(user.get(), PackageSource.PROMO_CODE, 5);
        pkg.setPromoCode(promoCode);
        packageRepo.save(pkg);

        StickerPackageDTO dto = new StickerPackageDTO();
        dto.setId(pkg.getId());
        dto.setUserId(userId);
        dto.setSource(PackageSource.PROMO_CODE);
        dto.setStatus(PackageStatus.PENDING);
        dto.setStickersCount(5);
        dto.setPromoCode(promoCode);
        dto.setGrantedAt(pkg.getGrantedAt());
        return dto;
    }

    /**
     * Abre un paquete de láminas pendiente. Genera las láminas aleatoriamente,
     * las agrega al álbum del usuario (nuevas como {@code PLACED}, repetidas como
     * {@code DUPLICATE}) y actualiza los contadores del álbum.
     *
     * @param packageId El ID del paquete a abrir.
     * @param userId    El ID del usuario propietario (validación de titularidad).
     * @return El {@link StickerPackageDTO} con la lista de láminas obtenidas;
     *         {@code null} si el paquete no existe, no pertenece al usuario
     *         o ya fue abierto.
     */
    public StickerPackageDTO openPackage(Long packageId, Long userId) {
        Optional<StickerPackage> pkgOpt = packageRepo.findById(packageId);
        if (pkgOpt.isEmpty()) return null;

        StickerPackage pkg = pkgOpt.get();
        if (!pkg.getUser().getId().equals(userId)) return null;
        if (pkg.getStatus() == PackageStatus.OPENED) return null;

        Optional<Album> albumOpt = albumRepo.findByUserId(userId);
        if (albumOpt.isEmpty()) return null;

        Album album = albumOpt.get();
        List<StickerDTO> newStickers = new ArrayList<>();

        for (int i = 0; i < pkg.getStickersCount(); i++) {
            Sticker sticker = generateRandomSticker(album);
            stickerRepo.save(sticker);
            newStickers.add(toStickerDTO(sticker));
        }

        pkg.setStatus(PackageStatus.OPENED);
        pkg.setOpenedAt(LocalDateTime.now());
        packageRepo.save(pkg);

        recalculateAlbumStats(album);

        StickerPackageDTO dto = new StickerPackageDTO();
        dto.setId(pkg.getId());
        dto.setUserId(userId);
        dto.setSource(pkg.getSource());
        dto.setStatus(PackageStatus.OPENED);
        dto.setStickersCount(pkg.getStickersCount());
        dto.setGrantedAt(pkg.getGrantedAt());
        dto.setOpenedAt(pkg.getOpenedAt());
        dto.setNewStickers(newStickers);
        return dto;
    }

    /**
     * Obtiene los paquetes pendientes de un usuario.
     *
     * @param userId El ID del usuario.
     * @return Lista de {@link StickerPackageDTO} pendientes.
     */
    public List<StickerPackageDTO> getPendingPackages(Long userId) {
        List<StickerPackage> packages = packageRepo.findByUserIdAndStatus(userId, PackageStatus.PENDING);
        List<StickerPackageDTO> dtoList = new ArrayList<>();
        packages.forEach(p -> {
            StickerPackageDTO dto = new StickerPackageDTO();
            dto.setId(p.getId());
            dto.setUserId(userId);
            dto.setSource(p.getSource());
            dto.setStatus(p.getStatus());
            dto.setStickersCount(p.getStickersCount());
            dto.setGrantedAt(p.getGrantedAt());
            dtoList.add(dto);
        });
        return dtoList;
    }

    // =========================================================================
    // Intercambios
    // =========================================================================

    /**
     * Crea una solicitud de intercambio entre dos usuarios. Valida que ambas
     * láminas sean repetidas, que no estén ya en un intercambio activo, y que
     * el solicitante no haya superado el límite de intercambios simultáneos.
     *
     * @param data El DTO con los datos del intercambio.
     * @return 0 si fue creado; 1 si el solicitante superó el límite de intercambios;
     *         2 si alguna entidad no existe; 4 si alguna lámina no es apta
     *         para intercambio (no es DUPLICATE o ya está en otro intercambio).
     */
    public int createExchange(StickerExchangeDTO data) {
        Optional<User> requester = userRepo.findById(data.getRequesterId());
        Optional<User> receiver = userRepo.findById(data.getReceiverId());
        Optional<Sticker> offered = stickerRepo.findById(data.getOfferedStickerId());
        Optional<Sticker> requested = stickerRepo.findById(data.getRequestedStickerId());

        if (requester.isEmpty() || receiver.isEmpty() || offered.isEmpty() || requested.isEmpty()) {
            return 2;
        }
        if (offered.get().getStatus() != StickerStatus.DUPLICATE
                || requested.get().getStatus() != StickerStatus.DUPLICATE) {
            return 4;
        }
        if (exchangeRepo.isStickerInActiveExchange(data.getOfferedStickerId())
                || exchangeRepo.isStickerInActiveExchange(data.getRequestedStickerId())) {
            return 4;
        }
        if (exchangeRepo.countActiveExchangesByUserId(data.getRequesterId()) >= MAX_ACTIVE_EXCHANGES) {
            return 1;
        }

        StickerExchange exchange = new StickerExchange(
                requester.get(), receiver.get(), offered.get(), requested.get()
        );
        offered.get().setStatus(StickerStatus.IN_EXCHANGE);
        requested.get().setStatus(StickerStatus.IN_EXCHANGE);
        stickerRepo.save(offered.get());
        stickerRepo.save(requested.get());
        exchangeRepo.save(exchange);
        return 0;
    }

    /**
     * Acepta una solicitud de intercambio. Transfiere las láminas entre los
     * álbumes de los dos usuarios y registra la fecha de resolución.
     *
     * @param exchangeId El ID del intercambio.
     * @param receiverId El ID del usuario receptor que acepta.
     * @return 0 si fue completado; 2 si no existe; 4 si no es el receptor
     *         o el intercambio no está en estado PENDING.
     */
    public int acceptExchange(Long exchangeId, Long receiverId) {
        Optional<StickerExchange> found = exchangeRepo.findById(exchangeId);
        if (found.isEmpty()) return 2;

        StickerExchange exchange = found.get();
        if (!exchange.getReceiver().getId().equals(receiverId)) return 4;
        if (exchange.getStatus() != ExchangeStatus.PENDING) return 4;

        Sticker offered = exchange.getOfferedSticker();
        Sticker requested = exchange.getRequestedSticker();

        offered.setAlbum(exchange.getReceiver().getId().equals(receiverId)
                ? albumRepo.findByUserId(receiverId).orElse(null)
                : offered.getAlbum());
        requested.setAlbum(albumRepo.findByUserId(exchange.getRequester().getId()).orElse(null));

        offered.setStatus(StickerStatus.PLACED);
        requested.setStatus(StickerStatus.PLACED);
        stickerRepo.save(offered);
        stickerRepo.save(requested);

        exchange.setStatus(ExchangeStatus.COMPLETED);
        exchange.setResolvedAt(LocalDateTime.now());
        exchangeRepo.save(exchange);

        albumRepo.findByUserId(exchange.getRequester().getId()).ifPresent(this::recalculateAlbumStats);
        albumRepo.findByUserId(receiverId).ifPresent(this::recalculateAlbumStats);

        return 0;
    }

    /**
     * Cancela o rechaza un intercambio. Libera las láminas bloqueadas
     * (de {@code IN_EXCHANGE} de vuelta a {@code DUPLICATE}).
     *
     * @param exchangeId El ID del intercambio.
     * @param userId     El ID del usuario que cancela o rechaza.
     * @param isRejection {@code true} si es un rechazo del receptor;
     *                    {@code false} si es una cancelación del solicitante.
     * @return 0 si fue cancelado/rechazado; 2 si no existe; 4 si el usuario
     *         no tiene permiso o el estado no lo permite.
     */
    public int cancelOrRejectExchange(Long exchangeId, Long userId, boolean isRejection) {
        Optional<StickerExchange> found = exchangeRepo.findById(exchangeId);
        if (found.isEmpty()) return 2;

        StickerExchange exchange = found.get();
        boolean isRequester = exchange.getRequester().getId().equals(userId);
        boolean isReceiver = exchange.getReceiver().getId().equals(userId);

        if (!isRequester && !isReceiver) return 4;
        if (exchange.getStatus() != ExchangeStatus.PENDING && exchange.getStatus() != ExchangeStatus.ACCEPTED) {
            return 4;
        }

        exchange.getOfferedSticker().setStatus(StickerStatus.DUPLICATE);
        exchange.getRequestedSticker().setStatus(StickerStatus.DUPLICATE);
        stickerRepo.save(exchange.getOfferedSticker());
        stickerRepo.save(exchange.getRequestedSticker());

        exchange.setStatus(isRejection ? ExchangeStatus.REJECTED : ExchangeStatus.CANCELLED);
        exchange.setResolvedAt(LocalDateTime.now());
        exchangeRepo.save(exchange);
        return 0;
    }

    /**
     * Obtiene los intercambios pendientes recibidos por un usuario (solicitudes
     * que debe aceptar o rechazar).
     *
     * @param userId El ID del receptor.
     * @return Lista de {@link StickerExchangeDTO} pendientes para el usuario.
     */
    public List<StickerExchangeDTO> getPendingExchangesForReceiver(Long userId) {
        List<StickerExchange> exchanges = exchangeRepo.findByReceiverIdAndStatus(userId, ExchangeStatus.PENDING);
        List<StickerExchangeDTO> dtoList = new ArrayList<>();
        exchanges.forEach(e -> dtoList.add(toExchangeDTO(e)));
        return dtoList;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Genera una lámina aleatoria para un álbum dado. Si el código ya está
     * en el álbum, la lámina se crea como {@code DUPLICATE}; de lo contrario
     * como {@code PLACED}.
     *
     * @param album El álbum al que pertenecerá la lámina.
     * @return La nueva lámina generada (no persistida aún).
     */
    private Sticker generateRandomSticker(Album album) {
        Random random = new Random();
        int stickerNumber = random.nextInt(TOTAL_CATALOG_STICKERS) + 1;
        String code = String.format("STK-%03d", stickerNumber);

        Sticker.StickerRarity rarity = calculateRarity(random);
        Sticker.StickerCategory category = Sticker.StickerCategory.values()[random.nextInt(4)];

        List<Sticker> existing = stickerRepo.findByAlbumIdAndStickerCode(album.getId(), code);
        StickerStatus status = existing.isEmpty() ? StickerStatus.PLACED : StickerStatus.DUPLICATE;

        Sticker sticker = new Sticker(album, code, "Lámina " + code, category, rarity);
        sticker.setStatus(status);
        return sticker;
    }

    /**
     * Calcula la rareza de una lámina usando probabilidades ponderadas:
     * COMMON 60%, UNCOMMON 25%, RARE 12%, LEGENDARY 3%.
     *
     * @param random Instancia de Random para el sorteo.
     * @return La rareza calculada.
     */
    private Sticker.StickerRarity calculateRarity(Random random) {
        int roll = random.nextInt(100);
        if (roll < 60) return Sticker.StickerRarity.COMMON;
        if (roll < 85) return Sticker.StickerRarity.UNCOMMON;
        if (roll < 97) return Sticker.StickerRarity.RARE;
        return Sticker.StickerRarity.LEGENDARY;
    }

    /**
     * Recalcula los contadores del álbum (únicas, repetidas, completitud)
     * y persiste los cambios.
     *
     * @param album El álbum a recalcular.
     */
    private void recalculateAlbumStats(Album album) {
        long placed = stickerRepo.countByAlbumIdAndStatus(album.getId(), StickerStatus.PLACED);
        long duplicates = stickerRepo.countByAlbumIdAndStatus(album.getId(), StickerStatus.DUPLICATE);

        album.setUniqueStickersCount((int) placed);
        album.setDuplicateStickersCount((int) duplicates);
        album.setCompletionPercentage(Math.min(100.0, (placed * 100.0) / TOTAL_CATALOG_STICKERS));
        albumRepo.save(album);
    }

    /**
     * Convierte una entidad {@link Sticker} a {@link StickerDTO}.
     *
     * @param s La entidad de la lámina.
     * @return El DTO de la lámina.
     */
    private StickerDTO toStickerDTO(Sticker s) {
        StickerDTO dto = new StickerDTO();
        dto.setId(s.getId());
        dto.setAlbumId(s.getAlbum().getId());
        dto.setStickerCode(s.getStickerCode());
        dto.setDisplayName(s.getDisplayName());
        dto.setCategory(s.getCategory());
        dto.setRarity(s.getRarity());
        dto.setStatus(s.getStatus());
        dto.setImageUrl(s.getImageUrl());
        dto.setTeamIsoCode(s.getTeamIsoCode());
        return dto;
    }

    /**
     * Convierte una entidad {@link StickerExchange} a {@link StickerExchangeDTO}.
     *
     * @param e La entidad del intercambio.
     * @return El DTO del intercambio.
     */
    private StickerExchangeDTO toExchangeDTO(StickerExchange e) {
        StickerExchangeDTO dto = new StickerExchangeDTO();
        dto.setId(e.getId());
        dto.setRequesterId(e.getRequester().getId());
        dto.setRequesterUsername(e.getRequester().getUsername());
        dto.setReceiverId(e.getReceiver().getId());
        dto.setReceiverUsername(e.getReceiver().getUsername());
        dto.setOfferedStickerId(e.getOfferedSticker().getId());
        dto.setOfferedStickerCode(e.getOfferedSticker().getStickerCode());
        dto.setOfferedStickerName(e.getOfferedSticker().getDisplayName());
        dto.setRequestedStickerId(e.getRequestedSticker().getId());
        dto.setRequestedStickerCode(e.getRequestedSticker().getStickerCode());
        dto.setRequestedStickerName(e.getRequestedSticker().getDisplayName());
        dto.setStatus(e.getStatus());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setResolvedAt(e.getResolvedAt());
        return dto;
    }
}