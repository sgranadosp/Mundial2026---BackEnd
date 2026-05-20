/**
 * Paquete que contiene las clases de entidad (modelo) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa un grupo de polla futbolera en la plataforma
 * Mundial 2026 Hub.
 * <p>
 * Una polla es un juego social de predicciones por puntos. El creador del grupo
 * genera un código de invitación y los miembros se unen mediante ese código.
 * El sistema calcula y consolida puntajes de forma automática cuando cada partido
 * finaliza, y publica un ranking interno del grupo.
 * </p>
 * <p>
 * El sistema se modela exclusivamente como juego por puntos; no se gestiona
 * dinero real entre usuarios (restricción de negocio del proyecto).
 * </p>
 */
@Entity
@Table(name = "poll_groups")
public class PollGroup {

    /**
     * Identificador único del grupo generado por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del grupo asignado por el creador (ej. "Los del trabajo", "Familia García").
     */
    private String name;

    /**
     * Descripción opcional del grupo.
     */
    private String description;

    /**
     * Código único de invitación generado al crear el grupo.
     * Los usuarios lo usan para unirse sin necesidad de ser invitados directamente.
     */
    @Column(unique = true, length = 12)
    private String inviteCode;

    /**
     * Usuario que creó el grupo. Es el administrador del grupo
     * y el único que puede eliminar el grupo o expulsar miembros.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * Lista de usuarios miembros del grupo, incluyendo el creador.
     * Se usa una tabla de unión para la relación muchos a muchos.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "poll_group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    /**
     * Fecha y hora en que se creó el grupo.
     */
    private LocalDateTime createdAt;

    /**
     * Indica si el grupo está activo. Un grupo inactivo no acepta nuevos
     * pronósticos pero conserva el historial de puntuaciones.
     */
    private boolean active;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el grupo como activo y registra la fecha de creación.
     */
    public PollGroup() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con los datos principales del grupo.
     *
     * @param name       Nombre del grupo.
     * @param owner      Usuario creador del grupo.
     * @param inviteCode Código de invitación único.
     */
    public PollGroup(String name, User owner, String inviteCode) {
        this();
        this.name = name;
        this.owner = owner;
        this.inviteCode = inviteCode;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del grupo. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del grupo. */
    public void setId(Long id) { this.id = id; }

    /** @return El nombre del grupo. */
    public String getName() { return name; }

    /** @param name El nuevo nombre del grupo. */
    public void setName(String name) { this.name = name; }

    /** @return La descripción del grupo. */
    public String getDescription() { return description; }

    /** @param description La nueva descripción del grupo. */
    public void setDescription(String description) { this.description = description; }

    /** @return El código de invitación del grupo. */
    public String getInviteCode() { return inviteCode; }

    /** @param inviteCode El nuevo código de invitación. */
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    /** @return El usuario creador del grupo. */
    public User getOwner() { return owner; }

    /** @param owner El nuevo creador del grupo. */
    public void setOwner(User owner) { this.owner = owner; }

    /** @return La lista de miembros del grupo. */
    public List<User> getMembers() { return members; }

    /** @param members La nueva lista de miembros. */
    public void setMembers(List<User> members) { this.members = members; }

    /** @return La fecha de creación del grupo. */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt La nueva fecha de creación. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return {@code true} si el grupo está activo. */
    public boolean isActive() { return active; }

    /** @param active El nuevo estado de actividad del grupo. */
    public void setActive(boolean active) { this.active = active; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    /**
     * Compara grupos por ID y código de invitación.
     *
     * @param obj El objeto a comparar.
     * @return {@code true} si representan el mismo grupo.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PollGroup other = (PollGroup) obj;
        return Objects.equals(id, other.id) && Objects.equals(inviteCode, other.inviteCode);
    }

    /**
     * Genera el código hash basado en ID y código de invitación.
     *
     * @return Código hash del objeto.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, inviteCode);
    }

    /**
     * Representación en cadena del grupo.
     *
     * @return Cadena con los atributos principales del grupo.
     */
    @Override
    public String toString() {
        return "PollGroup [id=" + id + ", name=" + name + ", inviteCode=" + inviteCode
                + ", owner=" + (owner != null ? owner.getUsername() : "null")
                + ", members=" + members.size() + ", active=" + active + "]";
    }
}