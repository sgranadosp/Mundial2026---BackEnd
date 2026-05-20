/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.AlbumDTO;
import co.edu.unbosque.mundial2026.dto.StickerExchangeDTO;
import co.edu.unbosque.mundial2026.dto.StickerPackageDTO;
import co.edu.unbosque.mundial2026.model.StickerPackage.PackageSource;
import co.edu.unbosque.mundial2026.service.AlbumService;
import co.edu.unbosque.mundial2026.service.AuditEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para el módulo de álbum digital en la plataforma
 * Mundial 2026 Hub.
 * Expone los endpoints para gestionar la colección de láminas de cada usuario:
 * ver el álbum, listar paquetes pendientes, abrir paquetes con animación
 * de revelación, y el flujo completo de intercambio entre usuarios con
 * confirmación mutua y control de límites antiabusos.
 */
@RestController
@RequestMapping("/album")
@CrossOrigin(origins = { "http://localhost:8080", "http://localhost:8081", "http://localhost:8082",
        "http://localhost:4200", "http://localhost:3000" })
@Transactional
@Tag(name = "Álbum Digital", description = "Colección de láminas, paquetes e intercambios")
@SecurityRequirement(name = "bearerAuth")
public class AlbumController {

    /**
     * Servicio de lógica de negocio del álbum digital.
     */
    @Autowired
    private AlbumService albumService;

    /**
     * Servicio centralizado de auditoría.
     */
    @Autowired
    private AuditEventService auditService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public AlbumController() {
    }

    // =========================================================================
    // Álbum
    // =========================================================================

