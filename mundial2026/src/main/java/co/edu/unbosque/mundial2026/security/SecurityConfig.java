package co.edu.unbosque.mundial2026.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración central de seguridad para la plataforma Mundial 2026 Hub.
 * Esta clase define la política de seguridad de la aplicación utilizando
 * Spring Security. Se implementa un enfoque de autenticación sin estado
 * (stateless) mediante el uso de tokens JWT, evitando la creación de
 * sesiones HTTP en el servidor.
 *
 * <h3>Características principales</h3>
 * <ul>
 *   <li>Autenticación basada en JWT mediante un filtro personalizado.</li>
 *   <li>Configuración de CORS para permitir solicitudes desde el frontend.</li>
 *   <li>Uso de BCrypt para el cifrado de contraseñas.</li>
 *   <li>Control de acceso basado en roles (USER y ADMIN).</li>
 * </ul>
 *
 * <h3>Estrategia de autorización</h3>
 * <ul>
 *   <li><b>Rutas públicas:</b> accesibles sin autenticación (auth y documentación).</li>
 *   <li><b>Rutas protegidas:</b> requieren autenticación con roles específicos.</li>
 *   <li><b>Administración:</b> restringida exclusivamente a usuarios con rol ADMIN.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Filtro de autenticación JWT encargado de interceptar cada solicitud HTTP
     * y validar el token de autenticación. 
     */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Servicio encargado de cargar los detalles del usuario durante el proceso
     * de autenticación.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param jwtAuthFilter      filtro de autenticación JWT
     * @param userDetailsService servicio de detalles de usuario
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Define la cadena de filtros de seguridad.
     * <p>
     * Se deshabilita CSRF debido a que la aplicación es stateless, se habilita CORS,
     * se configuran las reglas de autorización y se establece la política de sesiones.
     * Las peticiones OPTIONS (preflight de CORS) se permiten explícitamente para
     * que el navegador pueda completar el handshake antes del POST real.
     * </p>
     *
     * @param http objeto de configuración HTTP
     * @return cadena de filtros de seguridad
     * @throws Exception en caso de error de configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> {

                // Preflight CORS — debe permitirse SIEMPRE sin autenticación
                auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();

                // Rutas públicas
                auth.requestMatchers("/auth/**").permitAll();
                auth.requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**").permitAll();
                // Imágenes de láminas del álbum: públicas para que el navegador
                // pueda mostrarlas en <img src=""> sin manejar JWT en cada request.
                // El servidor solo expone los archivos PNG que cualquier usuario
                // ya posee saberlo viene controlado en /album/** que sí requiere JWT.
                auth.requestMatchers("/laminas/**").permitAll();

                // Webhook de MercadoPago: público para que MercadoPago pueda
                // notificarnos sin manejar JWT. La validación de autenticidad
                // se hace por el preferenceId que solo nosotros conocemos.
                auth.requestMatchers("/payments/webhook").permitAll();

                // Resto del módulo de pagos: requiere usuario autenticado.
                auth.requestMatchers("/payments/**").hasAnyRole("USER", "ADMIN");

                // Rutas accesibles por USER y ADMIN
                auth.requestMatchers(
                        "GET:/matches",
                        "GET:/matches/**").hasAnyRole("USER", "ADMIN");

                auth.requestMatchers(
                        "GET:/users/{id}",
                        "PUT:/users/{id}/profile",
                        "PUT:/users/{id}/email",
                        "PUT:/users/{id}/verificationCode",
                        "GET:/users/exists").hasAnyRole("USER", "ADMIN");

                auth.requestMatchers("/polls/**").hasAnyRole("USER", "ADMIN");
                auth.requestMatchers("/album/**").hasAnyRole("USER", "ADMIN");
                auth.requestMatchers("/fcm-token/**").hasAnyRole("USER", "ADMIN");

                auth.requestMatchers(
                        "POST:/tickets/reserve",
                        "PUT:/tickets/*/pay",
                        "PUT:/tickets/*/transfer",
                        "PUT:/tickets/*/refund",
                        "GET:/tickets/user/**",
                        "GET:/tickets/correlation/**").hasAnyRole("USER", "ADMIN");

                auth.requestMatchers("/ranking/**").hasAnyRole("USER", "ADMIN");

                // Catálogos de selecciones y estadios para preferencias del perfil (HU05).
                auth.requestMatchers(
                        "GET:/teams",
                        "GET:/teams/**",
                        "GET:/stadiums",
                        "GET:/stadiums/**").hasAnyRole("USER", "ADMIN");

                // Rutas exclusivas de ADMIN
                auth.requestMatchers(
                        "GET:/users",
                        "PUT:/users/*/block",
                        "PUT:/users/*/unblock",
                        "DELETE:/users/**",
                        "PUT:/users/*/role",
                        "GET:/users/count").hasRole("ADMIN");

                auth.requestMatchers(
                        "POST:/matches/admin/**",
                        "PUT:/matches/admin/**").hasRole("ADMIN");

                auth.requestMatchers("/notifications/**").hasRole("ADMIN");
                auth.requestMatchers("POST:/tickets/admin/**").hasRole("ADMIN");
                auth.requestMatchers("/admin/audit/**").hasRole("ADMIN");
                auth.requestMatchers("/admin/dashboard/**").hasRole("ADMIN");
                auth.requestMatchers("/admin/sync/**").hasRole("ADMIN");
                auth.requestMatchers("POST:/album/user/*/packages/grant").hasRole("ADMIN");

                // Cualquier otra solicitud requiere autenticación
                auth.anyRequest().authenticated();
            })
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura la política de CORS de la aplicación.
     * <p>
     * Incluye el puerto {@code 5173} usado por Vite en desarrollo, además de
     * los puertos tradicionales de Angular/React (4200, 3000) y de Vue/Tomcat
     * (8080/8081/8082). Permite todos los headers y todos los métodos HTTP
     * relevantes para la API REST, incluyendo OPTIONS para el preflight.
     * </p>
     *
     * @return fuente de configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:5173",
                "http://localhost:8080",
                "http://localhost:8081",
                "http://localhost:8082"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Define el proveedor de autenticación basado en DAO.
     * <p>
     * Este proveedor utiliza el servicio de detalles de usuario y el
     * codificador de contraseñas para validar credenciales.
     * </p>
     *
     * @return proveedor de autenticación configurado
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Expone el AuthenticationManager como un bean de Spring.
     *
     * @param config configuración de autenticación
     * @return gestor de autenticación
     * @throws Exception en caso de error 
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define el codificador de contraseñas basado en BCrypt.
     *
     * @return codificador de contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}