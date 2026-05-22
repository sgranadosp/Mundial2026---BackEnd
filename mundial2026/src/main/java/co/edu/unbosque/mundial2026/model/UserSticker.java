package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidad JPA que representa una lámina específica que un usuario posee
 * en su álbum personal.
 * <p>
 * Hay una fila por cada combinación única (usuario, lámina). Si el usuario
 * obtiene la misma lámina más de una vez en distintos paquetes, no se crea
 * una fila nueva sino que se incrementa el campo {@code quantity}. La primera
 * lámina obtenida queda "pegada" en el álbum; las copias adicionales se
 * consideran repetidas y quedan disponibles para la sección de intercambio.
 * </p>
 * <p>
 * Estado derivado a partir de {@code quantity}: 
 * <ul>
 *   <li>quantity = 0 → no existe esta fila → la lámina está FALTANTE.</li>
 *   <li>quantity = 1 → la lámina está PEGADA pero no es repetida.</li>
 *   <li>quantity ≥ 2 → la lámina está PEGADA y además tiene (quantity - 1)
 *       copias REPETIDAS disponibles para intercambiar.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "user_stickers",
       indexes = {
           @Index(name = "idx_user_stickers_user", columnList = "user_id"),
           @Index(name = "idx_user_stickers_sticker", columnList = "sticker_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_sticker",
                             columnNames = { "user_id", "sticker_id" })
       })
public class UserSticker {

    /**
     * Identificador único interno.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario propietario de la lámina.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Lámina del catálogo maestro que el usuario posee.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "sticker_id", nullable = false)
    private Sticker sticker;

    /**
     * Cantidad de copias de esta lámina que el usuario tiene.
     * Siempre ≥ 1 mientras exista la fila. Si llega a 0 (por intercambio
     * que entrega todas las copias), la fila debe eliminarse.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Marca temporal de la primera vez que se obtuvo esta lámina.
     */
    @Column(name = "obtained_at", nullable = false)
    private LocalDateTime obtainedAt;

    /** Constructor por defecto requerido por JPA. */
    public UserSticker() {
    }

    /**
     * Constructor con los campos obligatorios. La cantidad inicial es 1
     * y la fecha se autoinicializa a {@code LocalDateTime.now()}.
     *
     * @param user    Usuario propietario.
     * @param sticker Lámina obtenida.
     */
    public UserSticker(User user, Sticker sticker) {
        this.user = user;
        this.sticker = sticker;
        this.quantity = 1;
        this.obtainedAt = LocalDateTime.now();
    }

    // =========================================================================
    // Métodos de conveniencia
    // =========================================================================

    /**
     * Incrementa la cantidad de copias en uno.
     */
    public void incrementarCantidad() {
        this.quantity++;
    }

    /**
     * @return {@code true} si el usuario tiene al menos una copia repetida
     *         (quantity ≥ 2) disponible para intercambio.
     */
    public boolean tieneRepetidas() {
        return quantity >= 2;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID interno. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El usuario propietario. */
    public User getUser() { return user; }

    /** @param user El nuevo usuario. */
    public void setUser(User user) { this.user = user; }

    /** @return La lámina poseída. */
    public Sticker getSticker() { return sticker; }

    /** @param sticker La nueva lámina. */
    public void setSticker(Sticker sticker) { this.sticker = sticker; }

    /** @return La cantidad de copias. */
    public int getQuantity() { return quantity; }

    /** @param quantity La nueva cantidad. */
    public void setQuantity(int quantity) { this.quantity = quantity; }

    /** @return La fecha de obtención. */
    public LocalDateTime getObtainedAt() { return obtainedAt; }

    /** @param obtainedAt La nueva fecha de obtención. */
    public void setObtainedAt(LocalDateTime obtainedAt) { this.obtainedAt = obtainedAt; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserSticker other = (UserSticker) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "UserSticker [id=" + id
                + ", userId=" + (user != null ? user.getId() : null)
                + ", stickerCode=" + (sticker != null ? sticker.getCode() : null)
                + ", quantity=" + quantity + "]";
    }
}