    /**
     * Obtiene el resumen del álbum de un usuario (sin lista de láminas).
     * Se usa en el dashboard y en la vista de perfil.
     *
     * @param userId El ID del usuario.
     * @return 202 Accepted con el {@link AlbumDTO} de resumen; 404 si no existe.
     */
    @GetMapping("/user/{userId}/summary")
    @Operation(summary = "Resumen del álbum",
               description = "Retorna contadores y porcentaje de completitud sin la lista completa de láminas.")
    public ResponseEntity<?> getAlbumSummary(@PathVariable Long userId) {
        AlbumDTO album = albumService.getAlbumSummary(userId);
        if (album != null) {
            return new ResponseEntity<>(album, HttpStatus.ACCEPTED);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Álbum no encontrado para el usuario", "success", false));
    }

    /**
     * Obtiene el álbum completo de un usuario incluyendo todas sus láminas.
     *
     * @param userId El ID del usuario.
     * @return 202 Accepted con el álbum completo y lista de láminas; 404 si no existe.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Álbum completo",
               description = "Retorna el álbum con la lista completa de láminas (pegadas y repetidas).")
    public ResponseEntity<?> getFullAlbum(@PathVariable Long userId) {
        AlbumDTO album = albumService.getFullAlbum(userId);
        if (album != null) {
            return new ResponseEntity<>(album, HttpStatus.ACCEPTED);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Álbum no encontrado", "success", false));
    }

    // =========================================================================
    // Paquetes
    // =========================================================================

    /**
     * Lista los paquetes de láminas pendientes (no abiertos) de un usuario.
     *
     * @param userId El ID del usuario.
     * @return 202 Accepted con la lista de paquetes pendientes; 204 si no hay.
     */
    @GetMapping("/user/{userId}/packages/pending")
    @Operation(summary = "Paquetes pendientes",
               description = "Retorna los paquetes que el usuario aún no ha abierto.")
    public ResponseEntity<List<StickerPackageDTO>> getPendingPackages(@PathVariable Long userId) {
        List<StickerPackageDTO> packages = albumService.getPendingPackages(userId);
        if (packages.isEmpty()) {
            return new ResponseEntity<>(packages, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(packages, HttpStatus.ACCEPTED);
    }

    /**
     * Abre un paquete de láminas y revela su contenido.
     * La respuesta incluye la lista de láminas obtenidas para que el frontend
     * pueda mostrar la animación de apertura. Las nuevas láminas se agregan al
     * álbum como PLACED y las repetidas como DUPLICATE.
     *
     * @param packageId El ID del paquete a abrir.
     * @param userId    El ID del usuario propietario del paquete.
     * @return 202 Accepted con el paquete abierto y las láminas reveladas;
     *         404 si el paquete no existe, ya fue abierto o no pertenece al usuario.
     */
    @PostMapping("/packages/{packageId}/open")
    @Operation(summary = "Abrir paquete de láminas",
               description = "Abre el paquete y retorna las láminas reveladas para la animación del frontend.")
    public ResponseEntity<?> openPackage(@PathVariable Long packageId, @RequestParam Long userId) {
        StickerPackageDTO result = albumService.openPackage(packageId, userId);
        if (result != null) {
            auditService.logPackageOpened(userId, packageId);
            return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Paquete no encontrado, ya abierto o no te pertenece",
                        "success", false));
    }

    /**
     * Otorga un paquete a un usuario por una acción específica.
     * Endpoint interno usado por otros servicios (inicio de sesión, predicción,
     * seguimiento de partidos). Solo ADMIN puede invocarlo externamente.
     *
     * @param userId El ID del usuario.
     * @param source El origen del paquete (DAILY_LOGIN, PREDICTION_COMPLETED, etc.).
     * @return 201 Created con el paquete otorgado; 404 si el usuario no existe.
     */
    @PostMapping("/user/{userId}/packages/grant")
    @Operation(summary = "Otorgar paquete",
               description = "Solo ADMIN o servicios internos. Otorga un paquete al usuario por una acción.")
    public ResponseEntity<?> grantPackage(@PathVariable Long userId, @RequestParam PackageSource source) {
        StickerPackageDTO pkg = albumService.grantPackage(userId, source);
        if (pkg != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(pkg);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuario no encontrado", "success", false));
    }

    /**
     * Canjea un código promocional para obtener un paquete de láminas.
     *
     * @param userId    El ID del usuario que canjea el código.
     * @param promoCode El código promocional.
     * @return 201 Created con el paquete; 409 Conflict si el código ya fue usado;
     *         404 si el usuario no existe.
     */
    @PostMapping("/user/{userId}/packages/promo")
    @Operation(summary = "Canjear código promocional",
               description = "Canjea un código promo para obtener un paquete de láminas. Uso único por código.")
    public ResponseEntity<?> redeemPromoCode(@PathVariable Long userId, @RequestParam String promoCode) {
        StickerPackageDTO pkg = albumService.grantPackageByPromoCode(userId, promoCode);
        if (pkg != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(pkg);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Código inválido, ya usado o usuario no encontrado", "success", false));
    }

    // =========================================================================
    // Intercambios
    // =========================================================================

    /**
     * Crea una solicitud de intercambio de láminas entre dos usuarios.
     * Valida que ambas láminas sean repetidas, que no estén en otro intercambio
     * activo y que el solicitante no haya superado el límite de intercambios.
     *
     * @param data El DTO con requesterId, receiverId, offeredStickerId y requestedStickerId.
     * @return 201 Created si fue creada; 409 si se superó el límite de intercambios;
     *         404 si alguna entidad no existe; 403 si alguna lámina no es apta.
     */
    @PostMapping("/exchanges")
    @Operation(summary = "Solicitar intercambio",
               description = "Crea una solicitud de intercambio de láminas entre dos usuarios.")
    public ResponseEntity<?> createExchange(@RequestBody StickerExchangeDTO data) {
        int status = albumService.createExchange(data);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Solicitud de intercambio creada", "success", true));
            case 1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Has superado el límite de intercambios activos simultáneos",
                            "success", false));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario o lámina no encontrada", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Las láminas deben ser repetidas y no estar en otro intercambio",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al crear el intercambio", "success", false));
        };
    }

    /**
     * Acepta una solicitud de intercambio recibida.
     * Transfiere las láminas entre los álbumes de ambos usuarios.
     *
     * @param exchangeId El ID del intercambio a aceptar.
     * @param receiverId El ID del usuario receptor que acepta.
     * @return 202 Accepted si fue completado; 404 si no existe; 403 si el usuario
     *         no es el receptor o el intercambio no está en estado PENDING.
     */
    @PutMapping("/exchanges/{exchangeId}/accept")
    @Operation(summary = "Aceptar intercambio",
               description = "El receptor acepta la solicitud. Las láminas se transfieren mutuamente.")
    public ResponseEntity<?> acceptExchange(@PathVariable Long exchangeId, @RequestParam Long receiverId) {
        int status = albumService.acceptExchange(exchangeId, receiverId);

        return switch (status) {
            case 0 -> {
                auditService.logExchangeCompleted(exchangeId, null, receiverId);
                yield ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(Map.of("message", "Intercambio completado exitosamente", "success", true));
            }
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Intercambio no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso o el intercambio ya no está pendiente",
                            "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al aceptar el intercambio", "success", false));
        };
    }

    /**
     * Rechaza una solicitud de intercambio recibida.
     * Libera las láminas bloqueadas (de IN_EXCHANGE a DUPLICATE).
     *
     * @param exchangeId El ID del intercambio a rechazar.
     * @param userId     El ID del usuario receptor que rechaza.
     * @return 202 Accepted si fue rechazado; 404 si no existe; 403 sin permiso.
     */
    @PutMapping("/exchanges/{exchangeId}/reject")
    @Operation(summary = "Rechazar intercambio",
               description = "El receptor rechaza la solicitud. Las láminas quedan libres nuevamente.")
    public ResponseEntity<?> rejectExchange(@PathVariable Long exchangeId, @RequestParam Long userId) {
        int status = albumService.cancelOrRejectExchange(exchangeId, userId, true);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Intercambio rechazado", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Intercambio no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para rechazar este intercambio", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al rechazar el intercambio", "success", false));
        };
    }

    /**
     * Cancela una solicitud de intercambio enviada por el solicitante.
     *
     * @param exchangeId El ID del intercambio a cancelar.
     * @param userId     El ID del usuario que cancela (debe ser el solicitante).
     * @return 202 Accepted si fue cancelado; 404 si no existe; 403 sin permiso.
     */
    @PutMapping("/exchanges/{exchangeId}/cancel")
    @Operation(summary = "Cancelar intercambio",
               description = "El solicitante cancela su solicitud antes de que sea resuelta.")
    public ResponseEntity<?> cancelExchange(@PathVariable Long exchangeId, @RequestParam Long userId) {
        int status = albumService.cancelOrRejectExchange(exchangeId, userId, false);

        return switch (status) {
            case 0 -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Intercambio cancelado", "success", true));
            case 2 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Intercambio no encontrado", "success", false));
            case 4 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para cancelar este intercambio", "success", false));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al cancelar el intercambio", "success", false));
        };
    }

    /**
     * Lista las solicitudes de intercambio pendientes recibidas por un usuario.
     *
     * @param userId El ID del usuario receptor.
     * @return 202 Accepted con la lista; 204 si no hay solicitudes pendientes.
     */
    @GetMapping("/user/{userId}/exchanges/pending")
    @Operation(summary = "Intercambios pendientes recibidos",
               description = "Retorna las solicitudes de intercambio pendientes que el usuario debe resolver.")
    public ResponseEntity<List<StickerExchangeDTO>> getPendingExchanges(@PathVariable Long userId) {
        List<StickerExchangeDTO> exchanges = albumService.getPendingExchangesForReceiver(userId);
        if (exchanges.isEmpty()) {
            return new ResponseEntity<>(exchanges, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(exchanges, HttpStatus.ACCEPTED);
    }
}