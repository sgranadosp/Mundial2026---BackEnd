package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa un paquete de láminas OTORGADO a un usuario.
 * <p>
 * Hay una fila por cada pack que el sistema otorga al usuario, sin importar
 * si el usuario ya lo abrió o todavía no. El campo {@link #opened} y
 * {@link #openedAt} indican el estado de uso del pack. Los packs sin abrir
 * se acumulan y persisten a través de cierres y aperturas de sesión hasta
 * que el usuario decida abrirlos.
 * </p>
 *
 * <h3>Orígenes posibles ({@link Origin})</h3>
 * <ul>
 *   <li>{@code WELCOME}: pack de bienvenida. Se otorgan exactamente 3 al
 *       primer login de un usuario.</li>
 *   <li>{@code DAILY}: pack diario. Se otorga 1 cada 12 horas desde el
 *       primer login del usuario (cooldown reiniciable).</li>
 *   <li>{@code POLL}: pack por pronósticos. Se otorga 1 cada vez que el
 *       usuario alcanza un múltiplo de 6 predicciones registradas.</li>
 * </ul>
 *
 * <h3>Reutilización para auditoría</h3>
 * Esta misma tabla sirve como historial: filtrando por usuario y ordenando
 * por {@code grantedAt DESC} se obtiene el log completo de packs que el
 * usuario ha recibido. El estado se deriva del flag {@link #opened}.
 */
@Entity
@Table(name = "sticker_packs",
       indexes = {
           @Index(name = "idx_sticker_packs_user", columnList = "user_id"),
           @Index(name = "idx_sticker_packs_user_opened",
                  columnList = "user_id, opened"),
           @Index(name = "idx_sticker_packs_user_origin",
                  columnList = "user_id, origin, opened")
       })
public class StickerPack {

    /** Identificador único interno. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario al que se le otorgó este pack. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Origen del pack: BIENVENIDA / DIARIO / PRONOSTICOS. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origin origin;

    /** Fecha y hora en que el sistema otorgó este pack. */
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    /** {@code true} si el usuario ya lo abrió. */
    @Column(nullable = false)
    private boolean opened;

    /** Fecha en que el usuario abrió el pack. {@code null} si no se ha abierto. */
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /**
     * Origen del pack: razón por la cual el sistema lo otorgó.
     */
    public enum Origin {
        /** Pack de bienvenida (solo 3 al primer login). */
        WELCOME,
        /** Pack diario (uno cada 12 horas). */
        DAILY,
        /** Pack por cada 6 pronósticos realizados. */
        POLL
    }

    /** Constructor por defecto requerido por JPA. */
    public StickerPack() {
    }

    /**
     * Crea un nuevo pack pendiente para el usuario indicado, con el origen
     * dado y fecha de otorgamiento {@code LocalDateTime.now()}.
     *
     * @param user   Usuario al que se otorga el pack.
     * @param origin Origen del pack.
     */
    public StickerPack(User user, Origin origin) {
        this.user = user;
        this.origin = origin;
        this.grantedAt = LocalDateTime.now();
        this.opened = false;
        this.openedAt = null;
    }

    /**
     * Marca el pack como abierto en el instante actual.
     */
    public void markOpened() {
        this.opened = true;
        this.openedAt = LocalDateTime.now();
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public boolean isOpened() { return opened; }
    public void setOpened(boolean opened) { this.opened = opened; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StickerPack other = (StickerPack) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "StickerPack [id=" + id
                + ", userId=" + (user != null ? user.getId() : null)
                + ", origin=" + origin
                + ", grantedAt=" + grantedAt
                + ", opened=" + opened + "]";
    }
}