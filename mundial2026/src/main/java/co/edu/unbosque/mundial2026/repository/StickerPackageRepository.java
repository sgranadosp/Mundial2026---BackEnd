/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.StickerPackage;
import co.edu.unbosque.mundial2026.model.StickerPackage.PackageSource;
import co.edu.unbosque.mundial2026.model.StickerPackage.PackageStatus;

/**
 * Interfaz de repositorio para la entidad {@link StickerPackage}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code sticker_packages}. Los métodos personalizados permiten
 * consultar los paquetes pendientes de un usuario, validar códigos
 * promocionales para evitar uso duplicado, y auditar el historial de
 * paquetes otorgados por fuente.
 */
public interface StickerPackageRepository extends JpaRepository<StickerPackage, Long> {

    /**
     * Obtiene todos los paquetes de un usuario con un estado específico.
     * El uso principal es buscar paquetes con estado {@code PENDING} para
     * mostrar los paquetes disponibles para abrir.
     *
     * @param userId El ID del usuario.
     * @param status El estado del paquete (PENDING u OPENED).
     * @return Lista de paquetes del usuario con el estado indicado.
     */
    List<StickerPackage> findByUserIdAndStatus(Long userId, PackageStatus status);

    /**
     * Obtiene todos los paquetes de un usuario, independientemente del estado.
     * Se usa para mostrar el historial completo de paquetes en el perfil.
     *
     * @param userId El ID del usuario.
     * @return Lista de todos los paquetes del usuario.
     */
    List<StickerPackage> findByUserId(Long userId);

    /**
     * Obtiene todos los paquetes de un usuario filtrados por su origen.
     * Se usa en auditoría para verificar cuántos paquetes por inicio de sesión,
     * por pronóstico o por código promocional ha recibido un usuario,
     * para aplicar los límites anti-abuso.
     *
     * @param userId El ID del usuario.
     * @param source El origen del paquete.
     * @return Lista de paquetes del usuario con el origen indicado.
     */
    List<StickerPackage> findByUserIdAndSource(Long userId, PackageSource source);

    /**
     * Busca un paquete por el código promocional que lo originó.
     * Se usa para verificar si un código ya fue canjeado antes de otorgar
     * el paquete al usuario.
     *
     * @param promoCode El código promocional a buscar.
     * @return Un {@link Optional} con el paquete encontrado, o vacío si
     *         el código no ha sido canjeado todavía.
     */
    Optional<StickerPackage> findByPromoCode(String promoCode);

    /**
     * Cuenta el número de paquetes pendientes (no abiertos) de un usuario.
     * Se usa para mostrar el contador de paquetes disponibles en la interfaz.
     *
     * @param userId El ID del usuario.
     * @return Número de paquetes con estado {@code PENDING} del usuario.
     */
    long countByUserIdAndStatus(Long userId, PackageStatus status);

    /**
     * Verifica si un código promocional ya fue canjeado por algún usuario.
     * Se usa como validación rápida antes de procesar el canje.
     *
     * @param promoCode El código promocional a verificar.
     * @return {@code true} si el código ya fue usado.
     */
    boolean existsByPromoCode(String promoCode);
}