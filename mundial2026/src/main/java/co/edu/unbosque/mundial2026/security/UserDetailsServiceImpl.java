/**
 * Paquete que contiene las clases relacionadas con la seguridad de la
 * aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Implementación de {@link UserDetailsService} de Spring Security para la
 * plataforma Mundial 2026 Hub.
 * <p>
 * Spring Security llama a {@link #loadUserByUsername(String)} durante el
 * proceso de autenticación para cargar los detalles del usuario desde la base
 * de datos. En esta aplicación, el username que llega como parámetro ya viene
 * encriptado con AES (porque el token JWT almacena el username encriptado como
 * subject), por lo que se busca directamente en la BD sin encriptarlo de nuevo.
 * </p>
 * <p>
 * La entidad {@link co.edu.unbosque.mundial2026.model.User} implementa
 * {@link UserDetails}, por lo que puede retornarse directamente sin conversión.
 * </p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * Repositorio JPA para acceder a los datos de usuarios.
     */
    private final UserRepository userRepository;

    /**
     * Constructor con inyección del repositorio de usuarios.
     *
     * @param userRepository El repositorio de usuarios.
     */
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carga los detalles del usuario a partir de su username encriptado.
     * <p>
     * Este método es invocado por Spring Security durante:
     * <ul>
     *   <li>La autenticación inicial: el {@code AuthController} encripta el
     *       username antes de pasarlo al {@code AuthenticationManager}, que
     *       delega aquí.</li>
     *   <li>La validación de cada petición posterior: el
     *       {@link JwtAuthenticationFilter} extrae el username (encriptado) del
     *       subject del JWT y llama a este método para recargar el usuario.</li>
     * </ul>
     * </p>
     *
     * @param username El username encriptado con AES del usuario a cargar.
     * @return El objeto {@link UserDetails} (instancia de
     *         {@link co.edu.unbosque.mundial2026.model.User}) correspondiente.
     * @throws UsernameNotFoundException Si no existe ningún usuario con el
     *                                   username indicado en la base de datos.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con username: " + username));
    }
}