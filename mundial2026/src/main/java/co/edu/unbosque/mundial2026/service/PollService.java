/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.PollGroupDTO;
import co.edu.unbosque.mundial2026.dto.PredictionDTO;
import co.edu.unbosque.mundial2026.dto.RankingDTO;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.PollGroup;
import co.edu.unbosque.mundial2026.model.Prediction;
import co.edu.unbosque.mundial2026.model.Prediction.PredictionStatus;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.PollGroupRepository;
import co.edu.unbosque.mundial2026.repository.PredictionRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.util.ScoringUtil;

/**
 * Servicio encargado de la lógica de negocio de pollas futboleras y pronósticos
 * en la plataforma Mundial 2026 Hub.
 * Gestiona el ciclo completo: creación de grupos, incorporación de miembros
 * por código de invitación, registro y edición de pronósticos, bloqueo
 * automático al iniciar un partido, evaluación de puntajes al terminar
 * y publicación del ranking actualizado.
 * Convenciones de códigos de retorno:
 * <ul>
 *   <li>0 — Éxito.</li>
 *   <li>1 — Dato duplicado o restricción de negocio violada.</li>
 *   <li>2 — Entidad no encontrada.</li>
 *   <li>3 — Error genérico / datos inválidos.</li>
 *   <li>4 — Operación no permitida en el estado actual (pronóstico bloqueado).</li>
 * </ul>
 */
@Service
public class PollService {

    /**
     * Repositorio JPA para grupos de polla.
     */
    @Autowired
    private PollGroupRepository pollGroupRepo;

    /**
     * Repositorio JPA para pronósticos.
     */
    @Autowired
    private PredictionRepository predictionRepo;

    /**
     * Repositorio JPA para partidos (verificar estado antes de aceptar pronósticos).
     */
    @Autowired
    private MatchRepository matchRepo;

    /**
     * Repositorio JPA para usuarios (resolver miembros del grupo).
     */
    @Autowired
    private UserRepository userRepo;

    /**
     * Utilidad para el cálculo de puntajes de pronósticos.
     */
    @Autowired
    private ScoringUtil scoringUtil;

    /**
     * Mapper para conversión entre entidades y DTOs.
     */
    @Autowired
    private ModelMapper modelMapper;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public PollService() {
    }

    // =========================================================================
    // Gestión de grupos
    // =========================================================================

    /**
     * Crea un nuevo grupo de polla. Genera un código de invitación único de
     * 8 caracteres y registra al creador como primer miembro del grupo.
     *
     * @param name    Nombre del grupo.
     * @param ownerId ID del usuario creador.
     * @return El {@link PollGroupDTO} creado con el código de invitación,
     *         o {@code null} si el usuario no existe.
     */
    public PollGroupDTO createGroup(String name, Long ownerId) {
        Optional<User> owner = userRepo.findById(ownerId);
        if (owner.isEmpty()) {
            return null;
        }
        String inviteCode = generateUniqueInviteCode();
        PollGroup group = new PollGroup(name, owner.get(), inviteCode);
        group.getMembers().add(owner.get());
        pollGroupRepo.save(group);
        return toGroupDTO(group);
    }

    /**
     * Une a un usuario a un grupo de polla existente usando el código de invitación.
     *
     * @param userId     ID del usuario que quiere unirse.
     * @param inviteCode El código de invitación del grupo.
     * @return 0 si se unió exitosamente; 1 si ya era miembro; 2 si el grupo
     *         o usuario no existe.
     */
    public int joinGroup(Long userId, String inviteCode) {
        Optional<PollGroup> group = pollGroupRepo.findByInviteCode(inviteCode);
        Optional<User> user = userRepo.findById(userId);

        if (group.isEmpty() || user.isEmpty()) {
            return 2;
        }
        if (pollGroupRepo.isMemberOfGroup(group.get().getId(), userId)) {
            return 1;
        }
        group.get().getMembers().add(user.get());
        pollGroupRepo.save(group.get());
        return 0;
    }

