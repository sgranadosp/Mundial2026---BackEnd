/**
 * Paquete que contiene las interfaces de repositorio de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.Sticker;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.UserSticker;

/**
 * Repositorio JPA para la entidad {@link UserSticker} (colección personal
 * de láminas por usuario).
 * <p>
 * Toda consulta toma como filtro inicial el {@link User}, ya que estos datos
 * son específicos por usuario y nunca se cruzan entre cuentas.
 * </p>
 */
public interface UserStickerRepository extends JpaRepository<UserSticker, Long> {

    /**
     * Busca el registro de un usuario para una lámina específica del catálogo.
     *
     * @param user    Usuario propietario.
     * @param sticker Lámina del catálogo.
     * @return Un {@link Optional} con el registro si el usuario posee la lámina.
     */
    Optional<UserSticker> findByUserAndSticker(User user, Sticker sticker);

    /**
     * Devuelve todas las láminas que un usuario posee.
     *
     * @param user Usuario propietario.
     * @return Lista de todas sus láminas (con cantidad y fecha de obtención).
     */
    List<UserSticker> findByUser(User user);

    /**
     * Devuelve las láminas que un usuario posee de una selección específica.
     * Solo retorna láminas obtenidas; las faltantes se infieren al cruzar
     * con el catálogo en la capa de servicio.
     *
     * @param user        Usuario propietario. 
     * @param countryCode Código ASCII de la selección.
     * @return Lista de láminas obtenidas por el usuario para esa selección.
     */
    List<UserSticker> findByUserAndSticker_CountryCode(User user, String countryCode);

    /**
     * Cuenta cuántas láminas únicas posee un usuario (pegadas).
     * Útil para mostrar el progreso del álbum (ej. "120 / 294").
     *
     * @param user Usuario propietario.
     * @return Número de láminas únicas obtenidas (sin contar duplicados).
     */
    long countByUser(User user);
}