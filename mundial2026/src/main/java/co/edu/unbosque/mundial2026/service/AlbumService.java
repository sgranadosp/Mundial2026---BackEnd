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
 * <p>
 * Es responsable de:
 * <ul>
 *   <li>Listar las selecciones disponibles para el filtro de búsqueda.</li>
 *   <li>Resolver la página de un usuario para una selección, calculando
 *       el estado (PEGADA / REPETIDA / FALTANTE) de cada lámina a partir
 *       del cruce entre catálogo y colección personal.</li>
 *   <li>Aplicar el filtro de estado (Todas, Pegada, Repetida, Faltante).</li>
 *   <li>Abrir un paquete: seleccionar 5 láminas aleatorias del catálogo,
 *       insertarlas en la colección del usuario, y devolver el resultado
 *       con cantidades actualizadas.</li>
 * </ul>
 * </p>
 */
@Service
public class AlbumService {

    /**
     * Número de láminas que entrega un paquete al abrirlo.
     */
    private static final int STICKERS_PER_PACKAGE = 5;

    /**
     * URL base para servir las imágenes (relativa al context-path).
     */
    private static final String IMAGE_URL_PREFIX = "/laminas/";

    /**
     * Fuente de aleatoriedad para abrir paquetes. Se usa SecureRandom de
     * forma implícita vía {@link Random} y como instancia de clase para
     * mantener estado entre llamadas.
     */
    private final Random random = new Random();

    @Autowired
    private StickerRepository stickerRepo;

    @Autowired
    private UserStickerRepository userStickerRepo;

    @Autowired
    private UserRepository userRepo;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public AlbumService() {
    }

    // =========================================================================
    // Listado de selecciones para el autocomplete
    // =========================================================================

    /**
     * Devuelve las selecciones disponibles para el autocomplete del filtro,
     * ordenadas alfabéticamente por nombre legible, con la página de
     * "Especiales" siempre al final.
     *
     * @return Lista de {@link CountryOptionDTO} con código y nombre.
     */
    public List<CountryOptionDTO> getAvailableCountries() {
        List<Sticker> all = stickerRepo.findAll();

        // Agrupa por countryCode, conservando el nombre. Usa LinkedHashMap
        // para preservar el orden de inserción mientras se construyen.
        Map<String, String> codeToName = new LinkedHashMap<>();
        for (Sticker s : all) {
            codeToName.putIfAbsent(s.getCountryCode(), s.getCountryName());
        }

        // Convertimos a lista, ordenamos alfabéticamente excepto "especial"
        // que va al final.
        List<CountryOptionDTO> result = codeToName.entrySet().stream()
                .map(e -> new CountryOptionDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing((CountryOptionDTO o) ->
                                o.getCode().equals("especial") ? 1 : 0)
                        .thenComparing(CountryOptionDTO::getName,
                                Comparator.comparing(String::toLowerCase)))
                .collect(Collectors.toList());

        return result;
    }

    // =========================================================================
    // Página del álbum por selección
    // =========================================================================

    /**
     * Construye la página del álbum del usuario para una selección, con
     * todas las casillas (6 por defecto) y su estado actual.
     *
     * @param userId      ID del usuario consultante.
     * @param countryCode Código ASCII de la selección (ej. {@code colombia}).
     * @param filter      Filtro a aplicar: {@code null} o cadena vacía = TODAS;
     *                    {@code PEGADA}, {@code REPETIDA}, {@code FALTANTE}.
     * @return La página construida, o {@code null} si el usuario o la
     *         selección no existen.
     */
    public AlbumPageDTO getAlbumPage(Long userId, String countryCode, String filter) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();

        List<Sticker> catalogo = stickerRepo.findByCountryCodeOrderByPositionAsc(countryCode);
        if (catalogo.isEmpty()) {
            return null;
        }

        // Cargamos las láminas que el usuario YA tiene de esta selección y
        // las indexamos por stickerId para mirar O(1).
        List<UserSticker> propias = userStickerRepo.findByUserAndSticker_CountryCode(user, countryCode);
        Map<Long, UserSticker> propiasById = new HashMap<>();
        for (UserSticker us : propias) {
            propiasById.put(us.getSticker().getId(), us);
        }

