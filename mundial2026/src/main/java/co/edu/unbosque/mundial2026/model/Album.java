/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidad JPA que representa el álbum digital de un usuario en la plataforma
 * Mundial 2026 Hub.
 * <p>
 * El álbum es el contenedor principal de la colección de láminas ({@link Sticker})
 * de cada usuario. Cada usuario tiene exactamente un álbum. Las láminas se
 * obtienen al abrir paquetes ({@link StickerPackage}) y se pueden intercambiar
 * con otros usuarios ({@link StickerExchange}).
 * </p>
 * <p>
 * El álbum almacena el progreso de completitud del usuario para mostrar
 * en su perfil y como estímulo de engagement dentro de la plataforma.
 * No se vende contenido; los paquetes se obtienen por actividad en la app
 * o códigos promocionales.
 * </p>
 */
@Entity
@Table(
    name = "albums",
    uniqueConstraints = @UniqueConstraint(name = "uk_album_user", columnNames = {"user_id"})
)
public class Album {

    /**
     * Identificador único del álbum generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario propietario del álbum. La relación es uno a uno;
     * cada usuario tiene exactamente un álbum.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Lista de entradas de láminas en el álbum del usuario.
     * Incluye tanto láminas pegadas como repetidas disponibles para intercambio.
     */
    @OneToMany(mappedBy = "album", fetch = FetchType.LAZY)
    private List<Sticker> stickers = new ArrayList<>();

    /**
     * Número total de láminas únicas que el usuario ha obtenido y pegado.
     * Se actualiza cada vez que el usuario abre un paquete o recibe un intercambio.
     */
    private Integer uniqueStickersCount;

    /**
     * Número total de láminas repetidas que el usuario tiene disponibles
     * para intercambio. Se actualiza con cada apertura de paquete e intercambio.
     */
    private Integer duplicateStickersCount;

    /**
     * Porcentaje de completitud del álbum calculado por el sistema.
     * Va del 0 al 100 según cuántas láminas únicas tiene el usuario
     * sobre el total de láminas disponibles en el álbum.
     */
    private Double completionPercentage;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa los contadores en cero y la completitud en 0.0.
     */
    public Album() {
        this.uniqueStickersCount = 0;
        this.duplicateStickersCount = 0;
        this.completionPercentage = 0.0;
    }

    /**
     * Constructor con el usuario propietario.
     *
     * @param user Usuario propietario del álbum.
     */
    public Album(User user) {
        this();
        this.user = user;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del álbum. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del álbum. */
    public void setId(Long id) { this.id = id; }

    /** @return El usuario propietario del álbum. */
    public User getUser() { return user; }

    /** @param user El nuevo usuario propietario. */
    public void setUser(User user) { this.user = user; }

    /** @return La lista de láminas en el álbum. */
    public List<Sticker> getStickers() { return stickers; }

    /** @param stickers La nueva lista de láminas. */
    public void setStickers(List<Sticker> stickers) { this.stickers = stickers; }

    /** @return El número de láminas únicas obtenidas. */
    public Integer getUniqueStickersCount() { return uniqueStickersCount; }

    /** @param uniqueStickersCount El nuevo conteo de láminas únicas. */
    public void setUniqueStickersCount(Integer uniqueStickersCount) {
        this.uniqueStickersCount = uniqueStickersCount;
    }

    /** @return El número de láminas repetidas disponibles. */
    public Integer getDuplicateStickersCount() { return duplicateStickersCount; }

    /** @param duplicateStickersCount El nuevo conteo de láminas repetidas. */
    public void setDuplicateStickersCount(Integer duplicateStickersCount) {
        this.duplicateStickersCount = duplicateStickersCount;
    }

    /** @return El porcentaje de completitud del álbum (0-100). */
    public Double getCompletionPercentage() { return completionPercentage; }

    /** @param completionPercentage El nuevo porcentaje de completitud. */
    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Album other = (Album) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Album [id=" + id
                + ", user=" + (user != null ? user.getUsername() : "null")
                + ", uniqueStickersCount=" + uniqueStickersCount
                + ", duplicateStickersCount=" + duplicateStickersCount
                + ", completionPercentage=" + completionPercentage + "]";
    }
}