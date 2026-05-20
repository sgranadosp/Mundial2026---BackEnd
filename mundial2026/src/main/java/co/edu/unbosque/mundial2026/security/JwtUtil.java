/**
 * Paquete que contiene las clases relacionadas con la seguridad de la
 * aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utilidad para la generación, extracción y validación de tokens JWT (JSON Web
 * Tokens) en la plataforma Mundial 2026 Hub.
 * <p>
 * Sigue el mismo patrón del proyecto VirusDetected. Usa JJWT con HMAC-SHA256
 * para firmar los tokens. La clave secreta se inyecta desde
 * {@code application.properties} como {@code jwt.secret}; si no se configura,
 * se usa un valor por defecto que debe reemplazarse en producción.
 * </p>
 * <p>
 * El token incluye dos claims adicionales: las autoridades del usuario (para
 * autorización sin consultar la BD en cada petición) y el username encriptado
 * con AES (para trazabilidad en auditoría).
 * </p>
 */
@Component
public class JwtUtil {

    /**
     * Tiempo de validez del token JWT en milisegundos.
     * Actualmente configurado a 8 horas para cubrir una jornada de partidos.
     */
    private static final long JWT_TOKEN_VALIDITY = 8 * 60 * 60 * 1000L;

    /**
     * Clave secreta para firmar los tokens JWT. Se obtiene de la propiedad
     * {@code jwt.secret} en {@code application.properties}.
     * Debe tener al menos 32 caracteres para HMAC-SHA256.
     * Reemplazar el valor por defecto en producción con un secreto generado
     * aleatoriamente y almacenado en una variable de entorno.
     */
    @Value("${jwt.secret:mundial2026HubSecretKeyForJWTSigningMustBe32Chars}")
    private String secret;

    // =========================================================================
    // Generación de tokens
    // =========================================================================

    /**
     * Genera un token JWT para el usuario autenticado. Incluye como claims
     * adicionales las autoridades del usuario y su username encriptado con AES.
     *
     * @param userDetails           Los detalles del usuario autenticado.
     * @param usernameEncriptado    El username encriptado con AES para incluir
     *                              en el claim de trazabilidad.
     * @return El token JWT firmado y codificado.
     */
    public String generateToken(UserDetails userDetails, String usernameEncriptado) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities());
        claims.put("user", usernameEncriptado);
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Construye y firma el token JWT con las claims, el subject y las fechas
     * de emisión y expiración.
     *
     * @param claims  Las claims adicionales a incluir en el payload del token.
     * @param subject El subject del token (username encriptado del usuario).
     * @return El token JWT compacto y firmado.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================================
    // Extracción de claims
    // =========================================================================

    /**
     * Extrae el username (subject) del token JWT.
     * En esta aplicación el subject es el username encriptado con AES.
     *
     * @param token El token JWT.
     * @return El username extraído del subject del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración del token JWT.
     *
     * @param token El token JWT.
     * @return La fecha de expiración del token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae una claim específica del token JWT usando un resolvedor funcional.
     *
     * @param <T>            El tipo de la claim a extraer.
     * @param token          El token JWT.
     * @param claimsResolver Función que extrae la claim deseada del objeto
     *                       {@link Claims}.
     * @return El valor de la claim extraída.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea el token JWT y retorna todas sus claims.
     *
     * @param token El token JWT a parsear.
     * @return El objeto {@link Claims} con todas las claims del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // =========================================================================
    // Validación del token
    // =========================================================================

    /**
     * Valida que el token JWT sea válido para el usuario indicado.
     * Verifica que el subject del token coincida con el username del usuario
     * y que el token no haya expirado.
     *
     * @param token       El token JWT a validar.
     * @param userDetails Los detalles del usuario contra el que se valida.
     * @return {@code true} si el token es válido y no ha expirado para el usuario.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Verifica si el token JWT ya expiró comparando su fecha de expiración
     * con la fecha y hora actual.
     *
     * @param token El token JWT a verificar.
     * @return {@code true} si el token ha expirado.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // =========================================================================
    // Clave de firma
    // =========================================================================

    /**
     * Genera la clave HMAC-SHA para firmar y verificar los tokens.
     * Se construye a partir de los bytes de la cadena {@code secret}.
     *
     * @return La clave {@link Key} para operaciones de firma y verificación.
     */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}