    /**
     * Obtiene todos los grupos activos en los que participa un usuario,
     * tanto como creador como miembro.
     *
     * @param userId El ID del usuario.
     * @return Lista de {@link PollGroupDTO} de los grupos del usuario.
     */
    public List<PollGroupDTO> getGroupsByUser(Long userId) {
        List<PollGroup> groups = pollGroupRepo.findActiveGroupsByMemberId(userId);
        List<PollGroupDTO> dtoList = new ArrayList<>();
        groups.forEach(g -> dtoList.add(toGroupDTO(g)));
        return dtoList;
    }

    /**
     * Obtiene el detalle de un grupo por su ID.
     *
     * @param groupId El ID del grupo.
     * @return El {@link PollGroupDTO} del grupo, o {@code null} si no existe.
     */
    public PollGroupDTO getGroupById(Long groupId) {
        Optional<PollGroup> found = pollGroupRepo.findById(groupId);
        return found.map(this::toGroupDTO).orElse(null);
    }

    /**
     * Desactiva un grupo de polla. Solo el creador puede hacerlo.
     *
     * @param groupId El ID del grupo.
     * @param ownerId El ID del usuario que solicita la desactivación.
     * @return 0 si fue desactivado; 2 si no existe; 4 si no es el creador.
     */
    public int deactivateGroup(Long groupId, Long ownerId) {
        Optional<PollGroup> found = pollGroupRepo.findById(groupId);
        if (found.isEmpty()) {
            return 2;
        }
        PollGroup group = found.get();
        if (!group.getOwner().getId().equals(ownerId)) {
            return 4;
        }
        group.setActive(false);
        pollGroupRepo.save(group);
        return 0;
    }

    // =========================================================================
    // Gestión de pronósticos
    // =========================================================================

    /**
     * Registra un pronóstico de un usuario para un partido dentro de un grupo.
     * Solo se permite si el partido tiene estado {@code SCHEDULED}.
     * Si ya existe un pronóstico del mismo usuario para ese partido y grupo,
     * retorna error de duplicado.
     *
     * @param data El DTO con los datos del pronóstico.
     * @return 0 si fue registrado; 1 si ya existe; 2 si partido, grupo o usuario
     *         no existen; 4 si el partido ya no acepta pronósticos (LIVE o FINISHED).
     */
    public int submitPrediction(PredictionDTO data) {
        Optional<Match> match = matchRepo.findById(data.getMatchId());
        Optional<PollGroup> group = pollGroupRepo.findById(data.getPollGroupId());
        Optional<User> user = userRepo.findById(data.getUserId());

        if (match.isEmpty() || group.isEmpty() || user.isEmpty()) {
            return 2;
        }
        if (match.get().getStatus() != MatchStatus.SCHEDULED) {
            return 4;
        }
        Optional<Prediction> existing = predictionRepo.findByUserIdAndMatchIdAndPollGroupId(
                data.getUserId(), data.getMatchId(), data.getPollGroupId());
        if (existing.isPresent()) {
            return 1;
        }

        Prediction prediction = new Prediction(
                user.get(), match.get(), group.get(),
                data.getPredictedHomeScore(), data.getPredictedAwayScore()
        );
        predictionRepo.save(prediction);
        return 0;
    }

