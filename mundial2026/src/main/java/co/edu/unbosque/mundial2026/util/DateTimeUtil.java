/**
 * Paquete que contiene las clases de utilidad de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;

/**
 * Utilidad de manejo de fechas, horas y zonas horarias para la plataforma
 * Mundial 2026 Hub.
 * El Mundial 2026 se disputa en tres países con múltiples zonas horarias:
 * EE. UU. (Eastern, Central, Mountain, Pacific), Canadá (Eastern, Pacific)
 * y México (Central, Pacific). Todos los horarios se almacenan en UTC en la
 * base de datos y se convierten a la hora local del estadio o del usuario
 * en el momento de mostrarlos.
 * Esta clase provee métodos para convertir entre UTC y cualquier zona horaria
 * IANA, formatear fechas para la interfaz y calcular si un partido está
 * próximo a iniciarse (usado por el módulo de notificaciones y el bloqueo
 * automático de pronósticos).
 */
@Component
public class DateTimeUtil {

    /**
     * Formato estándar para mostrar fechas con hora en la interfaz de usuario.
     * Ejemplo: "15 Jun 2026 - 20:00"
     */
    public static final String DISPLAY_FORMAT = "dd MMM yyyy - HH:mm";

    /**
     * Formato ISO 8601 usado en los DTOs de la API REST.
     * Ejemplo: "2026-06-15T20:00:00"
     */
    public static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Zona horaria UTC utilizada internamente para todas las persistencias.
     */
    public static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Constructor por defecto requerido por Spring.
     */
    public DateTimeUtil() {
    }

    // =========================================================================
    // Conversión de zonas horarias
    // =========================================================================

