/**
 * Paquete que contiene las clases para el manejo de excepciones y validaciones
 * específicas de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.exception;

import co.edu.unbosque.mundial2026.model.User;

/**
 * Clase utilitaria que contiene métodos de validación de datos de entrada
 * para la aplicación Mundial 2026 Hub.
 * <p>
 * Sigue el mismo patrón del proyecto VirusDetected. Provee validaciones de
 * contraseñas, correos electrónicos, caracteres HTML, roles y datos específicos
 * del dominio del torneo (códigos ISO de equipos, categorías de entradas,
 * marcadores de pronósticos). Se instancia con {@code new Exceptions()} o
 * {@code new Exceptions(user)} en los servicios que requieren validaciones.
 * </p>
 */
public class Exceptions {

    /**
     * El usuario asociado a esta instancia de validaciones.
     * Puede ser nulo cuando la validación no requiere contexto de usuario.
     */
    private User user;

    /**
     * Constructor por defecto de {@code Exceptions}.
     * Para validaciones que no requieren contexto de usuario.
     */
    public Exceptions() {
    }

    /**
     * Constructor con el usuario cuya información se va a validar.
     *
     * @param user El usuario para el cual se realizarán las validaciones.
     */
    public Exceptions(User user) {
        this.user = user;
    }

    // =========================================================================
    // Validaciones de usuario
    // =========================================================================

