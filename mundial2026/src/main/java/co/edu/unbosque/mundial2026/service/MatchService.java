/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.MatchDTO;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Match.DataStatus;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.Match.Phase;
import co.edu.unbosque.mundial2026.repository.MatchRepository;

/**
 * Servicio encargado de la lógica de negocio relacionada con los partidos
 * del Mundial 2026.
 * Gestiona el ciclo de vida de los partidos, la conversión entre entidades
 * y DTOs, y la sincronización con la API externa. Implementa el principio
 * de "información confiable": si un dato no puede ser confirmado por el
 * proveedor, se marca como {@code PENDING_UPDATE} y el cliente muestra
 * un indicador visual de actualización pendiente.
 */
@Service
public class MatchService implements CRUDOperation<MatchDTO, Match> {

    /**
     * Repositorio JPA para operaciones de persistencia de partidos.
     */
    @Autowired
    private MatchRepository matchRepo;

    /**
     * Mapper para conversión entre entidades y DTOs.
     */
    @Autowired
    private ModelMapper modelMapper;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public MatchService() {
    }

    // =========================================================================
    // Implementación de CRUDOperation
    // =========================================================================

    /**
     * Crea o actualiza un partido a partir de datos recibidos de la API externa.
     * Si ya existe un partido con el mismo {@code externalId}, lo actualiza
     * en lugar de duplicarlo.
     *
     * @param data El DTO con los datos del partido.
     * @return 0 si fue creado/actualizado; 3 si los datos son inválidos.
     */
    @Override
    public int create(MatchDTO data) {
        if (data.getExternalId() == null || data.getScheduledAt() == null) {
            return 3;
        }
        if (matchRepo.existsByExternalId(data.getExternalId())) {
            return updateByExternalId(data);
        }
        Match entity = modelMapper.map(data, Match.class);
        entity.setStatus(MatchStatus.SCHEDULED);
        entity.setDataStatus(DataStatus.CONFIRMED);
        matchRepo.save(entity);
        return 0;
    }

    /**
     * Obtiene todos los partidos registrados como lista de DTOs.
     *
     * @return Lista de {@link MatchDTO} con todos los partidos.
     */
    @Override
    public List<MatchDTO> getAll() {
        List<Match> entities = matchRepo.findAll();
        List<MatchDTO> dtoList = new ArrayList<>();
        entities.forEach(entity -> dtoList.add(toDTO(entity)));
        return dtoList;
    }

    /**
     * Elimina un partido por su ID.
     *
     * @param id El ID del partido a eliminar.
     * @return 0 si fue eliminado; 2 si no existe.
     */
    @Override
    public int deleteById(Long id) {
        if (!matchRepo.existsById(id)) {
            return 2;
        }
        matchRepo.deleteById(id);
        return 0;
    }

    /**
     * Actualiza los datos de un partido por su ID interno.
     *
     * @param id      El ID interno del partido.
     * @param newData El DTO con los nuevos datos.
     * @return 0 si fue actualizado; 2 si no existe.
     */
    @Override
    public int updateById(Long id, MatchDTO newData) {
        Optional<Match> found = matchRepo.findById(id);
        if (found.isEmpty()) {
            return 2;
        }
        Match entity = found.get();
        applyUpdates(entity, newData);
        matchRepo.save(entity);
        return 0;
    }

    /** @return Número total de partidos registrados. */
    @Override
    public long count() {
        return matchRepo.count();
    }

    /** @return {@code true} si existe un partido con el ID indicado. */
    @Override
    public boolean exist(Long id) {
        return matchRepo.existsById(id);
    }

    // =========================================================================
    // Métodos específicos de partidos
    // =========================================================================

    /**
     * Obtiene un partido por su ID interno como DTO.
     *
     * @param id El ID interno del partido.
     * @return El {@link MatchDTO} correspondiente, o {@code null} si no existe.
     */
    public MatchDTO getById(Long id) {
        Optional<Match> found = matchRepo.findById(id);
        return found.map(this::toDTO).orElse(null);
    }

