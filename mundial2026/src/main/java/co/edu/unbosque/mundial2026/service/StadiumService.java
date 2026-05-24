/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.StadiumDTO;
import co.edu.unbosque.mundial2026.model.Stadium;
import co.edu.unbosque.mundial2026.repository.StadiumRepository;

/**
 * Servicio encargado de exponer los estadios y ciudades sede del Mundial 2026
 * al frontend.
 * <p>
 * Es un servicio de solo lectura: los estadios se sincronizan desde la API
 * externa de fútbol (ver {@link FootballApiSyncService} y
 * {@link OpenFootballSyncService}); este servicio se limita a leerlos y
 * mapearlos a DTOs.
 * </p>
 */
@Service
public class StadiumService {

    /**
     * Repositorio JPA de estadios.
     */
    @Autowired
    private StadiumRepository stadiumRepo;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public StadiumService() {
    }

    /**
     * Obtiene todos los estadios registrados, ordenados por ciudad.
     *
     * @return Lista de {@link StadiumDTO}. Vacía si no hay estadios sincronizados.
     */
    public List<StadiumDTO> getAll() {
        List<Stadium> entities = stadiumRepo.findAll();
        List<StadiumDTO> result = new ArrayList<>();
        for (Stadium entity : entities) {
            result.add(toDTO(entity));
        }
        result.sort(Comparator.comparing(
                StadiumDTO::getCity,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    /**
     * Devuelve las ciudades únicas donde se ubican los estadios sede,
     * en orden alfabético. Pensado para llenar un desplegable de "ciudad
     * preferida" en el perfil del usuario.
     *
     * @return Lista de ciudades sin duplicados, ordenadas alfabéticamente.
     */
    public List<String> getDistinctCities() {
        List<Stadium> entities = stadiumRepo.findAll();
        Set<String> uniqueCities = new LinkedHashSet<>();
        for (Stadium s : entities) {
            if (s.getCity() != null && !s.getCity().isBlank()) {
                uniqueCities.add(s.getCity().trim());
            }
        }
        List<String> result = new ArrayList<>(uniqueCities);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    /**
     * Mapeo manual de {@link Stadium} a {@link StadiumDTO}. Se hace a mano
     * (sin ModelMapper) por consistencia con TeamService y para evitar
     * cualquier conflicto con campos opcionales o tipos numéricos boxed.
     *
     * @param entity Entidad a mapear.
     * @return DTO con todos los campos copiados.
     */
    private StadiumDTO toDTO(Stadium entity) {
        StadiumDTO dto = new StadiumDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCity(entity.getCity());
        dto.setCountry(entity.getCountry());
        dto.setTimezone(entity.getTimezone());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setCapacity(entity.getCapacity());
        dto.setImageUrl(entity.getImageUrl());
        return dto;
    }
}
