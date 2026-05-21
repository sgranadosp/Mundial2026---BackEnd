package co.edu.unbosque.mundial2026.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa a un usuario registrado en la plataforma
 * Mundial 2026 Hub.
 * <p>
 * Implementa {@link UserDetails} de Spring Security para autenticación y
 * autorización mediante JWT. Almacena credenciales, datos básicos de perfil,
 * preferencias de notificación, rol en el sistema y flags de estado de cuenta.
 * </p>
 * <p>
 * <b>Política de cifrado en BD:</b>
 * <ul>
 *   <li>{@code email} se almacena encriptado con AES (dato sensible).</li>
 *   <li>{@code password} se almacena con BCrypt (hash unidireccional).</li>
 *   <li>{@code name} y {@code username} se almacenan en texto plano por
 *       razones funcionales (búsqueda, visualización en UI sin desencriptar,
 *       reproducibilidad de login).</li>
 * </ul>
 * </p>
 * <p>
 * El código de verificación o recuperación NO se persiste aquí; vive en una
 * tabla separada {@code VerificationCode} con expiración temporal, ya que es
 * un valor de un solo uso y no forma parte del estado del perfil.
 * </p>
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    /**
     * Identificador único para la serialización de objetos de esta clase.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Identificador único del usuario, generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del usuario. Se almacena en texto plano en BD.
     */
    private String name;

    /**
     * Nombre de usuario único para autenticación. Se almacena en texto plano
     * para permitir búsqueda directa y comparación reproducible durante el login.
     */
    @Column(unique = true)
    private String username;

    /**
     * Contraseña del usuario codificada con BCrypt (hash unidireccional).
     */
    private String password;

    /**
     * Dirección de correo electrónico única del usuario. Se almacena encriptada
     * con AES por ser un dato sensible que permite identificación personal.
     */
    @Column(unique = true)
    private String email;

    /**
     * Rol del usuario en el sistema, definido mediante el enum {@link Role}.
     */
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Indica si el usuario ha activado las notificaciones push.
     */
    private boolean pushNotificationsEnabled;

    /**
     * Indica si el usuario ha activado las notificaciones por correo electrónico.
     */
    private boolean emailNotificationsEnabled;

    /**
     * Indica si la cuenta del usuario no ha expirado. Exigido por Spring Security.
     */
    private boolean accountNonExpired;

    /**
     * Indica si la cuenta del usuario no está bloqueada.
     * Modificable por un administrador (HU17 / HU18).
     */
    private boolean accountNonLocked;

    /**
     * Indica si las credenciales del usuario no han expirado.
     * Exigido por Spring Security.
     */
    private boolean credentialsNonExpired;

    /**
     * Indica si la cuenta del usuario está habilitada. Una cuenta recién
     * registrada queda con {@code enabled = false} hasta que el usuario
     * verifique su código de 6 dígitos.
     */
    private boolean enabled;

    /**
     * Constructor por defecto. Inicializa los flags de estado de cuenta como
     * verdaderos y deja el rol como nulo hasta que sea asignado en el registro.
     * Las notificaciones push y por correo se activan por defecto.
     */
    public User() {
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
        this.pushNotificationsEnabled = true;
        this.emailNotificationsEnabled = true;
        this.role = null;
    }

    /**
     * Constructor con los datos básicos del usuario. Los flags de estado
     * se inicializan en el constructor por defecto mediante {@code this()}.
     *
     * @param name     Nombre completo del usuario.
     * @param username Nombre de usuario para autenticación.
     * @param password Contraseña (se codificará con BCrypt en el servicio).
     * @param email    Correo electrónico del usuario.
     */
    public User(String name, String username, String password, String email) {
        this();
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    /**
     * Enumeración que define los roles posibles de un usuario en el sistema.
     * <ul>
     *   <li>{@code USER} — aficionado registrado con acceso estándar.</li>
     *   <li>{@code ADMIN} — operador con acceso al panel de administración.</li>
     * </ul>
     */
    public enum Role {
        USER, ADMIN
    }

    // =========================================================================
    // Implementación de UserDetails (Spring Security)
    // =========================================================================

    /**
     * Retorna las autoridades concedidas al usuario basadas en su rol.
     * Se utiliza el prefijo "ROLE_" requerido por Spring Security.
     *
     * @return Colección con la autoridad correspondiente al rol del usuario.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Retorna el nombre de usuario utilizado para la autenticación.
     *
     * @return El campo {@code username} del usuario.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Retorna la contraseña codificada del usuario.
     *
     * @return La contraseña codificada con BCrypt.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Indica si la cuenta del usuario no ha expirado.
     *
     * @return {@code true} si la cuenta está vigente.
     */
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    /**
     * Indica si la cuenta del usuario no está bloqueada.
     *
     * @return {@code true} si la cuenta no ha sido bloqueada por un administrador.
     */
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /**
     * Indica si las credenciales del usuario no han expirado.
     *
     * @return {@code true} si las credenciales están vigentes.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    /**
     * Indica si la cuenta del usuario está habilitada.
     *
     * @return {@code true} si la cuenta está activa.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del usuario. */
    public Long getId() { return id; }

    /** @param id El nuevo ID del usuario. */
    public void setId(Long id) { this.id = id; }

    /** @return El nombre completo del usuario. */
    public String getName() { return name; }

    /** @param name El nuevo nombre completo. */
    public void setName(String name) { this.name = name; }

    /** @param username El nuevo nombre de usuario. */
    public void setUsername(String username) { this.username = username; }

    /** @param password La nueva contraseña codificada. */
    public void setPassword(String password) { this.password = password; }

    /** @return El correo electrónico del usuario (encriptado en BD). */
    public String getEmail() { return email; }

    /** @param email El nuevo correo electrónico. */
    public void setEmail(String email) { this.email = email; }

    /** @return El rol del usuario. */
    public Role getRole() { return role; }

    /** @param role El nuevo rol del usuario. */
    public void setRole(Role role) { this.role = role; }

    /** @return {@code true} si las notificaciones push están activas. */
    public boolean isPushNotificationsEnabled() { return pushNotificationsEnabled; }

    /** @param pushNotificationsEnabled Activa o desactiva las notificaciones push. */
    public void setPushNotificationsEnabled(boolean pushNotificationsEnabled) {
        this.pushNotificationsEnabled = pushNotificationsEnabled;
    }

    /** @return {@code true} si las notificaciones por correo están activas. */
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }

    /** @param emailNotificationsEnabled Activa o desactiva las notificaciones por correo. */
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    /** @param accountNonExpired Nuevo valor del flag de expiración de cuenta. */
    public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }

    /** @param accountNonLocked Nuevo valor del flag de bloqueo de cuenta. */
    public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }

    /** @param credentialsNonExpired Nuevo valor del flag de expiración de credenciales. */
    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    /** @param enabled Nuevo valor del flag de habilitación de cuenta. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    /**
     * Compara este objeto User con otro basándose en ID y username.
     *
     * @param obj El objeto a comparar.
     * @return {@code true} si ambos objetos representan el mismo usuario.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        User other = (User) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(username, other.username);
    }

    /**
     * Genera el código hash basado en ID y username.
     *
     * @return Código hash del objeto.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    /**
     * Representación en cadena del usuario. No incluye la contraseña por seguridad.
     *
     * @return Cadena con los atributos principales del usuario.
     */
    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + ", username=" + username
                + ", role=" + role
                + ", accountNonLocked=" + accountNonLocked
                + ", enabled=" + enabled + "]";
    }
}