/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.Album;

/**
 * Interfaz de repositorio para la entidad {@link Album}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code albums}. La relación con {@link co.edu.unbosque.mundial2026.model.User}
 * es uno a uno, por lo que el método principal es la búsqueda por usuario.
 * El álbum se crea automáticamente al registrar al usuario en el sistema.
 */
public interface AlbumRepository extends JpaRepository<Album, Long> {

    /**
     * Busca el álbum de un usuario específico por su ID.
     * Se usa en todos los endpoints del módulo de álbum para recuperar
     * la colección del usuario autenticado.
     *
     * @param userId El ID del usuario propietario.
     * @return Un {@link Optional} con el álbum encontrado, o vacío si
     *         el usuario no tiene álbum (no debería ocurrir en producción
     *         ya que se crea en el registro).
     */
    Optional<Album> findByUserId(Long userId);

    /**
     * Verifica si un usuario ya tiene un álbum registrado.
     * Se usa en el proceso de registro para evitar crear álbumes duplicados
     * si se llama al método de inicialización más de una vez.
     *
     * @param userId El ID del usuario a verificar.
     * @return {@code true} si el usuario ya tiene un álbum asociado.
     */
    boolean existsByUserId(Long userId);
}