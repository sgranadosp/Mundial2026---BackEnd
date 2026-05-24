/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.TeamDTO;
import co.edu.unbosque.mundial2026.model.Team;
import co.edu.unbosque.mundial2026.repository.TeamRepository;

/**
 * Servicio encargado de exponer las selecciones nacionales registradas en la
 * plataforma Mundial 2026 Hub al frontend.
 * <p>
 * Es un servicio de solo lectura: las selecciones se sincronizan desde la API
 * externa de fútbol (ver {@link FootballApiSyncService} y
 * {@link OpenFootballSyncService}), este servicio se limita a leerlas y
 * mapearlas a DTOs para consumo del cliente.
 * </p>
 */
@Service
public class TeamService {

    /**
     * Repositorio JPA de equipos.
     */
    @Autowired
    private TeamRepository teamRepo;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public TeamService() {
    }

    /**
     * Obtiene todas las selecciones registradas, ordenadas alfabéticamente
     * por nombre.
     *
     * @return Lista de {@link TeamDTO}. Vacía si aún no se ha hecho sync.
     */
    public List<TeamDTO> getAll() {
        List<Team> entities = teamRepo.findAll();
        List<TeamDTO> result = new ArrayList<>();
        for (Team entity : entities) {
            result.add(toDTO(entity));
        }
        result.sort(Comparator.comparing(
                TeamDTO::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    /**
     * Busca una selección por su código ISO-3166 alfa-3.
     *
     * @param isoCode Código ISO alfa-3 (se normaliza a mayúsculas).
     * @return El {@link TeamDTO} correspondiente, o {@code null} si no existe.
     */
    public TeamDTO getByIsoCode(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) {
            return null;
        }
        Optional<Team> found = teamRepo.findByIsoCode(isoCode.trim().toUpperCase());
        if (found.isEmpty()) {
            return null;
        }
        return toDTO(found.get());
    }

    /**
     * Mapeo manual de {@link Team} a {@link TeamDTO}. Se hace a mano (sin
     * ModelMapper) para evitar conflictos con el campo {@code group}, que
     * está mapeado a la columna {@code team_group} por ser palabra reservada
     * en MySQL.
     *
     * @param entity Entidad a mapear.
     * @return DTO con todos los campos copiados.
     */
    private TeamDTO toDTO(Team entity) {
        TeamDTO dto = new TeamDTO();
        dto.setId(entity.getId());
        dto.setExternalId(entity.getExternalId());
        dto.setName(entity.getName());
        dto.setIsoCode(entity.getIsoCode());
        dto.setShortName(entity.getShortName());
        dto.setCrestUrl(entity.getCrestUrl());
        dto.setGroup(entity.getGroup());
        return dto;
    }
}
