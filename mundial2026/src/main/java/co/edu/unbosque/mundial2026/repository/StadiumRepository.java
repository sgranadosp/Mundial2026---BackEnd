/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.Stadium;

/**
 * Interfaz de repositorio para la entidad {@link Stadium}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar
 * sobre la tabla {@code stadiums}. Los métodos personalizados permiten filtrar
 * estadios por ciudad y por país para el módulo de agenda personalizada y
 * para el filtro de partidos por sede (HU09 — Filtrar partidos por fecha /
 * ciudad / estadio).
 */
public interface StadiumRepository extends JpaRepository<Stadium, Long> {

    /**
     * Busca un estadio por su nombre oficial.
     * Se usa en la carga inicial de datos ({@code LoadDatabase}) y en
     * búsquedas directas desde el panel de administración.
     *
     * @param name El nombre oficial del estadio.
     * @return Un {@link Optional} con el estadio encontrado, o vacío si no existe.
     */
    Optional<Stadium> findByName(String name);

    /**
     * Obtiene todos los estadios ubicados en una ciudad específica.
     * Se usa cuando el usuario filtra partidos por ciudad en su agenda.
     *
     * @param city El nombre de la ciudad.
     * @return Lista de estadios en la ciudad indicada.
     */
    List<Stadium> findByCity(String city);

    /**
     * Obtiene todos los estadios de un país anfitrión específico
     * ("USA", "Canada" o "Mexico").
     * Se usa para mostrar las sedes disponibles por país en el módulo de
     * planificación logística del aficionado viajero.
     *
     * @param country El nombre del país anfitrión.
     * @return Lista de estadios en el país indicado.
     */
    List<Stadium> findByCountry(String country);

    /**
     * Obtiene todos los estadios de una ciudad dentro de un país específico.
     * Más preciso que filtrar solo por ciudad cuando hay ciudades con el
     * mismo nombre en diferentes países.
     *
     * @param city    El nombre de la ciudad.
     * @param country El nombre del país anfitrión.
     * @return Lista de estadios que coinciden con ciudad y país.
     */
    List<Stadium> findByCityAndCountry(String city, String country);
}