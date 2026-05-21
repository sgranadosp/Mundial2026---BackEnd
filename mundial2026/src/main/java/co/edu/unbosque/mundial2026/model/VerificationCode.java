package co.edu.unbosque.mundial2026.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa un código de verificación de un solo uso emitido
 * para un correo electrónico.
 * <p>
 * Se usa tanto para verificar la cuenta tras registro como para iniciar el
 * flujo de recuperación de contraseña. El código se guarda encriptado con AES
 * (mismo esquema que los datos sensibles del usuario) y se asocia al email
 * encriptado para evitar exposición tras una inspección directa de la tabla.
 * </p>
 * <p>
 * Se mantiene en una tabla independiente porque:
 * <ul>
 *   <li>Es un valor de un solo uso, no parte del perfil del usuario.</li>
 *   <li>Permite expiración temporal sin afectar la fila del usuario.</li>
 *   <li>Reduce la superficie de exposición al consultar {@code users}.</li>
 *   <li>Permite múltiples códigos históricos si se desea auditoría futura.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "verification_codes",
       indexes = { @Index(name = "idx_verification_email", columnList = "email") })
public class VerificationCode {

    /**
     * Identificador único del registro de código.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Correo electrónico (encriptado con AES) al que se emitió el código.
     * No es FK al usuario porque el flujo de recuperación puede iniciarse
     * antes de que la cuenta exista (defensa en profundidad ante enumeración).
     */
    @Column(nullable = false)
    private String email;

    /**
     * Código de 6 dígitos encriptado con AES.
     */
    @Column(nullable = false)
    private String code;

    /**
     * Propósito del código: verificación de cuenta nueva o recuperación de
     * contraseña existente.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    /**
     * Marca temporal de emisión del código. Permite implementar expiración
     * (por ejemplo, "el código vale 15 minutos") sin cambios estructurales.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Enumeración del propósito del código emitido.
     */
    public enum Purpose {
        /** Verificación de cuenta tras el registro inicial. */
        REGISTRATION,
        /** Recuperación de contraseña olvidada. */
        PASSWORD_RECOVERY
    }

    /** Constructor por defecto requerido por JPA. */
    public VerificationCode() {
    }

    /**
     * Constructor con todos los campos funcionales.
     *
     * @param email   Correo encriptado al que se emite el código.
     * @param code    Código de 6 dígitos encriptado.
     * @param purpose Propósito del código.
     */
    public VerificationCode(String email, String code, Purpose purpose) {
        this.email = email;
        this.code = code;
        this.purpose = purpose;
        this.createdAt = LocalDateTime.now();
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del código. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El email encriptado. */
    public String getEmail() { return email; }

    /** @param email El nuevo email encriptado. */
    public void setEmail(String email) { this.email = email; }

    /** @return El código encriptado. */
    public String getCode() { return code; }

    /** @param code El nuevo código encriptado. */
    public void setCode(String code) { this.code = code; }

    /** @return El propósito del código. */
    public Purpose getPurpose() { return purpose; }

    /** @param purpose El nuevo propósito. */
    public void setPurpose(Purpose purpose) { this.purpose = purpose; }

    /** @return La fecha de emisión. */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt La nueva fecha de emisión. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VerificationCode other = (VerificationCode) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "VerificationCode [id=" + id + ", purpose=" + purpose
                + ", createdAt=" + createdAt + "]";
    }
}