    /**
     * Obtiene todos los partidos filtrados por estado del ciclo de vida.
     * El caso principal es buscar partidos {@code SCHEDULED} para la agenda.
     *
     * @param status El estado del partido.
     * @return Lista de DTOs de partidos con el estado indicado.
     */
    public List<MatchDTO> getByStatus(MatchStatus status) {
        List<Match> entities = matchRepo.findByStatus(status);
        List<MatchDTO> dtoList = new ArrayList<>();
        entities.forEach(e -> dtoList.add(toDTO(e)));
        return dtoList;
    }

    /**
     * Obtiene todos los partidos de una fase del torneo.
     *
     * @param phase La fase del torneo.
     * @return Lista de DTOs de partidos de la fase indicada.
     */
    public List<MatchDTO> getByPhase(Phase phase) {
        List<Match> entities = matchRepo.findByPhase(phase);
        List<MatchDTO> dtoList = new ArrayList<>();
        entities.forEach(e -> dtoList.add(toDTO(e)));
        return dtoList;
    }

    /**
     * Obtiene los partidos de un equipo específico (como local o visitante).
     * Se usa para la agenda personalizada del usuario según su equipo favorito.
     *
     * @param teamId El ID interno del equipo.
     * @return Lista de DTOs de partidos del equipo.
     */
    public List<MatchDTO> getByTeam(Long teamId) {
        List<Match> entities = matchRepo.findByTeam(teamId, teamId);
        List<MatchDTO> dtoList = new ArrayList<>();
        entities.forEach(e -> dtoList.add(toDTO(e)));
        return dtoList;
    }

