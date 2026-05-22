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
 * Servicio encargado de la lógica de negocio de pollas futboleras y pronósticos.
 *
 * Convenciones de códigos de retorno:
 * <ul>
 *   <li>0 — Éxito.</li>
 *   <li>1 — Dato duplicado o restricción de negocio violada (ej. ya miembro,
 *       ya tiene predicción para ese partido en ese grupo).</li>
 *   <li>2 — Entidad no encontrada.</li>
 *   <li>3 — Error genérico / datos inválidos.</li>
 *   <li>4 — Operación no permitida en el estado actual (predicción bloqueada,
 *       no es el dueño del grupo, predicción no modificable, etc.).</li>
 *   <li>5 — Caso particular: el usuario intenta unirse a un grupo del cual
 *       ya es el creador.</li>
 * </ul>
 */
@Service
public class PollService {

    @Autowired
    private PollGroupRepository pollGroupRepo;

    @Autowired
    private PredictionRepository predictionRepo;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ScoringUtil scoringUtil;

    @Autowired
    private ModelMapper modelMapper;

    public PollService() {
    }

    // =========================================================================
    // Gestión de grupos
    // =========================================================================

    /**
     * Crea un nuevo grupo de polla. Genera un código de invitación único de
     * 8 caracteres y registra al creador como primer miembro del grupo.
     *
     * @param name        Nombre del grupo.
     * @param description Descripción del grupo (puede ser null o vacío).
     * @param ownerId     ID del usuario creador (extraído del JWT en el controller).
     * @return El {@link PollGroupDTO} creado con inviteCode incluido,
     *         o {@code null} si el usuario no existe.
     */
    public PollGroupDTO createGroup(String name, String description, Long ownerId) {
        Optional<User> owner = userRepo.findById(ownerId);
        if (owner.isEmpty()) {
            return null;
        }
        String inviteCode = generateUniqueInviteCode();
        PollGroup group = new PollGroup(name, owner.get(), inviteCode);
        // La descripción es opcional. Si llega vacía o solo espacios, se guarda null.
        if (description != null && !description.isBlank()) {
            group.setDescription(description.trim());
        }
        group.getMembers().add(owner.get());
        pollGroupRepo.save(group);
        return toGroupDTO(group);
    }

    /**
     * Une a un usuario a un grupo de polla existente usando el código de invitación.
     *
     * @param userId     ID del usuario que quiere unirse.
     * @param inviteCode El código de invitación del grupo.
     * @return
     *   <ul>
     *     <li>0 — Se unió exitosamente.</li>
     *     <li>1 — Ya era miembro del grupo.</li>
     *     <li>2 — El grupo o el usuario no existen.</li>
     *     <li>5 — El usuario es el CREADOR del grupo (no necesita unirse).</li>
     *   </ul>
     */
    public int joinGroup(Long userId, String inviteCode) {
        Optional<PollGroup> group = pollGroupRepo.findByInviteCode(inviteCode);
        Optional<User> user = userRepo.findById(userId);

        if (group.isEmpty() || user.isEmpty()) {
            return 2;
        }
        PollGroup pg = group.get();

        // Caso borde: el creador intenta unirse a su propio grupo.
        // Es miembro de facto, así que devolvemos código 5 para que el front
        // muestre "Ya eres el creador de este grupo".
        if (pg.getOwner() != null && pg.getOwner().getId().equals(userId)) {
            return 5;
        }

        if (pollGroupRepo.isMemberOfGroup(pg.getId(), userId)) {
            return 1;
        }
        pg.getMembers().add(user.get());
        pollGroupRepo.save(pg);
        return 0;
    }

    /**
     * Obtiene todos los grupos activos en los que participa un usuario.
     * <p>
     * El {@code inviteCode} solo se incluye en la respuesta si el usuario es
     * el CREADOR del grupo. Para grupos en los que es solo miembro invitado,
     * se devuelve null para no exponer el código.
     * </p>
     *
     * @param userId El ID del usuario.
     * @return Lista de {@link PollGroupDTO} con el inviteCode condicionado al rol.
     */
    public List<PollGroupDTO> getGroupsByUser(Long userId) {
        List<PollGroup> groups = pollGroupRepo.findActiveGroupsByMemberId(userId);
        List<PollGroupDTO> dtoList = new ArrayList<>();
        groups.forEach(g -> {
            PollGroupDTO dto = toGroupDTO(g);
            // Ocultar el código si no es el creador.
            if (g.getOwner() == null || !g.getOwner().getId().equals(userId)) {
                dto.setInviteCode(null);
            }
            dtoList.add(dto);
        });
        return dtoList;
    }

    /**
     * Obtiene el detalle de un grupo por su ID. No filtra el inviteCode aquí
     * — esa decisión la toma el controller comparando con el usuario autenticado.
     *
     * @param groupId El ID del grupo.
     * @return El {@link PollGroupDTO}, o {@code null} si no existe.
     */
    public PollGroupDTO getGroupById(Long groupId) {
        Optional<PollGroup> found = pollGroupRepo.findById(groupId);
        return found.map(this::toGroupDTO).orElse(null);
    }

