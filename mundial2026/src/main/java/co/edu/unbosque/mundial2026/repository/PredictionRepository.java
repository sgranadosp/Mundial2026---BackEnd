/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.mundial2026.model.Prediction;
import co.edu.unbosque.mundial2026.model.Prediction.PredictionStatus;

/**
 * Interfaz de repositorio para la entidad {@link Prediction}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar sobre
 * la tabla {@code predictions}. Los métodos personalizados cubren los flujos
 * clave del módulo de pollas: registrar y editar pronósticos, bloquearlos
 * al iniciar el partido, consultarlos por usuario y grupo, y calcular
 * los puntajes cuando el partido finaliza.
 */
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    /**
     * Busca el pronóstico único de un usuario para un partido dentro de un grupo.
     * La restricción de unicidad en la entidad garantiza como máximo un resultado.
     * Se usa para verificar si ya existe un pronóstico antes de crear uno nuevo
     * y para recuperarlo al editarlo (HU12 — Editar predicción).
     *
     * @param userId      El ID del usuario.
     * @param matchId     El ID del partido.
     * @param pollGroupId El ID del grupo de polla.
     * @return Un {@link Optional} con el pronóstico encontrado, o vacío si no existe.
     */
    Optional<Prediction> findByUserIdAndMatchIdAndPollGroupId(Long userId, Long matchId, Long pollGroupId);

    /**
     * Obtiene todos los pronósticos realizados por un usuario en todos sus grupos.
     * Se usa en el historial de predicciones del usuario (HU14 — Ver historial).
     *
     * @param userId El ID del usuario.
     * @return Lista de pronósticos del usuario, en todos sus grupos.
     */
    List<Prediction> findByUserId(Long userId);

    /**
     * Obtiene todos los pronósticos de un usuario dentro de un grupo específico.
     * Se usa para mostrar el desempeño del usuario dentro de esa polla.
     *
     * @param userId      El ID del usuario.
     * @param pollGroupId El ID del grupo de polla.
     * @return Lista de pronósticos del usuario en el grupo indicado.
     */
    List<Prediction> findByUserIdAndPollGroupId(Long userId, Long pollGroupId);

    /**
     * Obtiene todos los pronósticos registrados para un partido específico,
     * de todos los usuarios y todos los grupos. Se usa cuando el partido
     * finaliza para calcular los puntajes de todos los participantes.
     *
     * @param matchId El ID del partido.
     * @return Lista de pronósticos asociados al partido.
     */
    List<Prediction> findByMatchId(Long matchId);

    /**
     * Obtiene todos los pronósticos de un partido dentro de un grupo específico.
     * Se usa para calcular el ranking interno del grupo después de cada partido.
     *
     * @param matchId     El ID del partido.
     * @param pollGroupId El ID del grupo de polla.
     * @return Lista de pronósticos del partido en el grupo.
     */
    List<Prediction> findByMatchIdAndPollGroupId(Long matchId, Long pollGroupId);

    /**
     * Obtiene todos los pronósticos con un estado específico del ciclo de vida.
     * El uso principal es encontrar pronósticos con estado {@code OPEN} para
     * bloquearlos masivamente cuando un partido cambia a estado {@code LIVE}.
     *
     * @param status El estado del pronóstico a filtrar.
     * @return Lista de pronósticos con el estado indicado.
     */
    List<Prediction> findByStatus(PredictionStatus status);

    /**
     * Obtiene todos los pronósticos de un partido con un estado específico.
     * Se usa para bloquear ({@code OPEN → LOCKED}) todos los pronósticos
     * de un partido al iniciarse, o para evaluar ({@code LOCKED → EVALUATED})
     * al finalizar.
     *
     * @param matchId El ID del partido.
     * @param status  El estado del pronóstico.
     * @return Lista de pronósticos del partido con el estado indicado.
     */
    List<Prediction> findByMatchIdAndStatus(Long matchId, PredictionStatus status);

    /**
     * Calcula la suma total de puntos obtenidos por un usuario en un grupo de polla.
     * Se usa para construir el ranking del grupo (HU21, HU23 — Ver ranking).
     * Devuelve 0 si el usuario no tiene pronósticos evaluados en el grupo.
     *
     * @param userId      El ID del usuario.
     * @param pollGroupId El ID del grupo de polla.
     * @return La suma de puntos del usuario en el grupo, o 0 si no tiene.
     */
    @Query("SELECT COALESCE(SUM(p.pointsEarned), 0) FROM Prediction p " +
           "WHERE p.user.id = :userId AND p.pollGroup.id = :pollGroupId " +
           "AND p.status = 'EVALUATED'")
    Integer sumPointsByUserIdAndPollGroupId(@Param("userId") Long userId,
                                            @Param("pollGroupId") Long pollGroupId);

    /**
     * Cuenta el número de pronósticos evaluados que resultaron en marcador exacto
     * para un usuario en un grupo. Se usa para las estadísticas detalladas del
     * ranking (HU22 — Ver estadísticas personales).
     *
     * @param userId      El ID del usuario.
     * @param pollGroupId El ID del grupo de polla.
     * @return Número de marcadores exactos acertados.
     */
    @Query("SELECT COUNT(p) FROM Prediction p " +
           "JOIN p.match m " +
           "WHERE p.user.id = :userId AND p.pollGroup.id = :pollGroupId " +
           "AND p.status = 'EVALUATED' " +
           "AND p.predictedHomeScore = m.homeScore " +
           "AND p.predictedAwayScore = m.awayScore")
    Integer countExactScoresByUserIdAndPollGroupId(@Param("userId") Long userId,
                                                   @Param("pollGroupId") Long pollGroupId);
}