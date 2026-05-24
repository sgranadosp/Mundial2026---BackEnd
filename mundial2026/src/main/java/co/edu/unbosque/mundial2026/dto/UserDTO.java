/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.Objects;

import co.edu.unbosque.mundial2026.model.User.Role;

/**
 * Clase de Transferencia de Datos (DTO) para representar la información
 * de un usuario en la plataforma Mundial 2026 Hub.
 * <p>
 * Se usa para transferir datos entre capas sin exponer la entidad JPA
 * directamente. En operaciones de lectura la contraseña nunca se incluye en
 * la respuesta; el {@code email} se deserializa desencriptado desde el servicio
 * antes de retornarlo al cliente.
 * </p>
 */
public class UserDTO {

    /**
     * Identificador único del usuario.
     */
    private Long id;

    /**
     * Nombre completo del usuario.
     */
    private String name;

    /**
     * Nombre de usuario para autenticación.
     */
    private String username;

    /**
     * Contraseña del usuario. Solo se usa en operaciones de registro
     * y actualización; nunca se incluye en respuestas de lectura.
     */
    private String password;

    /**
     * Dirección de correo electrónico del usuario.
     */
    private String email;

    /**
     * Rol del usuario en el sistema (USER o ADMIN).
     */
    private Role role;

    /**
     * Indica si las notificaciones push están activas para el usuario.
     */
    private boolean pushNotificationsEnabled;

    /**
     * Indica si las notificaciones por correo están activas para el usuario.
     */
    private boolean emailNotificationsEnabled;

    /**
     * Indica si la cuenta del usuario está bloqueada por un administrador.
     */
    private boolean accountNonLocked;

    /**
     * Indica si la cuenta del usuario está habilitada.
     */
    private boolean enabled;

    /**
     * Código ISO-3166 alfa-3 de la selección favorita del usuario
     * (ej. "COL", "BRA"). {@code null} si aún no se ha elegido.
     */
    private String favoriteTeamCode;

    /**
     * Ciudad preferida del usuario, debe coincidir con alguna ciudad sede del
     * Mundial 2026 cargada en la tabla {@code stadiums}. {@code null} si aún
     * no se ha elegido.
     */
    private String favoriteCity;

    /**
     * Constructor por defecto de {@code UserDTO}.
     */
    public UserDTO() {
    }

    /**
     * Constructor con los campos básicos usados en registro e inicio de sesión.
     *
     * @param name     Nombre completo del usuario.
     * @param username Nombre de usuario para autenticación.
     * @param password Contraseña del usuario.
     * @param email    Correo electrónico del usuario.
     */
    public UserDTO(String name, String username, String password, String email) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del usuario. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El nombre completo. */
    public String getName() { return name; }

    /** @param name El nuevo nombre. */
    public void setName(String name) { this.name = name; }

    /** @return El nombre de usuario. */
    public String getUsername() { return username; }

    /** @param username El nuevo nombre de usuario. */
    public void setUsername(String username) { this.username = username; }

    /** @return La contraseña. */
    public String getPassword() { return password; }

    /** @param password La nueva contraseña. */
    public void setPassword(String password) { this.password = password; }

    /** @return El correo electrónico. */
    public String getEmail() { return email; }

    /** @param email El nuevo correo. */
    public void setEmail(String email) { this.email = email; }

    /** @return El rol del usuario. */
    public Role getRole() { return role; }

    /** @param role El nuevo rol. */
    public void setRole(Role role) { this.role = role; }

    /** @return {@code true} si las notificaciones push están activas. */
    public boolean isPushNotificationsEnabled() { return pushNotificationsEnabled; }

    /** @param pushNotificationsEnabled Nuevo estado de notificaciones push. */
    public void setPushNotificationsEnabled(boolean pushNotificationsEnabled) {
        this.pushNotificationsEnabled = pushNotificationsEnabled;
    }

    /** @return {@code true} si las notificaciones por correo están activas. */
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }

    /** @param emailNotificationsEnabled Nuevo estado de notificaciones por correo. */
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    /** @return {@code true} si la cuenta no está bloqueada. */
    public boolean isAccountNonLocked() { return accountNonLocked; }

    /** @param accountNonLocked Nuevo estado del bloqueo de cuenta. */
    public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }

    /** @return {@code true} si la cuenta está habilitada. */
    public boolean isEnabled() { return enabled; }

    /** @param enabled Nuevo estado de habilitación de cuenta. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return El código ISO alfa-3 del equipo favorito (puede ser {@code null}). */
    public String getFavoriteTeamCode() { return favoriteTeamCode; }

    /** @param favoriteTeamCode Nuevo código ISO alfa-3 del equipo favorito. */
    public void setFavoriteTeamCode(String favoriteTeamCode) {
        this.favoriteTeamCode = favoriteTeamCode;
    }

    /** @return La ciudad preferida del usuario (puede ser {@code null}). */
    public String getFavoriteCity() { return favoriteCity; }

    /** @param favoriteCity Nueva ciudad preferida. */
    public void setFavoriteCity(String favoriteCity) { this.favoriteCity = favoriteCity; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserDTO other = (UserDTO) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(username, other.username)
                && role == other.role;
    }

    @Override
    public int hashCode() { return Objects.hash(id, username, role); }

    @Override
    public String toString() {
        return "UserDTO [id=" + id + ", name=" + name + ", username=" + username
                + ", role=" + role
                + ", accountNonLocked=" + accountNonLocked
                + ", enabled=" + enabled + "]";
    }
}