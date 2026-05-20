/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.Sticker;
import co.edu.unbosque.mundial2026.model.Sticker.StickerCategory;
import co.edu.unbosque.mundial2026.model.Sticker.StickerRarity;
import co.edu.unbosque.mundial2026.model.Sticker.StickerStatus;

/**
 * Interfaz de repositorio para la entidad {@link Sticker}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code stickers}. Los métodos personalizados permiten listar láminas
 * por álbum con distintos filtros (estado, categoría, rareza) para los flujos
 * de visualización del álbum, y detectar repetidas disponibles para intercambio.
 */
public interface StickerRepository extends JpaRepository<Sticker, Long> {

    /**
     * Obtiene todas las láminas de un álbum específico.
     * Se usa para mostrar el contenido completo del álbum del usuario.
     *
     * @param albumId El ID del álbum.
     * @return Lista de todas las láminas del álbum.
     */
    List<Sticker> findByAlbumId(Long albumId);

    /**
     * Obtiene las láminas de un álbum con un estado específico.
     * El uso principal es buscar láminas con estado {@code DUPLICATE} para
     * mostrar las repetidas disponibles para intercambio.
     *
     * @param albumId El ID del álbum.
     * @param status  El estado de la lámina (PLACED, DUPLICATE o IN_EXCHANGE).
     * @return Lista de láminas del álbum con el estado indicado.
     */
    List<Sticker> findByAlbumIdAndStatus(Long albumId, StickerStatus status);

    /**
     * Obtiene las láminas de un álbum filtradas por categoría temática.
     * Se usa para mostrar secciones del álbum por tipo (selecciones,
     * estadios, trofeos, especiales).
     *
     * @param albumId  El ID del álbum.
     * @param category La categoría temática de las láminas.
     * @return Lista de láminas del álbum con la categoría indicada.
     */
    List<Sticker> findByAlbumIdAndCategory(Long albumId, StickerCategory category);

    /**
     * Obtiene las láminas de un álbum filtradas por rareza.
     * Se usa para mostrar secciones especiales con las láminas legendarias
     * o raras del usuario.
     *
     * @param albumId El ID del álbum.
     * @param rarity  La rareza de las láminas.
     * @return Lista de láminas del álbum con la rareza indicada.
     */
    List<Sticker> findByAlbumIdAndRarity(Long albumId, StickerRarity rarity);

    /**
     * Busca las láminas de un álbum con un código de catálogo específico.
     * Se usa para contar cuántas copias de una lámina tiene el usuario
     * (si hay más de una, la segunda en adelante son repetidas).
     *
     * @param albumId     El ID del álbum.
     * @param stickerCode El código de catálogo de la lámina (ej. "COL-01").
     * @return Lista de láminas del álbum con ese código (normalmente 1 o 2).
     */
    List<Sticker> findByAlbumIdAndStickerCode(Long albumId, String stickerCode);

    /**
     * Cuenta el número total de láminas únicas (no repetidas) en un álbum.
     * Se usa para calcular el porcentaje de completitud del álbum.
     *
     * @param albumId El ID del álbum.
     * @param status  El estado de las láminas a contar (normalmente {@code PLACED}).
     * @return Número de láminas del álbum con el estado indicado.
     */
    long countByAlbumIdAndStatus(Long albumId, StickerStatus status);
}