    /**
     * Edita un pronóstico existente. Solo se permite si el pronóstico tiene
     * estado {@code OPEN} (el partido aún no ha iniciado).
     *
     * @param predictionId       El ID del pronóstico a editar.
     * @param userId             El ID del usuario dueño del pronóstico.
     * @param newHomeScore       Nuevo marcador predicho para el equipo local.
     * @param newAwayScore       Nuevo marcador predicho para el equipo visitante.
     * @return 0 si fue editado; 2 si no existe; 4 si está bloqueado o no pertenece
     *         al usuario.
     */
    public int editPrediction(Long predictionId, Long userId,
                               Integer newHomeScore, Integer newAwayScore) {
        Optional<Prediction> found = predictionRepo.findById(predictionId);
        if (found.isEmpty()) {
            return 2;
        }
        Prediction prediction = found.get();
        if (!prediction.getUser().getId().equals(userId)) {
            return 4;
        }
        if (prediction.getStatus() != PredictionStatus.OPEN) {
            return 4;
        }
        prediction.setPredictedHomeScore(newHomeScore);
        prediction.setPredictedAwayScore(newAwayScore);
        prediction.setSubmittedAt(LocalDateTime.now());
        predictionRepo.save(prediction);
        return 0;
    }

    /**
     * Bloquea todos los pronósticos asociados a un partido cuando este inicia
     * (transición {@code OPEN → LOCKED}). Se llama desde el scheduler cuando
     * el partido cambia a estado {@code LIVE}.
     *
     * @param matchId El ID del partido que inicia.
     */
    public void lockPredictionsForMatch(Long matchId) {
        List<Prediction> openPredictions = predictionRepo.findByMatchIdAndStatus(matchId, PredictionStatus.OPEN);
        openPredictions.forEach(p -> {
            p.setStatus(PredictionStatus.LOCKED);
            predictionRepo.save(p);
        });
    }

    /**
     * Evalúa todos los pronósticos de un partido cuando este finaliza.
     * Calcula los puntos con {@link ScoringUtil} y transiciona el estado
     * a {@code EVALUATED} (transición {@code LOCKED → EVALUATED}).
     * Se llama desde el scheduler cuando el partido cambia a estado {@code FINISHED}.
     *
     * @param matchId      El ID del partido finalizado.
     * @param homeScore    Goles finales del equipo local.
     * @param awayScore    Goles finales del equipo visitante.
     */
    public void evaluatePredictionsForMatch(Long matchId, int homeScore, int awayScore) {
        List<Prediction> locked = predictionRepo.findByMatchIdAndStatus(matchId, PredictionStatus.LOCKED);
        locked.forEach(p -> {
            int points = scoringUtil.calculatePoints(
                    p.getPredictedHomeScore(), p.getPredictedAwayScore(),
                    homeScore, awayScore
            );
            p.setPointsEarned(points);
            p.setStatus(PredictionStatus.EVALUATED);
            predictionRepo.save(p);
        });
    }

    /**
     * Obtiene el historial de pronósticos de un usuario en todos sus grupos,
     * incluyendo los resultados reales para los ya evaluados.
     *
     * @param userId El ID del usuario.
     * @return Lista de {@link PredictionDTO} del usuario.
     */
    public List<PredictionDTO> getPredictionsByUser(Long userId) {
        List<Prediction> predictions = predictionRepo.findByUserId(userId);
        List<PredictionDTO> dtoList = new ArrayList<>();
        predictions.forEach(p -> dtoList.add(toPredictionDTO(p)));
        return dtoList;
    }

    /**
     * Obtiene los pronósticos de un usuario en un grupo específico.
     *
     * @param userId      El ID del usuario.
     * @param pollGroupId El ID del grupo.
     * @return Lista de {@link PredictionDTO} del usuario en el grupo.
     */
    public List<PredictionDTO> getPredictionsByUserAndGroup(Long userId, Long pollGroupId) {
        List<Prediction> predictions = predictionRepo.findByUserIdAndPollGroupId(userId, pollGroupId);
        List<PredictionDTO> dtoList = new ArrayList<>();
        predictions.forEach(p -> dtoList.add(toPredictionDTO(p)));
        return dtoList;
    }

    // =========================================================================
    // Ranking
    // =========================================================================

