/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.VerificationCode;
import co.edu.unbosque.mundial2026.model.VerificationCode.Purpose;

/**
 * Repositorio JPA para la entidad {@link VerificationCode}.
 * <p>
 * Provee operaciones de consulta y limpieza para códigos de verificación
 * de un solo uso (registro y recuperación de contraseña). Como el correo
 * se almacena encriptado con AES, las consultas deben recibir el email ya
 * encriptado.
 * </p>
 */
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * Busca el código vigente para un email y propósito específico.
     * Solo debe existir uno por combinación (los anteriores se eliminan
     * cuando se emite uno nuevo).
     *
     * @param email   Email encriptado del usuario.
     * @param purpose Propósito del código (REGISTRATION o PASSWORD_RECOVERY).
     * @return Un {@link Optional} con el código si existe.
     */
    Optional<VerificationCode> findByEmailAndPurpose(String email, Purpose purpose);

    /**
     * Elimina todos los códigos asociados a un email y propósito.
     * Se usa antes de emitir un código nuevo para garantizar que solo
     * exista un código vigente por usuario y por flujo.
     *
     * @param email   Email encriptado del usuario.
     * @param purpose Propósito a borrar.
     */
    void deleteByEmailAndPurpose(String email, Purpose purpose);

    /**
     * Elimina todos los códigos asociados a un email, sin importar el propósito.
     * Útil al desactivar o eliminar una cuenta.
     *
     * @param email Email encriptado del usuario.
     */
    void deleteByEmail(String email);
}