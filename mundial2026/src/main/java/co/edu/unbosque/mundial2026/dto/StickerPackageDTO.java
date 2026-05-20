/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import co.edu.unbosque.mundial2026.model.StickerPackage.PackageSource;
import co.edu.unbosque.mundial2026.model.StickerPackage.PackageStatus;

/**
 * Clase de Transferencia de Datos (DTO) para representar un paquete de láminas
 * en la plataforma Mundial 2026 Hub.
 * En la respuesta de apertura de un paquete ({@code POST /album/packages/{id}/open})
 * se incluye la lista {@code newStickers} con las láminas que se acaban de revelar,
 * separando las nuevas (pegadas en el álbum) de las repetidas disponibles para
 * intercambio, para que el frontend pueda mostrar la animación de apertura.
 */
public class StickerPackageDTO {

    /**
     * Identificador único del paquete.
     */
    private Long id;

    /**
     * ID del usuario al que fue otorgado el paquete.
     */
    private Long userId;

    /**
     * Nombre de usuario del propietario.
     */
    private String username;

    /**
     * Origen del paquete: DAILY_LOGIN, PREDICTION_COMPLETED, MATCH_FOLLOWED,
     * PROMO_CODE o POLL_PRIZE.
     */
    private PackageSource source;

    /**
     * Estado del paquete: PENDING u OPENED.
     */
    private PackageStatus status;

    /**
     * Número de láminas que contiene el paquete.
     */
    private Integer stickersCount;

    /**
     * Fecha y hora en que el paquete fue otorgado.
     */
    private LocalDateTime grantedAt;

    /**
     * Fecha y hora en que el paquete fue abierto. Nulo si no ha sido abierto.
     */
    private LocalDateTime openedAt;

    /**
     * Código promocional que originó el paquete, si aplica.
     */
    private String promoCode;

    /**
     * Láminas reveladas al abrir el paquete.
     * Solo se popula en la respuesta del endpoint de apertura.
     */
    private List<StickerDTO> newStickers = new ArrayList<>();

    /**
     * Constructor por defecto de {@code StickerPackageDTO}.
     */
    public StickerPackageDTO() {
    }

    /**
     * Constructor con los campos principales del paquete.
     *
     * @param userId        ID del usuario propietario.
     * @param source        Origen del paquete.
     * @param stickersCount Número de láminas.
     */
    public StickerPackageDTO(Long userId, PackageSource source, Integer stickersCount) {
        this.userId = userId;
        this.source = source;
        this.stickersCount = stickersCount;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del paquete. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID del usuario propietario. */
    public Long getUserId() { return userId; }

    /** @param userId El nuevo ID de usuario. */
    public void setUserId(Long userId) { this.userId = userId; }

    /** @return El nombre de usuario del propietario. */
    public String getUsername() { return username; }

    /** @param username El nuevo nombre de usuario. */
    public void setUsername(String username) { this.username = username; }

    /** @return El origen del paquete. */
    public PackageSource getSource() { return source; }

    /** @param source El nuevo origen. */
    public void setSource(PackageSource source) { this.source = source; }

    /** @return El estado del paquete. */
    public PackageStatus getStatus() { return status; }

    /** @param status El nuevo estado. */
    public void setStatus(PackageStatus status) { this.status = status; }

    /** @return El número de láminas. */
    public Integer getStickersCount() { return stickersCount; }

    /** @param stickersCount El nuevo número de láminas. */
    public void setStickersCount(Integer stickersCount) { this.stickersCount = stickersCount; }

    /** @return La fecha de otorgamiento. */
    public LocalDateTime getGrantedAt() { return grantedAt; }

    /** @param grantedAt La nueva fecha de otorgamiento. */
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    /** @return La fecha de apertura. */
    public LocalDateTime getOpenedAt() { return openedAt; }

    /** @param openedAt La nueva fecha de apertura. */
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    /** @return El código promocional del paquete. */
    public String getPromoCode() { return promoCode; }

    /** @param promoCode El nuevo código promocional. */
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    /** @return Las láminas reveladas al abrir el paquete. */
    public List<StickerDTO> getNewStickers() { return newStickers; }

    /** @param newStickers Las nuevas láminas reveladas. */
    public void setNewStickers(List<StickerDTO> newStickers) { this.newStickers = newStickers; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StickerPackageDTO other = (StickerPackageDTO) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "StickerPackageDTO [id=" + id + ", userId=" + userId
                + ", source=" + source + ", status=" + status
                + ", stickersCount=" + stickersCount
                + ", grantedAt=" + grantedAt + ", openedAt=" + openedAt + "]";
    }
}