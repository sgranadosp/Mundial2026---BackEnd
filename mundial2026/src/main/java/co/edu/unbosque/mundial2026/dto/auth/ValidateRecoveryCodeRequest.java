/**
 * Paquete que contiene los DTOs de las peticiones del flujo de autenticación.
 */
package co.edu.unbosque.mundial2026.dto.auth;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de petición para validar (sin consumir) el código de recuperación
 * de contraseña recibido por correo (paso 2 del flujo de recuperación).
 */
@Schema(description = "Petición para validar el código de recuperación sin consumirlo.")
public class ValidateRecoveryCodeRequest {

    /**
     * Correo electrónico al que se envió el código de recuperación.
     */
    @Schema(description = "Correo electrónico al que se envió el código.",
            example = "usuario@ejemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /**
     * Código de 6 dígitos recibido por correo.
     */
    @Schema(description = "Código de 6 dígitos recibido por correo.",
            example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String codigo;

    /** Constructor por defecto. */
    public ValidateRecoveryCodeRequest() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param email  Correo electrónico.
     * @param codigo Código de recuperación.
     */
    public ValidateRecoveryCodeRequest(String email, String codigo) {
        this.email = email;
        this.codigo = codigo;
    }

    /** @return El correo electrónico. */
    public String getEmail() { return email; }

    /** @param email El correo electrónico. */
    public void setEmail(String email) { this.email = email; }

    /** @return El código de recuperación. */
    public String getCodigo() { return codigo; }

    /** @param codigo El código de recuperación. */
    public void setCodigo(String codigo) { this.codigo = codigo; }

    @Override
    public int hashCode() { return Objects.hash(email, codigo); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ValidateRecoveryCodeRequest other)) return false;
        return Objects.equals(email, other.email) && Objects.equals(codigo, other.codigo);
    }

    @Override
    public String toString() {
        return "ValidateRecoveryCodeRequest [email=" + email + ", codigo=" + codigo + "]";
    }
}