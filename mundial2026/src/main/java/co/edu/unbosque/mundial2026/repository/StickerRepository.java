/**
 * Paquete que contiene las interfaces de repositorio de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.Sticker;

/**
 * Repositorio JPA para la entidad {@link Sticker} (catálogo maestro de láminas).
 * <p>
 * Todas las consultas operan sobre el catálogo completo de 294 láminas, que
 * es información estática igual para todos los usuarios. La información
 * personal de qué láminas posee cada usuario vive en
 * {@code UserStickerRepository}.
 * </p>
 */
public interface StickerRepository extends JpaRepository<Sticker, Long> {

    /**
     * Busca una lámina por su código único.
     *
     * @param code Código de la lámina (ej. {@code argentina_01}).
     * @return Un {@link Optional} con la lámina si existe.
     */
    Optional<Sticker> findByCode(String code);

    /**
     * Devuelve todas las láminas de una selección específica, ordenadas
     * por posición ascendente. 
     *
     * @param countryCode Código ASCII de la selección (ej. {@code colombia}).
     * @return Lista de las 6 láminas de la selección, en orden 1 a 6.
     */
    List<Sticker> findByCountryCodeOrderByPositionAsc(String countryCode);

    /**
     * Verifica si ya existe al menos una lámina en el catálogo.
     * Útil para decidir en el arranque si hay que precargar las 294 láminas.
     *
     * @return {@code true} si el catálogo tiene al menos una fila.
     */
    @Override
    long count();
}