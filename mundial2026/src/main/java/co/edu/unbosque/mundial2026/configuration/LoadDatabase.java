/**
 * Paquete que contiene las clases de configuración de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.User.Role;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.StadiumRepository;
import co.edu.unbosque.mundial2026.repository.TeamRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.util.AESUtil;

/**
 * Clase de configuración que inicializa la base de datos con datos semilla
 * al arrancar la aplicación Mundial 2026 Hub.
 * <p>
 * Sigue el mismo patrón del proyecto VirusDetected: usa {@link CommandLineRunner}
 * para ejecutar la carga inicial después de que el contexto de Spring esté listo.
 * Cada entidad se crea solo si no existe previamente, garantizando idempotencia
 * en reinicios del servidor durante el desarrollo.
 * </p>
 * <p>
 * Datos que se cargan al arranque:
 * <ul>
 *   <li>Usuario administrador por defecto.</li>
 *   <li>Usuario de prueba con rol USER.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Nota:</b> los estadios, equipos y partidos se cargan desde una API
 * externa en tiempo de ejecución; no se precargan datos semilla en la BD.
 * </p>
 */
@Configuration
public class LoadDatabase {

    /**
     * Logger para registrar el progreso de la carga inicial de datos.
     */
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    /**
     * Bean {@link CommandLineRunner} que ejecuta la inicialización de la base de
     * datos al arrancar la aplicación. Verifica la existencia de cada entidad
     * antes de crearla para garantizar idempotencia.
     *
     * @param userRepo        Repositorio de usuarios.
     * @param stadiumRepo     Repositorio de estadios (reservado para uso futuro).
     * @param teamRepo        Repositorio de equipos (reservado para uso futuro).
     * @param matchRepo       Repositorio de partidos (reservado para uso futuro).
     * @param passwordEncoder Codificador de contraseñas BCrypt.
     * @return El {@link CommandLineRunner} con la lógica de inicialización.
     */
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepo,
                                   StadiumRepository stadiumRepo,
                                   TeamRepository teamRepo,
                                   MatchRepository matchRepo,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("=== Mundial 2026 Hub — Inicializando base de datos ===");

            initUsers(userRepo, passwordEncoder);

            // Estadios, equipos y partidos se cargan desde una API externa
            // en tiempo de ejecución. No se precargan datos semilla aquí.

            log.info("=== Inicialización completada ===");
        };
    }

    // =========================================================================
    // Usuarios semilla
    // =========================================================================

    /**
     * Crea los usuarios de prueba si no existen. El email se almacena
     * encriptado con AES; username y name en texto plano. La contraseña
     * se codifica con BCrypt.
     *
     * @param userRepo        Repositorio de usuarios.
     * @param passwordEncoder Codificador BCrypt para las contraseñas.
     */
    private void initUsers(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        Optional<User> existingAdmin = userRepo.findByUsername("admin");
        if (existingAdmin.isPresent()) {
            log.info("Usuario admin ya existe — omitiendo creación");
        } else {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setName("Administrador");
            adminUser.setEmail(AESUtil.encrypt("admin@mundial2026hub.com"));
            adminUser.setPassword(passwordEncoder.encode("Admin2026!"));
            adminUser.setRole(Role.ADMIN);
            adminUser.setEnabled(true);
            userRepo.save(adminUser);
            log.info("Precargando usuario administrador");
        }

        Optional<User> existingUser = userRepo.findByUsername("testuser");
        if (existingUser.isPresent()) {
            log.info("Usuario de prueba ya existe — omitiendo creación");
        } else {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setName("Juan Díaz");
            testUser.setEmail(AESUtil.encrypt("testuser@gmail.com"));
            testUser.setPassword(passwordEncoder.encode("Test2026!"));
            testUser.setRole(Role.USER);
            testUser.setEnabled(true);
            userRepo.save(testUser);
            log.info("Precargando usuario de prueba");
        }
    }
}