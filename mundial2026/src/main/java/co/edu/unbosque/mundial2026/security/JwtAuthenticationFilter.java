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
 * exactamente una vez por petición, sin importar cuántos filtros haya en la
 * cadena. El flujo es:
 * <ol>
 *   <li>Extrae el token JWT del header {@code Authorization: Bearer <token>}.</li>
 *   <li>Extrae el username del subject del token.</li>
 *   <li>Si el username es válido y no hay autenticación activa, carga el
 *       {@link UserDetails} desde la base de datos.</li>
 *   <li>Valida el token contra el usuario cargado.</li>
 *   <li>Si es válido, establece la autenticación en el
 *       {@link SecurityContextHolder} para que Spring Security la use
 *       en la autorización del endpoint.</li>
 * </ol>
 * Sigue el mismo patrón del proyecto VirusDetected.
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
     * Si el token es inválido o está ausente, la petición continúa sin
     * autenticación y Spring Security rechazará el acceso en endpoints
     * protegidos.
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
                logger.error("Error al extraer el username del token JWT: " + e.getMessage());
            }
        }

        // Si hay username y no hay autenticación activa en el contexto de seguridad
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
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
        }

        filterChain.doFilter(request, response);
    }
}