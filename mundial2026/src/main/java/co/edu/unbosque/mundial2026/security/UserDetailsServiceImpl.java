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
 * proceso de autenticación. En esta aplicación,
 * {@link co.edu.unbosque.mundial2026.controller.AuthController} ya resolvió
 * el identificador entrante (que puede ser un username o un email en texto
 * plano) a su {@code username} en texto plano antes de delegar al
 * {@code AuthenticationManager}, por lo que aquí siempre se recibe un username.
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
     * Carga los detalles del usuario a partir de su username en texto plano.
     * <p>
     * La entidad {@link co.edu.unbosque.mundial2026.model.User} implementa
     * {@link UserDetails}, por lo que puede retornarse directamente sin
     * conversión adicional.
     * </p>
     *
     * @param username El username (texto plano) del usuario.
     * @return El objeto {@link UserDetails} correspondiente.
     * @throws UsernameNotFoundException Si no existe ningún usuario con ese
     *                                   username en la base de datos.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .<UserDetails>map(u -> u)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con username: " + username));
    }
}