    /**
     * Convierte una fecha y hora en UTC a la zona horaria local de un estadio.
     * Se usa al mostrar el horario del partido en la agenda del usuario.
     *
     * @param utcDateTime    La fecha y hora del partido almacenada en UTC.
     * @param stadiumTimezone La zona horaria IANA del estadio (ej. "America/New_York").
     * @return La fecha y hora equivalente en la zona horaria del estadio.
     */
    public ZonedDateTime toStadiumLocalTime(LocalDateTime utcDateTime, String stadiumTimezone) {
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC);
        return utcZoned.withZoneSameInstant(ZoneId.of(stadiumTimezone));
    }

    /**
     * Convierte una fecha y hora en UTC a la zona horaria local del usuario.
     * Se usa en los recordatorios contextuales del aficionado viajero.
     *
     * @param utcDateTime  La fecha y hora en UTC.
     * @param userTimezone La zona horaria IANA del usuario (ej. "America/Bogota").
     * @return La fecha y hora equivalente en la zona horaria del usuario.
     */
    public ZonedDateTime toUserLocalTime(LocalDateTime utcDateTime, String userTimezone) {
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC);
        return utcZoned.withZoneSameInstant(ZoneId.of(userTimezone));
    }

    /**
     * Convierte una fecha y hora local de un estadio a UTC.
     * Se usa al recibir datos de la API externa que pueden venir en hora local.
     *
     * @param localDateTime  La fecha y hora en la zona horaria local.
     * @param sourceTimezone La zona horaria IANA de origen.
     * @return La fecha y hora equivalente en UTC.
     */
    public LocalDateTime toUtc(LocalDateTime localDateTime, String sourceTimezone) {
        ZonedDateTime localZoned = localDateTime.atZone(ZoneId.of(sourceTimezone));
        return localZoned.withZoneSameInstant(UTC).toLocalDateTime();
    }

    // =========================================================================
    // Formateo para la interfaz
    // =========================================================================

    /**
     * Formatea una fecha y hora con zona para mostrar en la interfaz de usuario.
     * Ejemplo de salida: "15 Jun 2026 - 20:00 (EDT)"
     *
     * @param zonedDateTime La fecha y hora con zona a formatear.
     * @return La cadena formateada lista para mostrar, incluyendo la abreviatura
     *         de la zona horaria.
     */
    public String formatForDisplay(ZonedDateTime zonedDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DISPLAY_FORMAT + " (z)");
        return zonedDateTime.format(formatter);
    }

    /**
     * Formatea una fecha y hora UTC para incluirla en la respuesta de la API REST
     * en formato ISO 8601.
     *
     * @param utcDateTime La fecha y hora en UTC.
     * @return La cadena en formato ISO 8601 (ej. "2026-06-15T20:00:00").
     */
    public String formatForApi(LocalDateTime utcDateTime) {
        return utcDateTime.format(DateTimeFormatter.ofPattern(ISO_FORMAT));
    }

    /**
     * Parsea una cadena de fecha en formato ISO 8601 a {@link LocalDateTime}.
     * Tolerante con la 'Z' final de UTC (la remueve antes de parsear).
     *
     * @param isoString La cadena de fecha en formato ISO 8601.
     * @return El {@link LocalDateTime} parseado, o {@code null} si el formato
     *         no es reconocido.
     */
    public LocalDateTime parseIsoString(String isoString) {
        if (isoString == null || isoString.isEmpty()) return null;
        String cleaned = isoString.replace("Z", "");
        try {
            return LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern(ISO_FORMAT));
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    // =========================================================================
    // Lógica temporal del torneo
    // =========================================================================

    /**
     * Determina si un partido está próximo a iniciarse dentro de la ventana
     * indicada. Se usa para:
     * <ul>
     *   <li>Enviar recordatorios contextuales antes del partido.</li>
     *   <li>Activar el bloqueo automático de pronósticos de la polla.</li>
     * </ul>
     *
     * @param matchDateTime  La fecha y hora del partido en UTC.
     * @param minutesBefore  Los minutos de anticipación para considerar "próximo".
     *                       Valor típico: 5 minutos antes para bloquear pronósticos,
     *                       30 minutos para notificaciones de recordatorio.
     * @return {@code true} si el partido inicia dentro de los próximos
     *         {@code minutesBefore} minutos desde el momento actual.
     */
    public boolean isMatchStartingSoon(LocalDateTime matchDateTime, long minutesBefore) {
        LocalDateTime now = LocalDateTime.now(UTC);
        LocalDateTime threshold = matchDateTime.minusMinutes(minutesBefore);
        return !now.isBefore(threshold) && now.isBefore(matchDateTime);
    }

    /**
     * Determina si una reserva de entrada ha expirado según su TTL.
     * Equivalente a comparar la fecha de expiración con la hora actual,
     * pero encapsulado para facilitar las pruebas unitarias.
     *
     * @param reservationExpiresAt La fecha y hora de expiración de la reserva en UTC.
     * @return {@code true} si la reserva ya expiró (la hora actual superó la
     *         fecha de expiración).
     */
    public boolean isReservationExpired(LocalDateTime reservationExpiresAt) {
        return LocalDateTime.now(UTC).isAfter(reservationExpiresAt);
    }

    /**
     * Calcula la fecha y hora de expiración de una reserva sumando el TTL
     * desde el momento actual.
     *
     * @param ttlMinutes Los minutos de vida de la reserva (Time To Live).
     * @return La fecha y hora de expiración calculada en UTC.
     */
    public LocalDateTime calculateReservationExpiry(int ttlMinutes) {
        return LocalDateTime.now(UTC).plusMinutes(ttlMinutes);
    }

    /**
     * Verifica si la zona horaria IANA proporcionada es válida y reconocida
     * por la JVM. Se usa para validar el campo {@code Stadium#timezone} antes
     * de persistir.
     *
     * @param timezoneId La cadena de la zona horaria IANA a validar
     *                   (ej. "America/New_York", "America/Bogota").
     * @return {@code true} si la zona horaria es válida.
     */
    public boolean isValidTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.isEmpty()) return false;
        try {
            ZoneId.of(timezoneId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retorna la hora actual en UTC. Método centralizado para facilitar
     * el mocking en pruebas unitarias.
     *
     * @return El {@link LocalDateTime} actual en UTC.
     */
    public LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }
}