    /**
     * Verifica si una contraseña cumple la política de seguridad del sistema.
     * <p>
     * Criterios requeridos:
     * <ul>
     *   <li>Mínimo 8 caracteres de longitud.</li>
     *   <li>Al menos una letra minúscula.</li>
     *   <li>Al menos una letra mayúscula.</li>
     *   <li>Al menos un dígito.</li>
     *   <li>Al menos un símbolo especial que no sea {@code < > & " ' / = } ni espacio.</li>
     * </ul>
     * </p>
     *
     * @param password La contraseña a validar.
     * @return {@code true} si la contraseña cumple todos los criterios;
     *         {@code false} si es nula, tiene menos de 8 caracteres o no cumple
     *         alguno de los requisitos.
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasValidSymbol = false;

        String disallowedSymbols = "<>&\"'/= ";

        for (char ch : password.toCharArray()) {
            if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (disallowedSymbols.indexOf(ch) != -1) {
                return false;
            } else {
                hasValidSymbol = true;
            }
        }

        return hasLower && hasUpper && hasDigit && hasValidSymbol;
    }

    /**
     * Valida si una dirección de correo electrónico tiene un formato válido.
     * Acepta cualquier dominio (no solo Gmail), con el formato estándar:
     * {@code usuario@dominio.extension}.
     *
     * @param emailToCheck La dirección de correo a validar.
     * @return {@code true} si el formato es válido; {@code false} si es nula,
     *         vacía o no coincide con el patrón de email estándar.
     */
    public boolean isEmailValid(String emailToCheck) {
        if (emailToCheck == null || emailToCheck.trim().isEmpty()) {
            return false;
        }
        return emailToCheck.matches(
                "(?i)^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Verifica si una cadena de texto contiene caracteres HTML que podrían
     * usarse para inyección de código. Se bloquean: {@code < > & " ' / =}.
     *
     * @param text La cadena de texto a verificar.
     * @return {@code true} si la cadena contiene algún carácter HTML peligroso;
     *         {@code false} si es nula, vacía o no contiene ninguno.
     */
    public boolean containsHtmlSymbols(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return text.matches(".*[<>&\"'/=].*");
    }

    /**
     * Verifica si el rol dado corresponde a uno de los roles válidos del sistema
     * ({@code USER} o {@code ADMIN}).
     * <p>
     * Retorna {@code false} si el rol es válido (no hay error), y {@code true}
     * si el rol es inválido (hay error). Este comportamiento sigue la convención
     * del proyecto VirusDetected.
     * </p>
     *
     * @param rol La cadena que representa el rol a verificar.
     * @return {@code false} si el rol es "USER" o "ADMIN" (válido);
     *         {@code true} si el rol no es reconocido (inválido).
     */
    public boolean containsRole(String rol) {
        if (rol == null) return true;
        return !rol.equals("USER") && !rol.equals("ADMIN");
    }

    // =========================================================================
    // Validaciones del dominio de torneo
    // =========================================================================

    /**
     * Valida si un código ISO de equipo tiene el formato correcto.
     * Los códigos ISO válidos son cadenas de 2 a 3 letras mayúsculas
     * (ej. "COL", "BRA", "ARG", "USA").
     *
     * @param isoCode El código ISO del equipo a validar.
     * @return {@code true} si el código tiene entre 2 y 3 letras mayúsculas;
     *         {@code false} en cualquier otro caso.
     */
    public boolean isValidTeamIsoCode(String isoCode) {
        if (isoCode == null || isoCode.trim().isEmpty()) {
            return false;
        }
        return isoCode.matches("^[A-Z]{2,3}$");
    }

    /**
     * Valida si un marcador de pronóstico es razonable.
     * Un marcador válido es un número entero entre 0 y 20 inclusive.
     * Se bloquean valores negativos y valores excesivamente altos que
     * sugieran un error de entrada.
     *
     * @param score El marcador a validar.
     * @return {@code true} si el marcador está entre 0 y 20 inclusive;
     *         {@code false} si es nulo, negativo o mayor a 20.
     */
    public boolean isValidScore(Integer score) {
        return score != null && score >= 0 && score <= 20;
    }

    /**
     * Valida si el nombre de un grupo de polla o equipo es apropiado.
     * Verifica que no esté vacío, no supere 100 caracteres y no contenga
     * caracteres HTML peligrosos.
     *
     * @param name El nombre a validar.
     * @return {@code true} si el nombre es válido; {@code false} si es nulo,
     *         vacío, supera 100 caracteres o contiene HTML.
     */
    public boolean isValidGroupName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (name.length() > 100) {
            return false;
        }
        return !containsHtmlSymbols(name);
    }

    /**
     * Valida que un código de invitación de polla tenga el formato correcto.
     * Los códigos son cadenas de exactamente 8 caracteres alfanuméricos
     * en mayúsculas (generados por {@code PollService}).
     *
     * @param inviteCode El código de invitación a validar.
     * @return {@code true} si el código tiene exactamente 8 caracteres
     *         alfanuméricos en mayúsculas; {@code false} en cualquier otro caso.
     */
    public boolean isValidInviteCode(String inviteCode) {
        if (inviteCode == null) return false;
        return inviteCode.matches("^[A-Z0-9]{8}$");
    }

    /**
     * Valida que un código promocional de álbum tenga el formato correcto.
     * Los códigos de promo son cadenas alfanuméricas con guiones de entre
     * 6 y 20 caracteres (ej. "MUNDIAL2026", "PROMO-XXXXX").
     *
     * @param promoCode El código promocional a validar.
     * @return {@code true} si el código tiene entre 6 y 20 caracteres
     *         alfanuméricos con guiones; {@code false} en otro caso.
     */
    public boolean isValidPromoCode(String promoCode) {
        if (promoCode == null || promoCode.trim().isEmpty()) return false;
        return promoCode.matches("^[A-Z0-9\\-]{6,20}$");
    }

    /**
     * Valida que una zona horaria IANA sea reconocida por la JVM.
     * Se usa para validar el campo {@code timezone} de los estadios antes de
     * persistir o para verificar la zona del usuario.
     *
     * @param timezoneId La cadena de la zona horaria IANA a validar.
     * @return {@code true} si la zona horaria es reconocida; {@code false} en
     *         caso contrario.
     */
    public boolean isValidTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.trim().isEmpty()) return false;
        try {
            java.time.ZoneId.of(timezoneId);
            return true;
        } catch (java.time.DateTimeException e) {
            return false;
        }
    }

    // =========================================================================
    // Getter y Setter del usuario asociado
    // =========================================================================

    /**
     * Retorna el usuario asociado a esta instancia de validaciones.
     *
     * @return El usuario asociado, o {@code null} si no se asignó ninguno.
     */
    public User getUser() {
        return user;
    }

    /**
     * Asigna el usuario asociado a esta instancia de validaciones.
     *
     * @param user El nuevo usuario a asociar.
     */
    public void setUser(User user) {
        this.user = user;
    }
}