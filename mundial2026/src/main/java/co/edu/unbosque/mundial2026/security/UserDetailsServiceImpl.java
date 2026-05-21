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
 * de datos. En esta aplicación el "username" que llega al método puede ser
 * realmente el <b>username</b> O el <b>email</b> del usuario (ambos encriptados
 * con AES, ya que el {@link co.edu.unbosque.mundial2026.controller.AuthController}
 * encripta el identificador entrante antes de delegar al
 * {@code AuthenticationManager}).
 * </p>
 * <p>
 * Por eso este servicio busca primero por username; si no encuentra, busca
 * por email. La entidad {@link co.edu.unbosque.mundial2026.model.User} implementa
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
     * Carga los detalles del usuario a partir de su identificador encriptado.
     * <p>
     * El identificador puede ser tanto el username como el email del usuario
     * (ambos encriptados con AES). El método:
     * <ol>
     *   <li>Intenta primero buscar por username.</li>
     *   <li>Si no encuentra, intenta buscar por email.</li>
     *   <li>Si tampoco encuentra, lanza {@link UsernameNotFoundException}.</li>
     * </ol>
     * </p>
     *
     * @param identifier El identificador encriptado con AES del usuario.
     *                   Puede ser su username o su email.
     * @return El objeto {@link UserDetails} correspondiente.
     * @throws UsernameNotFoundException Si no existe ningún usuario con ese
     *                                   identificador en la base de datos.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return userRepository.findByUsername(identifier)
                .<UserDetails>map(u -> u)
                .orElseGet(() -> userRepository.findByEmail(identifier)
                        .<UserDetails>map(u -> u)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "Usuario no encontrado con identificador: " + identifier)));
    }
}