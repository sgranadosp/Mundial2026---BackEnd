/**
 * Paquete que contiene los DTOs de las peticiones del flujo de autenticación.
 */
package co.edu.unbosque.mundial2026.dto.auth;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de petición para verificar el código de 6 dígitos enviado al correo
 * tras el registro de un nuevo usuario.
 */
@Schema(description = "Petición para verificar el código de registro enviado por correo.")
public class VerifyCodeRequest {

    /**
     * Correo electrónico del usuario al que se envió el código.
     */
    @Schema(description = "Correo electrónico al que se envió el código de verificación.",
            example = "usuario@ejemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /**
     * Código de 6 dígitos recibido por correo.
     */
    @Schema(description = "Código de 6 dígitos recibido por correo.",
            example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String codigo;

    /** Constructor por defecto. */
    public VerifyCodeRequest() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param email  Correo electrónico.
     * @param codigo Código de verificación.
     */
    public VerifyCodeRequest(String email, String codigo) {
        this.email = email;
        this.codigo = codigo;
    }

    /** @return El correo electrónico. */
    public String getEmail() { return email; }

    /** @param email El correo electrónico. */
    public void setEmail(String email) { this.email = email; }

    /** @return El código de verificación. */
    public String getCodigo() { return codigo; }

    /** @param codigo El código de verificación. */
    public void setCodigo(String codigo) { this.codigo = codigo; }

    @Override
    public int hashCode() { return Objects.hash(email, codigo); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VerifyCodeRequest other)) return false;
        return Objects.equals(email, other.email) && Objects.equals(codigo, other.codigo);
    }

    @Override
    public String toString() {
        return "VerifyCodeRequest [email=" + email + ", codigo=" + codigo + "]";
    }
}