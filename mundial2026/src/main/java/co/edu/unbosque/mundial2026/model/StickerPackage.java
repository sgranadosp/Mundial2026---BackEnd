/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa un paquete de láminas (sticker package) otorgado
 * a un usuario en la plataforma Mundial 2026 Hub.
 * <p>
 * Los paquetes se otorgan al usuario por diferentes acciones dentro de la app
 * (inicio de sesión diario, completar un pronóstico, seguir partidos) o mediante
 * códigos promocionales. También se pueden ganar como premio para los ganadores
 * de una polla. Un paquete contiene un número fijo de láminas que se revelan
 * al momento de ser abierto.
 * </p>
 * <p>
 * Un paquete en estado {@code PENDING} puede ser abierto por el usuario.
 * Una vez abierto, el sistema genera las láminas correspondientes y actualiza
 * el álbum del usuario. El registro del paquete queda en estado {@code OPENED}
 * como evidencia auditable de la transacción.
 * </p>
 */
@Entity
@Table(name = "sticker_packages")
public class StickerPackage {

    /**
     * Identificador único del paquete generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario al que se le otorgó el paquete.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Origen del paquete, definido mediante el enum {@link PackageSource}.
     * Indica por qué acción o evento fue otorgado al usuario.
     */
    @Enumerated(EnumType.STRING)
    private PackageSource source;

    /**
     * Estado actual del paquete, definido mediante el enum {@link PackageStatus}.
     */
    @Enumerated(EnumType.STRING)
    private PackageStatus status;

    /**
     * Número de láminas que contiene este paquete.
     * El valor estándar es 5, pero puede variar según el tipo de paquete.
     */
    private Integer stickersCount;

    /**
     * Fecha y hora en que el paquete fue otorgado al usuario.
     */
    private LocalDateTime grantedAt;

    /**
     * Fecha y hora en que el paquete fue abierto por el usuario.
     * Es nulo mientras el paquete no haya sido abierto.
     */
    private LocalDateTime openedAt;

    /**
     * Código promocional que originó este paquete, si aplica.
     * Es nulo si el paquete fue otorgado por actividad en la app o como premio.
     */
    private String promoCode;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el estado como {@code PENDING}, asigna 5 láminas por defecto
     * y registra la fecha de otorgamiento.
     */
    public StickerPackage() {
        this.status = PackageStatus.PENDING;
        this.stickersCount = 5;
        this.grantedAt = LocalDateTime.now();
    }

    /**
     * Constructor con los datos principales del paquete.
     *
     * @param user          Usuario al que se le otorga el paquete.
     * @param source        Origen del paquete.
     * @param stickersCount Número de láminas que contiene.
     */
    public StickerPackage(User user, PackageSource source, Integer stickersCount) {
        this();
        this.user = user;
        this.source = source;
        this.stickersCount = stickersCount;
    }

    /**
     * Enumeración que define el origen de un paquete de láminas.
     */
    public enum PackageSource {
        /** Otorgado por iniciar sesión diariamente. */
        DAILY_LOGIN,
        /** Otorgado por completar un pronóstico en la polla. */
        PREDICTION_COMPLETED,
        /** Otorgado por seguir partidos en la plataforma. */
        MATCH_FOLLOWED,
        /** Otorgado mediante un código promocional. */
        PROMO_CODE,
        /** Otorgado como premio al ganar una polla. */
        POLL_PRIZE
    }

    /**
     * Enumeración que define el estado de ciclo de vida de un paquete.
     */
    public enum PackageStatus {
        /** El paquete está disponible para ser abierto por el usuario. */
        PENDING,
        /** El paquete ya fue abierto y las láminas fueron agregadas al álbum. */
        OPENED
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del paquete. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del paquete. */
    public void setId(Long id) { this.id = id; }

    /** @return El usuario al que fue otorgado el paquete. */
    public User getUser() { return user; }

    /** @param user El nuevo usuario. */
    public void setUser(User user) { this.user = user; }

    /** @return El origen del paquete. */
    public PackageSource getSource() { return source; }

    /** @param source El nuevo origen del paquete. */
    public void setSource(PackageSource source) { this.source = source; }

    /** @return El estado actual del paquete. */
    public PackageStatus getStatus() { return status; }

    /** @param status El nuevo estado del paquete. */
    public void setStatus(PackageStatus status) { this.status = status; }

    /** @return El número de láminas que contiene el paquete. */
    public Integer getStickersCount() { return stickersCount; }

    /** @param stickersCount El nuevo número de láminas. */
    public void setStickersCount(Integer stickersCount) { this.stickersCount = stickersCount; }

    /** @return La fecha y hora en que el paquete fue otorgado. */
    public LocalDateTime getGrantedAt() { return grantedAt; }

    /** @param grantedAt La nueva fecha de otorgamiento. */
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    /** @return La fecha y hora en que el paquete fue abierto. */
    public LocalDateTime getOpenedAt() { return openedAt; }

    /** @param openedAt La nueva fecha de apertura. */
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    /** @return El código promocional que originó el paquete. */
    public String getPromoCode() { return promoCode; }

    /** @param promoCode El nuevo código promocional. */
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StickerPackage other = (StickerPackage) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "StickerPackage [id=" + id
                + ", user=" + (user != null ? user.getUsername() : "null")
                + ", source=" + source + ", status=" + status
                + ", stickersCount=" + stickersCount
                + ", grantedAt=" + grantedAt + ", openedAt=" + openedAt + "]";
    }
}