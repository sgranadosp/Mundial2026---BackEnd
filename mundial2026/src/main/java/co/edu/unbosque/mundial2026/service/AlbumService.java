/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.album.AlbumPageDTO;
import co.edu.unbosque.mundial2026.dto.album.CountryOptionDTO;
import co.edu.unbosque.mundial2026.dto.album.OpenPackageResponseDTO;
import co.edu.unbosque.mundial2026.dto.album.StickerSlotDTO;
import co.edu.unbosque.mundial2026.dto.album.StickerSlotDTO.Status;
import co.edu.unbosque.mundial2026.model.Sticker;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.UserSticker;
import co.edu.unbosque.mundial2026.repository.StickerRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.repository.UserStickerRepository;

/**
 * Servicio que encapsula la lógica de negocio del álbum de láminas del
 * Mundial 2026 Hub.
 */
@Service
public class AlbumService {

    /** Número de láminas que entrega un paquete al abrirlo. */
    private static final int STICKERS_PER_PACKAGE = 5;

    /** URL base para servir las imágenes (relativa al context-path). */
    private static final String IMAGE_URL_PREFIX = "/laminas/";

    private final Random random = new Random();

    @Autowired private StickerRepository stickerRepo;
    @Autowired private UserStickerRepository userStickerRepo;
    @Autowired private UserRepository userRepo;

    public AlbumService() { }

    // =========================================================================
    // Listado de selecciones para el autocomplete
    // =========================================================================

    public List<CountryOptionDTO> getAvailableCountries() {
        List<Sticker> all = stickerRepo.findAll();
        Map<String, String> codeToName = new LinkedHashMap<>();
        for (Sticker s : all) {
            codeToName.putIfAbsent(s.getCountryCode(), s.getCountryName());
        }
        return codeToName.entrySet().stream()
                .map(e -> new CountryOptionDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing((CountryOptionDTO o) ->
                                o.getCode().equals("especial") ? 1 : 0)
                        .thenComparing(CountryOptionDTO::getName,
                                Comparator.comparing(String::toLowerCase)))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Página del álbum por selección
    // =========================================================================

    public AlbumPageDTO getAlbumPage(Long userId, String countryCode, String filter) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();

        List<Sticker> catalogo = stickerRepo.findByCountryCodeOrderByPositionAsc(countryCode);
        if (catalogo.isEmpty()) return null;

        List<UserSticker> propias = userStickerRepo.findByUserAndSticker_CountryCode(user, countryCode);
        Map<Long, UserSticker> propiasById = new HashMap<>();
        for (UserSticker us : propias) {
            propiasById.put(us.getSticker().getId(), us);
        }

        List<StickerSlotDTO> slots = new ArrayList<>();
        for (Sticker s : catalogo) {
            UserSticker propia = propiasById.get(s.getId());
            StickerSlotDTO slot;
            if (propia == null) {
                slot = new StickerSlotDTO(s.getCode(), s.getPosition(),
                        null, Status.FALTANTE, 0);
            } else if (propia.getQuantity() >= 2) {
                slot = new StickerSlotDTO(s.getCode(), s.getPosition(),
                        IMAGE_URL_PREFIX + s.getCode() + ".png",
                        Status.REPETIDA, propia.getQuantity());
            } else {
                slot = new StickerSlotDTO(s.getCode(), s.getPosition(),
                        IMAGE_URL_PREFIX + s.getCode() + ".png",
                        Status.PEGADA, propia.getQuantity());
            }
            slots.add(slot);
        }

        if (filter != null && !filter.isBlank() && !"TODAS".equalsIgnoreCase(filter)) {
            Status target;
            try {
                target = Status.valueOf(filter.toUpperCase());
            } catch (IllegalArgumentException e) {
                target = null;
            }
            if (target != null) {
                final Status t = target;
                slots = slots.stream()
                        .filter(slot -> slot.getStatus() == t)
                        .collect(Collectors.toList());
            }
        }

        String countryName = catalogo.get(0).getCountryName();
        return new AlbumPageDTO(countryCode, countryName, slots);
    }

    // =========================================================================
    // Abrir paquete (LÓGICA CORREGIDA)
    // =========================================================================

