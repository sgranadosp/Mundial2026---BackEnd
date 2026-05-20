/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Clase de Transferencia de Datos (DTO) para representar la información
 * de un grupo de polla futbolera en la plataforma Mundial 2026 Hub.
 * En la respuesta al cliente se incluye la lista de miembros como
 * {@code List<UserDTO>} con datos mínimos (ID, nombre de usuario) para
 * evitar exponer información sensible. El {@code inviteCode} solo se
 * incluye en la respuesta al crear el grupo o cuando el dueño lo consulta.
 */
public class PollGroupDTO {

    /**
     * Identificador único del grupo.
     */
    private Long id;

    /**
     * Nombre del grupo asignado por el creador.
     */
    private String name;

    /**
     * Descripción opcional del grupo.
     */
    private String description;

    /**
     * Código único de invitación para unirse al grupo.
     * Solo se incluye en la respuesta al creador.
     */
    private String inviteCode;

    /**
     * ID del usuario creador del grupo.
     */
    private Long ownerId;

    /**
     * Nombre de usuario del creador del grupo.
     */
    private String ownerUsername;

    /**
     * Lista de miembros del grupo con datos mínimos.
     */
    private List<UserDTO> members = new ArrayList<>();

    /**
     * Número total de miembros en el grupo.
     */
    private Integer membersCount;

    /**
     * Fecha y hora en que se creó el grupo.
     */
    private LocalDateTime createdAt;

    /**
     * Indica si el grupo está activo.
     */
    private boolean active;

    /**
     * Constructor por defecto de {@code PollGroupDTO}.
     */
    public PollGroupDTO() {
    }

    /**
     * Constructor con los campos usados al crear un grupo.
     *
     * @param name       Nombre del grupo.
     * @param inviteCode Código de invitación generado.
     * @param ownerId    ID del usuario creador.
     */
    public PollGroupDTO(String name, String inviteCode, Long ownerId) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.ownerId = ownerId;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del grupo. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El nombre del grupo. */
    public String getName() { return name; }

    /** @param name El nuevo nombre. */
    public void setName(String name) { this.name = name; }

    /** @return La descripción del grupo. */
    public String getDescription() { return description; }

    /** @param description La nueva descripción. */
    public void setDescription(String description) { this.description = description; }

    /** @return El código de invitación del grupo. */
    public String getInviteCode() { return inviteCode; }

    /** @param inviteCode El nuevo código de invitación. */
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    /** @return El ID del creador. */
    public Long getOwnerId() { return ownerId; }

    /** @param ownerId El nuevo ID del creador. */
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    /** @return El nombre de usuario del creador. */
    public String getOwnerUsername() { return ownerUsername; }

    /** @param ownerUsername El nuevo nombre de usuario del creador. */
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    /** @return La lista de miembros del grupo. */
    public List<UserDTO> getMembers() { return members; }

    /** @param members La nueva lista de miembros. */
    public void setMembers(List<UserDTO> members) { this.members = members; }

    /** @return El número total de miembros. */
    public Integer getMembersCount() { return membersCount; }

    /** @param membersCount El nuevo conteo de miembros. */
    public void setMembersCount(Integer membersCount) { this.membersCount = membersCount; }

    /** @return La fecha de creación del grupo. */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt La nueva fecha de creación. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return {@code true} si el grupo está activo. */
    public boolean isActive() { return active; }

    /** @param active El nuevo estado del grupo. */
    public void setActive(boolean active) { this.active = active; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PollGroupDTO other = (PollGroupDTO) obj;
        return Objects.equals(id, other.id) && Objects.equals(inviteCode, other.inviteCode);
    }

    @Override
    public int hashCode() { return Objects.hash(id, inviteCode); }

    @Override
    public String toString() {
        return "PollGroupDTO [id=" + id + ", name=" + name
                + ", ownerUsername=" + ownerUsername
                + ", membersCount=" + membersCount
                + ", active=" + active + "]";
    }
}