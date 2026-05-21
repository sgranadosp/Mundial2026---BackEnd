/**
 * Paquete que contiene los DTOs de las peticiones del flujo de autenticación.
 */
package co.edu.unbosque.mundial2026.dto.auth;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de petición para el endpoint {@code POST /auth/login}.
 * <p>
 * Acepta como identificador el {@code username} o el {@code email} del usuario
 * en texto plano. El backend resuelve internamente cuál de los dos coincide
 * antes de autenticar contra Spring Security.
 * </p>
 */
@Schema(description = "Petición de inicio de sesión por username o email.")
public class LoginRequest {

    /**
     * Username o correo electrónico del usuario en texto plano.
     */
    @Schema(description = "Username o correo electrónico del usuario.",
            example = "miusuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;

    /**
     * Contraseña en texto plano.
     */
    @Schema(description = "Contraseña en texto plano.",
            example = "MiPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /** Constructor por defecto. */
    public LoginRequest() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param identifier Username o email.
     * @param password   Contraseña.
     */
    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    /** @return El identificador (username o email). */
    public String getIdentifier() { return identifier; }

    /** @param identifier El identificador (username o email). */
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    /** @return La contraseña. */
    public String getPassword() { return password; }

    /** @param password La contraseña. */
    public void setPassword(String password) { this.password = password; }

    @Override
    public int hashCode() { return Objects.hash(identifier); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LoginRequest other)) return false;
        return Objects.equals(identifier, other.identifier);
    }

    @Override
    public String toString() {
        return "LoginRequest [identifier=" + identifier + ", password=***]";
    }
}