    /**
     * Abre un paquete de láminas para el usuario indicado.
     * <p>
     * Selecciona {@value #STICKERS_PER_PACKAGE} láminas aleatorias del
     * catálogo completo y las agrega a la colección del usuario.
     * </p>
     * <p>
     * <b>Lógica de inserción correcta:</b>
     * Para cada lámina sorteada:
     * <ol>
     *   <li>Si el usuario NO tiene la lámina en BD → crear fila con quantity=1
     *       (queda PEGADA).</li>
     *   <li>Si el usuario YA tiene la lámina (en BD o en una iteración previa
     *       de este mismo paquete) → incrementar quantity en 1 (queda REPETIDA
     *       si quedó ≥ 2).</li>
     * </ol>
     * </p>
     *
     * @param userId ID del usuario que abre el paquete.
     * @return Resultado con la lista de láminas obtenidas y el progreso
     *         actualizado, o {@code null} si el usuario no existe.
     */
    @Transactional
    public OpenPackageResponseDTO openPackage(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();

        List<Sticker> catalogoCompleto = stickerRepo.findAll();
        if (catalogoCompleto.isEmpty()) {
            return new OpenPackageResponseDTO(new ArrayList<>(), 0, 0);
        }

        List<StickerSlotDTO> obtenidas = new ArrayList<>();

        for (int i = 0; i < STICKERS_PER_PACKAGE; i++) {
            Sticker sorteada = catalogoCompleto.get(random.nextInt(catalogoCompleto.size()));

            // Buscar si ya existe en BD
            Optional<UserSticker> existenteOpt = userStickerRepo.findByUserAndSticker(user, sorteada);
            UserSticker registro;
            if (existenteOpt.isPresent()) {
                // Ya la tenía → incrementar
                registro = existenteOpt.get();
                registro.incrementarCantidad();
                registro = userStickerRepo.save(registro);
            } else {
                // Nueva → crear con quantity=1
                registro = userStickerRepo.save(new UserSticker(user, sorteada));
            }

            // Construir slot de respuesta con el estado actual
            Status status = registro.getQuantity() >= 2 ? Status.REPETIDA : Status.PEGADA;
            StickerSlotDTO slot = new StickerSlotDTO(
                    sorteada.getCode(), sorteada.getPosition(),
                    IMAGE_URL_PREFIX + sorteada.getCode() + ".png",
                    status, registro.getQuantity());
            obtenidas.add(slot);
        }

        long totalUser = userStickerRepo.countByUser(user);
        long totalCatalog = stickerRepo.count();
        return new OpenPackageResponseDTO(obtenidas, totalUser, totalCatalog);
    }

    // =========================================================================
    // Progreso del usuario
    // =========================================================================

    public long getUserProgress(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return 0;
        return userStickerRepo.countByUser(userOpt.get());
    }

    public long getCatalogSize() {
        return stickerRepo.count();
    }

    // =========================================================================
    // Álbum completo del usuario (todas las selecciones en una sola página)
    // =========================================================================

    /**
     * Devuelve el álbum completo del usuario como una única página agrupando
     * las láminas de todas las selecciones, opcionalmente filtrada por estado
     * de posesión.
     * <p>
     * El catálogo se ordena primero por nombre de selección (case-insensitive)
     * y dentro de cada selección por posición ascendente, dejando las páginas
     * de "especiales" al final para que la vista del frontend mantenga un
     * orden estable y predecible.
     * </p>
     *
     * @param userId ID del usuario consultante.
     * @param filter Filtro de estado: {@code TODAS} (por defecto), {@code PEGADA},
     *               {@code REPETIDA} o {@code FALTANTE}.
     * @return {@link AlbumPageDTO} con countryCode {@code "all"} y todos los
     *         slots; {@code null} si el usuario no existe.
     */
    public AlbumPageDTO getFullAlbum(Long userId, String filter) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();

        List<Sticker> catalogo = stickerRepo.findAll();
        catalogo.sort(Comparator
                .comparing((Sticker s) -> "especial".equals(s.getCountryCode()) ? 1 : 0)
                .thenComparing(s -> s.getCountryName().toLowerCase())
                .thenComparing(Sticker::getPosition));

        List<UserSticker> propias = userStickerRepo.findByUser(user);
        Map<Long, UserSticker> propiasById = new HashMap<>();
        for (UserSticker us : propias) {
            propiasById.put(us.getSticker().getId(), us);
        }

        List<StickerSlotDTO> slots = new ArrayList<>();
        for (Sticker s : catalogo) {
            UserSticker propia = propiasById.get(s.getId());
            StickerSlotDTO slot;
            if (propia == null) {
                slot = new StickerSlotDTO(s.getCode(), s.getPosition(),
                        null, Status.FALTANTE, 0);
            } else if (propia.getQuantity() >= 2) {
                slot = new StickerSlotDTO(s.getCode(), s.getPosition(),
                        IMAGE_URL_PREFIX + s.getCode() + ".png",
                        Status.REPETIDA, propia.getQuantity());
            } else {
                slot = new StickerSlotDTO(s.getCode(), s.getPosition(),
                        IMAGE_URL_PREFIX + s.getCode() + ".png",
                        Status.PEGADA, propia.getQuantity());
            }
            slots.add(slot);
        }

        if (filter != null && !filter.isBlank() && !"TODAS".equalsIgnoreCase(filter)) {
            Status target;
            try {
                target = Status.valueOf(filter.toUpperCase());
            } catch (IllegalArgumentException e) {
                target = null;
            }
            if (target != null) {
                final Status t = target;
                slots = slots.stream()
                        .filter(slot -> slot.getStatus() == t)
                        .collect(Collectors.toList());
            }
        }

        return new AlbumPageDTO("all", "Toda mi colección", slots);
    }
}