    /**
     * Desactiva un grupo de polla (soft delete). Solo el creador puede hacerlo.
     * El grupo conserva el historial de pronósticos pero no acepta nuevos.
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

    /**
     * Elimina COMPLETAMENTE un grupo de polla y sus pronósticos asociados
     * (hard delete). Solo el creador puede hacerlo.
     * <p>
     * Borra primero todas las predicciones asociadas al grupo (de todos sus
     * miembros) para no violar la integridad referencial, después limpia la
     * tabla de unión {@code poll_group_members} y finalmente elimina el grupo.
     * </p>
     *
     * @param groupId El ID del grupo a eliminar.
     * @param ownerId El ID del usuario que solicita la eliminación.
     * @return 0 si fue eliminado; 2 si no existe; 4 si no es el creador.
     */
    public int deleteGroup(Long groupId, Long ownerId) {
        Optional<PollGroup> found = pollGroupRepo.findById(groupId);
        if (found.isEmpty()) {
            return 2;
        }
        PollGroup group = found.get();
        if (group.getOwner() == null || !group.getOwner().getId().equals(ownerId)) {
            return 4;
        }

        // Borrar todas las predicciones asociadas a este grupo.
        // Iteramos sobre los miembros para limpiar sus pronósticos individuales.
        for (User member : group.getMembers()) {
            List<Prediction> userPredictions =
                    predictionRepo.findByUserIdAndPollGroupId(member.getId(), groupId);
            predictionRepo.deleteAll(userPredictions);
        }

        // Vaciar la lista de miembros (tabla de unión) antes de eliminar.
        group.getMembers().clear();
        pollGroupRepo.save(group);

        // Eliminar el grupo definitivamente.
        pollGroupRepo.delete(group);
        return 0;
    }

    // =========================================================================
    // Gestión de pronósticos
    // =========================================================================

    /**
     * Registra un pronóstico de un usuario para un partido dentro de un grupo.
     * <p>
     * Reglas de negocio:
     * <ul>
     *   <li>El partido debe estar en estado {@code SCHEDULED}.</li>
     *   <li>El usuario debe ser miembro del grupo de polla.</li>
     *   <li>NO debe existir ya un pronóstico previo del mismo usuario para
     *       ese partido en ese grupo (la UK de la tabla lo refuerza).</li>
     *   <li>Una vez creado, el pronóstico NO es modificable (regla del proyecto).</li>
     * </ul>
     * </p>
     *
     * @param data   El DTO con matchId, pollGroupId y los marcadores predichos.
     *               El {@code userId} del DTO se IGNORA — se usa el que llega
     *               por parámetro (del JWT).
     * @param userId ID del usuario autenticado (extraído del JWT por el controller).
     * @return 0 si fue registrado; 1 si ya existe; 2 si partido/grupo/usuario
     *         no existen; 4 si el partido ya no acepta pronósticos.
     */
    public int submitPrediction(PredictionDTO data, Long userId) {
        Optional<Match> match = matchRepo.findById(data.getMatchId());
        Optional<PollGroup> group = pollGroupRepo.findById(data.getPollGroupId());
        Optional<User> user = userRepo.findById(userId);

        if (match.isEmpty() || group.isEmpty() || user.isEmpty()) {
            return 2;
        }
        if (match.get().getStatus() != MatchStatus.SCHEDULED) {
            return 4;
        }
        // Verificar que el usuario sea miembro del grupo.
        if (!pollGroupRepo.isMemberOfGroup(group.get().getId(), userId)) {
            return 4;
        }
        Optional<Prediction> existing = predictionRepo.findByUserIdAndMatchIdAndPollGroupId(
                userId, data.getMatchId(), data.getPollGroupId());
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
     * Edita un pronóstico existente.
     * <p>
     * <b>Esta operación NO es permitida</b> por regla de negocio del proyecto:
     * los pronósticos son inmutables una vez creados. Se mantiene el método
     * en el servicio por completitud de la API, pero siempre devuelve 4.
     * </p>
     *
     * @param predictionId El ID del pronóstico.
     * @param userId       El ID del usuario.
     * @param newHomeScore Ignorado.
     * @param newAwayScore Ignorado.
     * @return Siempre 4 (operación no permitida).
     */
    public int editPrediction(Long predictionId, Long userId,
                               Integer newHomeScore, Integer newAwayScore) {
        // Las predicciones no son modificables. Siempre se rechaza la operación.
        return 4;
    }

    public void lockPredictionsForMatch(Long matchId) {
        List<Prediction> openPredictions = predictionRepo.findByMatchIdAndStatus(matchId, PredictionStatus.OPEN);
        openPredictions.forEach(p -> {
            p.setStatus(PredictionStatus.LOCKED);
            predictionRepo.save(p);
        });
    }

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

    public List<PredictionDTO> getPredictionsByUser(Long userId) {
        List<Prediction> predictions = predictionRepo.findByUserId(userId);
        List<PredictionDTO> dtoList = new ArrayList<>();
        predictions.forEach(p -> dtoList.add(toPredictionDTO(p)));
        return dtoList;
    }

    public List<PredictionDTO> getPredictionsByUserAndGroup(Long userId, Long pollGroupId) {
        List<Prediction> predictions = predictionRepo.findByUserIdAndPollGroupId(userId, pollGroupId);
        List<PredictionDTO> dtoList = new ArrayList<>();
        predictions.forEach(p -> dtoList.add(toPredictionDTO(p)));
        return dtoList;
    }

    // =========================================================================
    // Ranking
    // =========================================================================

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
            entry.setCorrectResults(totalPoints != null
                    ? totalPoints - (exactScores != null ? exactScores * 2 : 0)
                    : 0);
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

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (pollGroupRepo.existsByInviteCode(code));
        return code;
    }

    private PollGroupDTO toGroupDTO(PollGroup group) {
        PollGroupDTO dto = new PollGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setInviteCode(group.getInviteCode());
        if (group.getOwner() != null) {
            dto.setOwnerId(group.getOwner().getId());
            dto.setOwnerUsername(group.getOwner().getUsername());
        }
        dto.setMembersCount(group.getMembers().size());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setActive(group.isActive());
        return dto;
    }

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