/**
 * Paquete que contiene los DTOs de las peticiones del flujo de autenticación.
 */
package co.edu.unbosque.mundial2026.dto.auth;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de petición para solicitar el envío de un código de recuperación
 * de contraseña al correo del usuario (paso 1 del flujo de recuperación).
 */
@Schema(description = "Petición para solicitar el envío de un código de recuperación de contraseña.")
public class RecoveryRequest {

    /**
     * Correo electrónico al que se enviará el código de recuperación.
     */
    @Schema(description = "Correo electrónico del usuario que olvidó su contraseña.",
            example = "usuario@ejemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /** Constructor por defecto. */
    public RecoveryRequest() {
    }

    /**
     * Constructor con el correo.
     *
     * @param email Correo electrónico.
     */
    public RecoveryRequest(String email) {
        this.email = email;
    }

    /** @return El correo electrónico. */
    public String getEmail() { return email; }

    /** @param email El correo electrónico. */
    public void setEmail(String email) { this.email = email; }

    @Override
    public int hashCode() { return Objects.hash(email); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecoveryRequest other)) return false;
        return Objects.equals(email, other.email);
    }

    @Override
    public String toString() {
        return "RecoveryRequest [email=" + email + "]";
    }
}