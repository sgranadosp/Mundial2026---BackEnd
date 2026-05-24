package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
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
 * autorización mediante JWT.
 * </p>
 *
 * <h3>Campos relacionados con el módulo de Packs</h3>
 * <ul>
 *   <li>{@code firstLoginAt}: marca temporal del primer inicio de sesión
 *       exitoso. Es {@code null} hasta que el usuario se loguea por primera
 *       vez. Sirve para detectar el primer login y otorgar los 3 packs de
 *       bienvenida y arrancar el contador de 12h del pack diario.</li>
 *   <li>{@code dailyPackTimerStartedAt}: instante desde el cual se cuenta el
 *       cooldown de 12 horas para el pack diario. Se reinicia cada vez que
 *       el sistema otorga un pack diario nuevo (no cuando el usuario lo abre).
 *       En el primer login se inicializa igual a {@code firstLoginAt}.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String username;

    private String password;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean pushNotificationsEnabled;

    private boolean emailNotificationsEnabled;

    /**
     * Token de Firebase Cloud Messaging del dispositivo/navegador del usuario.
     * Se actualiza cada vez que el cliente se loguea y obtiene un token fresco.
     * Puede ser {@code null} si el usuario aún no autorizó notificaciones push.
     */
    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private boolean credentialsNonExpired;

    private boolean enabled;

    // =========================================================================
    // Campos del módulo de Packs
    // =========================================================================

    /**
     * Marca temporal del primer inicio de sesión exitoso. {@code null} hasta
     * que el usuario se loguea por primera vez. Cuando se detecta {@code null}
     * en login, el sistema:
     *   1. Setea este campo a {@code LocalDateTime.now()}.
     *   2. Otorga los 3 packs de bienvenida.
     *   3. Inicializa {@code dailyPackTimerStartedAt} con el mismo instante.
     */
    @Column(name = "first_login_at")
    private LocalDateTime firstLoginAt;

    /**
     * Instante desde el cual se cuenta el cooldown de 12 horas para el
     * pack diario. Se reinicia cada vez que el sistema otorga un nuevo
     * pack diario al detectar que han pasado ≥12 horas desde este valor.
     */
    @Column(name = "daily_pack_timer_started_at")
    private LocalDateTime dailyPackTimerStartedAt;

    // =========================================================================
    // Campos de preferencias del usuario (HU05 — Editar perfil)
    // =========================================================================

    /**
     * Código ISO-3166 alfa-3 de la selección favorita del usuario
     * (ej. "COL", "BRA", "ARG"). {@code null} si el usuario aún no ha
     * elegido equipo favorito. Se resuelve contra
     * {@link co.edu.unbosque.mundial2026.repository.TeamRepository#findByIsoCode}
     * para obtener el {@link Team} cuando se necesita.
     */
    @Column(name = "favorite_team_code", length = 3)
    private String favoriteTeamCode;

    /**
     * Ciudad preferida del usuario (ej. "Miami", "Los Angeles", "Ciudad de
     * México"). Debe coincidir con alguna de las ciudades de los estadios
     * sede del Mundial 2026 cargados en la tabla {@code stadiums}.
     * {@code null} si el usuario aún no ha elegido ciudad preferida.
     */
    @Column(name = "favorite_city", length = 80)
    private String favoriteCity;

    /**
     * Constructor por defecto.
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

    public User(String name, String username, String password, String email) {
        this();
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public enum Role {
        USER, ADMIN
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public boolean isAccountNonExpired() { return accountNonExpired; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return credentialsNonExpired; }

    @Override
    public boolean isEnabled() { return enabled; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isPushNotificationsEnabled() { return pushNotificationsEnabled; }
    public void setPushNotificationsEnabled(boolean pushNotificationsEnabled) {
        this.pushNotificationsEnabled = pushNotificationsEnabled;
    }

    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    /** @return Token FCM del dispositivo del usuario, o null si no se ha registrado. */
    public String getFcmToken() { return fcmToken; }

    /** @param fcmToken Nuevo token FCM. */
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }
    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }
    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return Marca del primer login, o {@code null} si nunca se ha logueado. */
    public LocalDateTime getFirstLoginAt() { return firstLoginAt; }
    public void setFirstLoginAt(LocalDateTime firstLoginAt) { this.firstLoginAt = firstLoginAt; }

    /** @return Instante desde el cual corre el cooldown del pack diario. */
    public LocalDateTime getDailyPackTimerStartedAt() { return dailyPackTimerStartedAt; }
    public void setDailyPackTimerStartedAt(LocalDateTime dailyPackTimerStartedAt) {
        this.dailyPackTimerStartedAt = dailyPackTimerStartedAt;
    }

    /** @return Código ISO alfa-3 del equipo favorito, o {@code null} si no se ha elegido. */
    public String getFavoriteTeamCode() { return favoriteTeamCode; }

    /** @param favoriteTeamCode Nuevo código ISO alfa-3 del equipo favorito. */
    public void setFavoriteTeamCode(String favoriteTeamCode) {
        this.favoriteTeamCode = favoriteTeamCode;
    }

    /** @return Ciudad preferida del usuario, o {@code null} si no se ha elegido. */
    public String getFavoriteCity() { return favoriteCity; }

    /** @param favoriteCity Nueva ciudad preferida. */
    public void setFavoriteCity(String favoriteCity) {
        this.favoriteCity = favoriteCity;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        User other = (User) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() { return Objects.hash(id, username); }

    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + ", username=" + username
                + ", role=" + role
                + ", firstLoginAt=" + firstLoginAt
                + ", enabled=" + enabled + "]";
    }
}