/**
 * Paquete que contiene las clases de utilidad de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.util;

import org.springframework.stereotype.Component;

/**
 * Utilidad de cálculo de puntajes para el módulo de pollas futboleras
 * de la plataforma Mundial 2026 Hub.
 * <p>
 * Encapsula las reglas de puntuación de forma centralizada y reutilizable,
 * de modo que cualquier cambio en la tabla de puntos afecte a todo el sistema
 * desde un solo lugar. El servicio {@link co.edu.unbosque.mundial2026.service.PollService}
 * delega el cálculo a esta clase al evaluar los pronósticos de cada partido.
 *
 * <h3>Tabla de puntuación</h3>
 * <table border="1">
 *   <tr><th>Resultado</th><th>Puntos</th></tr>
 *   <tr><td>Marcador exacto (local y visitante correctos)</td><td>3</td></tr>
 *   <tr><td>Resultado correcto (ganador o empate acertado, marcador no exacto)</td><td>1</td></tr>
 *   <tr><td>Sin acierto</td><td>0</td></tr>
 * </table>
 *
 * <h3>Determinación del resultado</h3>
 * <p>Un resultado se considera correcto cuando:</p>
 * <ul>
 *   <li>El equipo local ganó en la predicción y ganó en el partido real.</li>
 *   <li>El equipo visitante ganó en la predicción y ganó en el partido real.</li>
 *   <li>Se predijo empate y el partido terminó en empate.</li>
 * </ul>
 */
@Component
public class ScoringUtil {

    /**
     * Puntos otorgados por acertar el marcador exacto (local y visitante correctos).
     */
    public static final int POINTS_EXACT_SCORE = 3;

    /**
     * Puntos otorgados por acertar el resultado (ganador o empate) sin marcador exacto.
     */
    public static final int POINTS_CORRECT_RESULT = 1;

    /**
     * Puntos otorgados cuando no se acertó ni el resultado ni el marcador.
     */
    public static final int POINTS_NO_MATCH = 0;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public ScoringUtil() {
    }

    // =========================================================================
    // Cálculo de puntos
    // =========================================================================

    /**
     * Calcula los puntos obtenidos por un pronóstico comparándolo con el
     * resultado real del partido.
     * <p>
     * La lógica aplica primero la regla de mayor valor (marcador exacto) y,
     * si no se cumple, verifica la de menor valor (resultado correcto).
     * </p>
     *
     * @param predictedHome Goles predichos para el equipo local.
     * @param predictedAway Goles predichos para el equipo visitante.
     * @param actualHome    Goles reales del equipo local al finalizar el partido.
     * @param actualAway    Goles reales del equipo visitante al finalizar el partido.
     * @return {@value #POINTS_EXACT_SCORE} si el marcador es exacto;
     *         {@value #POINTS_CORRECT_RESULT} si solo el resultado es correcto;
     *         {@value #POINTS_NO_MATCH} si no hubo ningún acierto.
     *         Retorna {@value #POINTS_NO_MATCH} si algún valor es {@code null}.
     */
    public int calculatePoints(Integer predictedHome, Integer predictedAway,
                                int actualHome, int actualAway) {
        if (predictedHome == null || predictedAway == null) {
            return POINTS_NO_MATCH;
        }

        if (isExactScore(predictedHome, predictedAway, actualHome, actualAway)) {
            return POINTS_EXACT_SCORE;
        }

        if (isCorrectResult(predictedHome, predictedAway, actualHome, actualAway)) {
            return POINTS_CORRECT_RESULT;
        }

        return POINTS_NO_MATCH;
    }

    /**
     * Verifica si el pronóstico acertó el marcador exacto.
     * Ambos valores (local y visitante) deben coincidir con el resultado real.
     *
     * @param predictedHome Goles predichos para el equipo local.
     * @param predictedAway Goles predichos para el equipo visitante.
     * @param actualHome    Goles reales del equipo local.
     * @param actualAway    Goles reales del equipo visitante.
     * @return {@code true} si el marcador predicho es exactamente igual al real.
     */
    public boolean isExactScore(int predictedHome, int predictedAway,
                                 int actualHome, int actualAway) {
        return predictedHome == actualHome && predictedAway == actualAway;
    }

    /**
     * Verifica si el pronóstico acertó el resultado (ganador o empate)
     * independientemente del marcador exacto.
     * <p>
     * Tres casos posibles de acierto de resultado:
     * <ul>
     *   <li>El equipo local gana según la predicción y también ganó en el partido.</li>
     *   <li>El equipo visitante gana según la predicción y también ganó en el partido.</li>
     *   <li>Se predijo empate y el partido terminó en empate.</li>
     * </ul>
     * </p>
     *
     * @param predictedHome Goles predichos para el equipo local.
     * @param predictedAway Goles predichos para el equipo visitante.
     * @param actualHome    Goles reales del equipo local.
     * @param actualAway    Goles reales del equipo visitante.
     * @return {@code true} si el resultado predicho coincide con el resultado real.
     */
    public boolean isCorrectResult(int predictedHome, int predictedAway,
                                    int actualHome, int actualAway) {
        int predictedOutcome = Integer.compare(predictedHome, predictedAway);
        int actualOutcome = Integer.compare(actualHome, actualAway);
        return predictedOutcome == actualOutcome;
    }

    /**
     * Determina el resultado de un partido codificado como entero.
     * Útil para mostrar en la interfaz el resultado de cada pronóstico.
     *
     * @param homeScore Goles del equipo local.
     * @param awayScore Goles del equipo visitante.
     * @return 1 si gana el equipo local; -1 si gana el visitante; 0 en empate.
     */
    public int getMatchOutcome(int homeScore, int awayScore) {
        return Integer.compare(homeScore, awayScore);
    }

    /**
     * Calcula el porcentaje de acierto de un usuario a partir de sus estadísticas.
     * Se usa en las estadísticas personales del perfil (HU22).
     *
     * @param totalPredictions Total de pronósticos realizados.
     * @param correctResults   Número de resultados acertados (exactos + solo resultado).
     * @return El porcentaje de acierto entre 0.0 y 100.0, o 0.0 si no hay pronósticos.
     */
    public double calculateAccuracyPercentage(int totalPredictions, int correctResults) {
        if (totalPredictions == 0) return 0.0;
        return Math.round((correctResults * 100.0 / totalPredictions) * 10.0) / 10.0;
    }
}