    /**
     * Obtiene los partidos programados dentro de un rango de fechas.
     * Implementa el filtro por fecha de la HU09.
     *
     * @param start Fecha y hora de inicio del rango (UTC).
     * @param end   Fecha y hora de fin del rango (UTC).
     * @return Lista de DTOs de partidos dentro del rango.
     */
    public List<MatchDTO> getByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Match> entities = matchRepo.findByScheduledAtBetween(start, end);
        List<MatchDTO> dtoList = new ArrayList<>();
        entities.forEach(e -> dtoList.add(toDTO(e)));
        return dtoList;
    }

    /**
     * Obtiene los partidos de un estadio específico.
     *
     * @param stadiumId El ID del estadio.
     * @return Lista de DTOs de partidos del estadio.
     */
    public List<MatchDTO> getByStadium(Long stadiumId) {
        List<Match> entities = matchRepo.findByStadiumId(stadiumId);
        List<MatchDTO> dtoList = new ArrayList<>();
        entities.forEach(e -> dtoList.add(toDTO(e)));
        return dtoList;
    }

    /**
     * Actualiza el estado y el marcador de un partido. Se llama cuando la API
     * externa reporta un cambio de estado o goles. Marca el dato como
     * {@code CONFIRMED} si la actualización llegó correctamente.
     *
     * @param externalId  El ID externo del partido.
     * @param newStatus   El nuevo estado del ciclo de vida.
     * @param homeScore   Goles del equipo local (puede ser nulo si no ha terminado).
     * @param awayScore   Goles del equipo visitante.
     * @return 0 si fue actualizado; 2 si no existe el partido.
     */
    public int updateMatchResult(Long externalId, MatchStatus newStatus,
                                  Integer homeScore, Integer awayScore) {
        Optional<Match> found = matchRepo.findByExternalId(externalId);
        if (found.isEmpty()) {
            return 2;
        }
        Match entity = found.get();
        entity.setStatus(newStatus);
        if (homeScore != null) entity.setHomeScore(homeScore);
        if (awayScore != null) entity.setAwayScore(awayScore);
        entity.setDataStatus(DataStatus.CONFIRMED);
        matchRepo.save(entity);
        return 0;
    }

    /**
     * Marca un partido como con datos pendientes de actualización cuando
     * la fuente externa falla o responde lento. Implementa la degradación
     * elegante requerida por el proyecto.
     *
     * @param externalId El ID externo del partido.
     * @return 0 si fue marcado; 2 si no existe.
     */
    public int markAsPendingUpdate(Long externalId) {
        Optional<Match> found = matchRepo.findByExternalId(externalId);
        if (found.isEmpty()) {
            return 2;
        }
        Match entity = found.get();
        entity.setDataStatus(DataStatus.PENDING_UPDATE);
        matchRepo.save(entity);
        return 0;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Actualiza un partido existente a partir de un DTO con datos externos.
     *
     * @param data El DTO con los datos actualizados.
     * @return 0 si fue actualizado; 2 si no existe.
     */
    private int updateByExternalId(MatchDTO data) {
        Optional<Match> found = matchRepo.findByExternalId(data.getExternalId());
        if (found.isEmpty()) {
            return 2;
        }
        Match entity = found.get();
        applyUpdates(entity, data);
        matchRepo.save(entity);
        return 0;
    }

    /**
     * Aplica los cambios del DTO a la entidad sin reemplazar el objeto completo.
     *
     * @param entity  La entidad a actualizar.
     * @param newData El DTO con los nuevos valores.
     */
    private void applyUpdates(Match entity, MatchDTO newData) {
        if (newData.getScheduledAt() != null) entity.setScheduledAt(newData.getScheduledAt());
        if (newData.getStatus() != null) entity.setStatus(newData.getStatus());
        if (newData.getHomeScore() != null) entity.setHomeScore(newData.getHomeScore());
        if (newData.getAwayScore() != null) entity.setAwayScore(newData.getAwayScore());
        if (newData.getPhase() != null) entity.setPhase(newData.getPhase());
        if (newData.getDataStatus() != null) entity.setDataStatus(newData.getDataStatus());
    }

    /**
     * Convierte una entidad {@link Match} a {@link MatchDTO} aplanando los datos
     * de equipos y estadio para evitar llamadas adicionales desde el cliente.
     *
     * @param entity La entidad a convertir.
     * @return El DTO con los datos del partido.
     */
    private MatchDTO toDTO(Match entity) {
        MatchDTO dto = new MatchDTO();
        dto.setId(entity.getId());
        dto.setExternalId(entity.getExternalId());
        dto.setScheduledAt(entity.getScheduledAt());
        dto.setPhase(entity.getPhase());
        dto.setStatus(entity.getStatus());
        dto.setHomeScore(entity.getHomeScore());
        dto.setAwayScore(entity.getAwayScore());
        dto.setGroupName(entity.getGroupName());
        dto.setMatchday(entity.getMatchday());
        dto.setDataStatus(entity.getDataStatus());

        if (entity.getHomeTeam() != null) {
            dto.setHomeTeamId(entity.getHomeTeam().getId());
            dto.setHomeTeamName(entity.getHomeTeam().getName());
            dto.setHomeTeamIsoCode(entity.getHomeTeam().getIsoCode());
            dto.setHomeTeamCrestUrl(entity.getHomeTeam().getCrestUrl());
        }
        if (entity.getAwayTeam() != null) {
            dto.setAwayTeamId(entity.getAwayTeam().getId());
            dto.setAwayTeamName(entity.getAwayTeam().getName());
            dto.setAwayTeamIsoCode(entity.getAwayTeam().getIsoCode());
            dto.setAwayTeamCrestUrl(entity.getAwayTeam().getCrestUrl());
        }
        if (entity.getStadium() != null) {
            dto.setStadiumId(entity.getStadium().getId());
            dto.setStadiumName(entity.getStadium().getName());
            dto.setStadiumCity(entity.getStadium().getCity());
            dto.setStadiumTimezone(entity.getStadium().getTimezone());
        }
        return dto;
    }
}