    /**
     * Calcula y devuelve el ranking actualizado de un grupo de polla.
     * Ordena los miembros por puntos totales descendente y asigna posiciones.
     *
     * @param groupId El ID del grupo de polla.
     * @return Lista de {@link RankingDTO} ordenada por puntos descendente,
     *         o lista vacía si el grupo no existe.
     */
    public List<RankingDTO> getRankingByGroup(Long groupId) {
        Optional<PollGroup> groupOpt = pollGroupRepo.findById(groupId);
        if (groupOpt.isEmpty()) {
            return new ArrayList<>();
        }

        PollGroup group = groupOpt.get();
        List<RankingDTO> ranking = new ArrayList<>();

        group.getMembers().forEach(member -> {
            Integer totalPoints = predictionRepo.sumPointsByUserIdAndPollGroupId(member.getId(), groupId);
            Integer exactScores = predictionRepo.countExactScoresByUserIdAndPollGroupId(member.getId(), groupId);
            List<Prediction> allPredictions = predictionRepo.findByUserIdAndPollGroupId(member.getId(), groupId);

            RankingDTO entry = new RankingDTO();
            entry.setUserId(member.getId());
            entry.setUsername(member.getUsername());
            entry.setPollGroupId(groupId);
            entry.setPollGroupName(group.getName());
            entry.setTotalPoints(totalPoints != null ? totalPoints : 0);
            entry.setTotalPredictions(allPredictions.size());
            entry.setExactScores(exactScores != null ? exactScores : 0);
            entry.setCorrectResults(totalPoints != null ? totalPoints - (exactScores != null ? exactScores * 2 : 0) : 0);
            ranking.add(entry);
        });

        ranking.sort((a, b) -> b.getTotalPoints().compareTo(a.getTotalPoints()));
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setPosition(i + 1);
        }
        return ranking;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Genera un código de invitación único de 8 caracteres en mayúsculas.
     * Reintenta hasta encontrar uno que no esté ya en uso.
     *
     * @return Un código de invitación único.
     */
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (pollGroupRepo.existsByInviteCode(code));
        return code;
    }

    /**
     * Convierte una entidad {@link PollGroup} a {@link PollGroupDTO}.
     *
     * @param group La entidad del grupo.
     * @return El DTO del grupo.
     */
    private PollGroupDTO toGroupDTO(PollGroup group) {
        PollGroupDTO dto = new PollGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setInviteCode(group.getInviteCode());
        dto.setOwnerId(group.getOwner().getId());
        dto.setOwnerUsername(group.getOwner().getUsername());
        dto.setMembersCount(group.getMembers().size());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setActive(group.isActive());
        return dto;
    }

    /**
     * Convierte una entidad {@link Prediction} a {@link PredictionDTO},
     * incluyendo datos del partido y del resultado real si está evaluado.
     *
     * @param p La entidad del pronóstico.
     * @return El DTO del pronóstico.
     */
    private PredictionDTO toPredictionDTO(Prediction p) {
        PredictionDTO dto = new PredictionDTO();
        dto.setId(p.getId());
        dto.setUserId(p.getUser().getId());
        dto.setUsername(p.getUser().getUsername());
        dto.setMatchId(p.getMatch().getId());
        dto.setPollGroupId(p.getPollGroup().getId());
        dto.setPollGroupName(p.getPollGroup().getName());
        dto.setPredictedHomeScore(p.getPredictedHomeScore());
        dto.setPredictedAwayScore(p.getPredictedAwayScore());
        dto.setSubmittedAt(p.getSubmittedAt());
        dto.setStatus(p.getStatus());
        dto.setPointsEarned(p.getPointsEarned());

        if (p.getMatch().getHomeTeam() != null) {
            dto.setHomeTeamName(p.getMatch().getHomeTeam().getName());
        }
        if (p.getMatch().getAwayTeam() != null) {
            dto.setAwayTeamName(p.getMatch().getAwayTeam().getName());
        }
        if (p.getStatus() == PredictionStatus.EVALUATED) {
            dto.setActualHomeScore(p.getMatch().getHomeScore());
            dto.setActualAwayScore(p.getMatch().getAwayScore());
        }
        return dto;
    }
}