        // Construimos las 6 casillas cruzando catálogo con propias.
        List<StickerSlotDTO> slots = new ArrayList<>();
        for (Sticker s : catalogo) {
            UserSticker propia = propiasById.get(s.getId());
            StickerSlotDTO slot;
            if (propia == null) {
                // Faltante: sin imageUrl para que el front no cargue nada.
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

        // Aplicamos el filtro si corresponde.
        if (filter != null && !filter.isBlank() && !"TODAS".equalsIgnoreCase(filter)) {
            Status target;
            try {
                target = Status.valueOf(filter.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Filtro no reconocido: se ignora y se devuelven todas.
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
    // Abrir paquete
    // =========================================================================

    /**
     * Abre un paquete de láminas para el usuario indicado.
     * <p>
     * Selecciona {@value #STICKERS_PER_PACKAGE} láminas aleatorias del
     * catálogo completo (cualquier selección o página especial) y las
     * agrega a la colección del usuario. Si el usuario ya posee alguna
     * de las láminas obtenidas, se incrementa su contador en vez de
     * crear filas duplicadas.
     * </p>
     *
     * @param userId ID del usuario que abre el paquete.
     * @return Resultado con la lista de láminas obtenidas y el progreso
     *         actualizado, o {@code null} si el usuario no existe.
     */
    @Transactional
    public OpenPackageResponseDTO openPackage(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();

        List<Sticker> catalogoCompleto = stickerRepo.findAll();
        if (catalogoCompleto.isEmpty()) {
            return new OpenPackageResponseDTO(new ArrayList<>(), 0, 0);
        }

        List<StickerSlotDTO> obtenidas = new ArrayList<>();
        for (int i = 0; i < STICKERS_PER_PACKAGE; i++) {
            Sticker sorteada = catalogoCompleto.get(random.nextInt(catalogoCompleto.size()));
            UserSticker registro = userStickerRepo.findByUserAndSticker(user, sorteada)
                    .orElseGet(() -> userStickerRepo.save(new UserSticker(user, sorteada)));

            // Si la lámina ya existía, incrementamos. Si fue recién creada
            // arriba ya viene con quantity=1, no incrementamos.
            // Para distinguir: si quantity vino > 1 ya estaba; si es 1 puede
            // ser la que recién creamos O una preexistente con qty=1.
            // Para evitar ambigüedad, comparamos con la versión persistida:
            // si el ID no es nulo y la encontramos vía findByUserAndSticker,
            // significa que ya existía y debemos incrementar.
            boolean preexistente = registro.getQuantity() >= 1
                    && wasAlreadyOwned(user, sorteada, obtenidas);
            if (preexistente) {
                registro.incrementarCantidad();
                userStickerRepo.save(registro);
            }

            // Construimos el slot de respuesta con el estado actual.
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

    /**
     * Determina si el usuario ya poseía la lámina ANTES del sorteo actual,
     * sin contar las repeticiones dentro del mismo paquete.
     * <p>
     * Esto se resuelve consultando la BD: si el registro tiene
     * {@code quantity >= 2}, sin duda ya estaba antes del paquete. Si tiene
     * {@code quantity == 1} y aparece ya en la lista de obtenidas actual,
     * es porque la cogimos también en este paquete pero ya existía
     * previamente; si no aparece, es la primera vez que la vemos en este
     * paquete pero ya estaba antes (entró con qty=1 desde otro paquete
     * anterior).
     * </p>
     * <p>
     * En la práctica, para mantener la lógica simple y siempre correcta:
     * consultamos la cantidad ACTUAL y comparamos contra el número de veces
     * que ya la hemos sorteado en este paquete.
     * </p>
     *
     * @param user       Usuario.
     * @param sticker    Lámina sorteada.
     * @param obtenidas  Láminas ya entregadas en este paquete.
     * @return {@code true} si la lámina ya existía antes (debe incrementarse).
     */
    private boolean wasAlreadyOwned(User user, Sticker sticker, List<StickerSlotDTO> obtenidas) {
        // Cuenta cuántas veces ya hemos sorteado esta misma lámina en este paquete.
        long vecesEnEstePaquete = obtenidas.stream()
                .filter(s -> s.getCode().equals(sticker.getCode()))
                .count();
        UserSticker actual = userStickerRepo.findByUserAndSticker(user, sticker).orElse(null);
        if (actual == null) {
            return false;
        }
        // Si la cantidad guardada es mayor a las veces ya sorteadas, significa
        // que la diferencia viene de paquetes previos: ya la tenía.
        return actual.getQuantity() > vecesEnEstePaquete;
    }

    // =========================================================================
    // Progreso del usuario
    // =========================================================================

    /**
     * Devuelve el conteo de láminas únicas que el usuario tiene en su álbum.
     *
     * @param userId ID del usuario.
     * @return Número de láminas distintas obtenidas, o 0 si el usuario no existe.
     */
    public long getUserProgress(Long userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return 0;
        }
        return userStickerRepo.countByUser(userOpt.get());
    }

    /**
     * Devuelve el total de láminas en el catálogo completo (debe ser 294).
     *
     * @return Número total de láminas únicas en el catálogo.
     */
    public long getCatalogSize() {
        return stickerRepo.count();
    }
}