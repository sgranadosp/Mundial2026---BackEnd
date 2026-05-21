/**
 * Paquete que contiene los DTOs de las peticiones del flujo de autenticación.
 */
package co.edu.unbosque.mundial2026.dto.auth;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de petición para restablecer la contraseña usando el código de
 * recuperación enviado por correo (paso 3 del flujo de recuperación).
 * Si la operación tiene éxito, el código se consume y no se podrá reutilizar.
 */
@Schema(description = "Petición para restablecer la contraseña con código de recuperación.")
public class ResetPasswordRequest {

    /**
     * Correo electrónico del usuario.
     */
    @Schema(description = "Correo electrónico del usuario.",
            example = "usuario@ejemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /**
     * Código de 6 dígitos recibido por correo.
     */
    @Schema(description = "Código de 6 dígitos recibido por correo.",
            example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String codigo;

    /**
     * Nueva contraseña en texto plano. Debe cumplir la política de seguridad:
     * mínimo 8 caracteres, mayúscula, minúscula, número y símbolo.
     */
    @Schema(description = "Nueva contraseña en texto plano. Debe cumplir la política de seguridad.",
            example = "NuevaPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nuevaContrasena;

    /** Constructor por defecto. */
    public ResetPasswordRequest() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param email           Correo electrónico.
     * @param codigo          Código de recuperación.
     * @param nuevaContrasena Nueva contraseña.
     */
    public ResetPasswordRequest(String email, String codigo, String nuevaContrasena) {
        this.email = email;
        this.codigo = codigo;
        this.nuevaContrasena = nuevaContrasena;
    }

    /** @return El correo electrónico. */
    public String getEmail() { return email; }

    /** @param email El correo electrónico. */
    public void setEmail(String email) { this.email = email; }

    /** @return El código de recuperación. */
    public String getCodigo() { return codigo; }

    /** @param codigo El código de recuperación. */
    public void setCodigo(String codigo) { this.codigo = codigo; }

    /** @return La nueva contraseña. */
    public String getNuevaContrasena() { return nuevaContrasena; }

    /** @param nuevaContrasena La nueva contraseña. */
    public void setNuevaContrasena(String nuevaContrasena) { this.nuevaContrasena = nuevaContrasena; }

    @Override
    public int hashCode() { return Objects.hash(email, codigo); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResetPasswordRequest other)) return false;
        return Objects.equals(email, other.email) && Objects.equals(codigo, other.codigo);
    }

    @Override
    public String toString() {
        return "ResetPasswordRequest [email=" + email + ", codigo=" + codigo + ", nuevaContrasena=***]";
    }
}