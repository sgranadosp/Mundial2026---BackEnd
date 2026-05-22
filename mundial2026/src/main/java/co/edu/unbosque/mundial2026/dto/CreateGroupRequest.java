/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

/**
 * DTO de entrada para crear un nuevo grupo de polla.
 * <p>
 * El cliente envía solo {@code name} y {@code description}: el {@code ownerId}
 * NO viene en el body, se obtiene del JWT del usuario autenticado en el
 * controlador (más seguro: evita que un atacante cree grupos en nombre
 * de otro usuario).
 * </p>
 */
public class CreateGroupRequest {

    /**
     * Nombre del grupo (obligatorio).
     */
    private String name;

    /**
     * Descripción del grupo (opcional). Puede ser texto breve que ayude a
     * los miembros a reconocer la polla.
     */
    private String description;

    /**
     * Constructor por defecto requerido por Jackson.
     */
    public CreateGroupRequest() {
    }

    /**
     * Constructor con los dos campos del DTO.
     *
     * @param name        Nombre del grupo.
     * @param description Descripción del grupo.
     */
    public CreateGroupRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "CreateGroupRequest [name=" + name + ", description=" + description + "]";
    }
}