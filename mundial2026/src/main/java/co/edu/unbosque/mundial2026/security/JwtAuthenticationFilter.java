/**
 * Paquete que contiene las clases relacionadas con la seguridad de la
 * aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de autenticación JWT que intercepta cada petición HTTP entrante
 * en la plataforma Mundial 2026 Hub.
 * <p>
 * Extiende {@link OncePerRequestFilter} para garantizar que se ejecuta
 * exactamente una vez por petición. El flujo es:
 * <ol>
 *   <li>Extrae el token JWT del header {@code Authorization: Bearer <token>}.</li>
 *   <li>Extrae el username del subject del token.</li>
 *   <li>Si el username es válido y no hay autenticación activa, carga el
 *       {@link UserDetails} desde la base de datos.</li>
 *   <li>Valida el token contra el usuario cargado.</li>
 *   <li>Si todo es válido, establece la autenticación en el
 *       {@link SecurityContextHolder} para que Spring Security la use
 *       en la autorización del endpoint.</li>
 * </ol>
 * </p>
 * <p>
 * <b>Comportamiento ante tokens inválidos:</b> si el JWT está malformado,
 * expirado, firmado con otra clave o apunta a un usuario inexistente, el
 * filtro registra un WARN y deja seguir la petición SIN autenticación. Spring
 * Security responderá 401 si el endpoint requiere autenticación, o procesará
 * la petición normalmente si el endpoint es público (caso típico:
 * {@code /auth/login} cuando el navegador conserva un JWT obsoleto).
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Utilidad para extracción y validación de tokens JWT.
     */
    private final JwtUtil jwtUtil;

    /**
     * Servicio para cargar los detalles del usuario desde la base de datos.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Constructor con inyección de dependencias para JWT y UserDetails.
     *
     * @param jwtUtil            Utilidad para manejo de tokens JWT.
     * @param userDetailsService Servicio para cargar usuarios desde la BD.
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Lógica principal del filtro. Se ejecuta una vez por cada petición HTTP.
     * <p>
     * Extrae el JWT del header Authorization, valida su firma e integridad,
     * y si es válido establece la autenticación en el contexto de seguridad.
     * Si el token es inválido o el usuario ya no existe, la petición continúa
     * sin autenticación; Spring Security rechazará después solo si el endpoint
     * requiere autenticación.
     * </p>
     *
     * @param request     La petición HTTP entrante.
     * @param response    La respuesta HTTP saliente.
     * @param filterChain La cadena de filtros para continuar el procesamiento.
     * @throws ServletException Si ocurre un error de servlet.
     * @throws IOException      Si ocurre un error de entrada/salida.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Extrae el token del header Authorization si tiene el prefijo "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // Token mal formado, expirado o firmado con otra clave.
                // No es razón para tirar la petición; solo seguimos sin auth.
                logger.warn("Token JWT inválido o ilegible: " + e.getMessage());
            }
        }

        // Si hay username y no hay autenticación activa en el contexto de seguridad
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (UsernameNotFoundException e) {
                // El JWT apunta a un usuario que ya no existe (BD limpia, refactor,
                // cuenta eliminada). Se ignora el token y se sigue sin autenticación.
                logger.warn("JWT con username inexistente: " + username
                        + " — se ignora el token y se continúa sin autenticación.");
            } catch (Exception e) {
                // Cualquier otro error inesperado al validar: log y continuar.
                logger.warn("Error inesperado validando JWT: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}