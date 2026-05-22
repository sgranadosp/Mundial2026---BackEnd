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
 * genera un código de invitación de 8 caracteres y los miembros se unen
 * mediante ese código.
 * </p>
 */
@Entity
@Table(name = "poll_groups")
public class PollGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    /**
     * Código único de invitación generado al crear el grupo.
     * Son 8 caracteres alfanuméricos en mayúsculas (los primeros 8 de un UUID).
     */
    @Column(unique = true, length = 8)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "poll_group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    private LocalDateTime createdAt;

    private boolean active;

    public PollGroup() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public PollGroup(String name, User owner, String inviteCode) {
        this();
        this.name = name;
        this.owner = owner;
        this.inviteCode = inviteCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) { this.members = members; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PollGroup other = (PollGroup) obj;
        return Objects.equals(id, other.id) && Objects.equals(inviteCode, other.inviteCode);
    }

    @Override
    public int hashCode() { return Objects.hash(id, inviteCode); }

    @Override
    public String toString() {
        return "PollGroup [id=" + id + ", name=" + name + ", inviteCode=" + inviteCode
                + ", owner=" + (owner != null ? owner.getUsername() : "null")
                + ", members=" + members.size() + ", active=" + active + "]";
    }
}