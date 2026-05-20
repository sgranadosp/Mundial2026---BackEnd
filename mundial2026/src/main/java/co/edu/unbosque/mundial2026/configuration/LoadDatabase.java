/**
 * Paquete que contiene las clases de configuración de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Match.DataStatus;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.Match.Phase;
import co.edu.unbosque.mundial2026.model.Stadium;
import co.edu.unbosque.mundial2026.model.Team;
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
 *   <li>Estadios principales de los tres países anfitriones.</li>
 *   <li>Selecciones nacionales participantes.</li>
 *   <li>Partidos de ejemplo para la fase de grupos.</li>
 * </ul>
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
     * @param stadiumRepo     Repositorio de estadios.
     * @param teamRepo        Repositorio de equipos.
     * @param matchRepo       Repositorio de partidos.
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
            initStadiums(stadiumRepo);
            initTeams(teamRepo);
            initMatches(matchRepo, teamRepo, stadiumRepo);

            log.info("=== Inicialización completada ===");
        };
    }

    // =========================================================================
    // Usuarios semilla
    // =========================================================================

    /**
     * Crea los usuarios de prueba si no existen. El username y email se
     * almacenan encriptados con AES, igual que en el registro normal.
     *
     * @param userRepo        Repositorio de usuarios.
     * @param passwordEncoder Codificador BCrypt para las contraseñas.
     */
    private void initUsers(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        Optional<User> existingAdmin = userRepo.findByUsername(AESUtil.encrypt("admin"));
        if (existingAdmin.isPresent()) {
            log.info("Usuario admin ya existe — omitiendo creación");
        } else {
            User adminUser = new User();
            adminUser.setUsername(AESUtil.encrypt("admin"));
            adminUser.setName(AESUtil.encrypt("Administrador"));
            adminUser.setEmail(AESUtil.encrypt("admin@mundial2026hub.com"));
            adminUser.setVerificationCode(AESUtil.encrypt("0"));
            adminUser.setPassword(passwordEncoder.encode("Admin2026!"));
            adminUser.setRole(Role.ADMIN);
            userRepo.save(adminUser);
            log.info("Precargando usuario administrador (admin / Admin2026!)");
        }

        Optional<User> existingUser = userRepo.findByUsername(AESUtil.encrypt("testuser"));
        if (existingUser.isPresent()) {
            log.info("Usuario de prueba ya existe — omitiendo creación");
        } else {
            User testUser = new User();
            testUser.setUsername(AESUtil.encrypt("testuser"));
            testUser.setName(AESUtil.encrypt("Juan Díaz"));
            testUser.setEmail(AESUtil.encrypt("testuser@gmail.com"));
            testUser.setVerificationCode(AESUtil.encrypt("0"));
            testUser.setPassword(passwordEncoder.encode("Test2026!"));
            testUser.setRole(Role.USER);
            testUser.setFavoriteTeamCode("COL");
            testUser.setPreferredCity("Miami");
            userRepo.save(testUser);
            log.info("Precargando usuario de prueba (testuser / Test2026!)");
        }
    }

    // =========================================================================
    // Estadios semilla
    // =========================================================================

    /**
     * Crea los estadios sede del Mundial 2026 si no existen.
     * Cubre los tres países anfitriones: EE. UU., Canadá y México.
     *
     * @param stadiumRepo Repositorio de estadios.
     */
    private void initStadiums(StadiumRepository stadiumRepo) {
        Object[][] stadiums = {
            {"MetLife Stadium",      "East Rutherford", "USA",    "America/New_York",   40.8135, -74.0745, 82500},
            {"AT&T Stadium",         "Arlington",       "USA",    "America/Chicago",    32.7480, -97.0928, 80000},
            {"SoFi Stadium",         "Inglewood",       "USA",    "America/Los_Angeles",33.9535,-118.3392, 70240},
            {"Hard Rock Stadium",    "Miami Gardens",   "USA",    "America/New_York",   25.9580, -80.2389, 65326},
            {"Levi's Stadium",       "Santa Clara",     "USA",    "America/Los_Angeles",37.4032,-121.9697, 68500},
            {"Gillette Stadium",     "Foxborough",      "USA",    "America/New_York",   42.0909, -71.2643, 65878},
            {"Lincoln Financial",    "Philadelphia",    "USA",    "America/New_York",   39.9008, -75.1675, 69176},
            {"Arrowhead Stadium",    "Kansas City",     "USA",    "America/Chicago",    39.0489, -94.4839, 76416},
            {"Lumen Field",          "Seattle",         "USA",    "America/Los_Angeles",47.5952,-122.3316, 68740},
            {"Rose Bowl",            "Pasadena",        "USA",    "America/Los_Angeles",34.1614,-118.1676, 88565},
            {"Estadio Azteca",       "Ciudad de México","Mexico", "America/Mexico_City",19.3029, -99.1505, 87523},
            {"Estadio Jalisco",      "Guadalajara",     "Mexico", "America/Mexico_City",20.6752,-103.4668, 55030},
            {"BC Place",             "Vancouver",       "Canada", "America/Vancouver",  49.2768,-123.1118, 54500},
            {"BMO Field",            "Toronto",         "Canada", "America/Toronto",    43.6333, -79.4179, 45736},
        };

        for (Object[] s : stadiums) {
            if (stadiumRepo.findByName((String) s[0]).isPresent()) {
                continue;
            }
            Stadium stadium = new Stadium(
                    (String) s[0],
                    (String) s[1],
                    (String) s[2],
                    (String) s[3],
                    (Double) s[4],
                    (Double) s[5],
                    (Integer) s[6]
            );
            stadiumRepo.save(stadium);
            log.info("Precargando estadio: {}", s[0]);
        }
    }

    // =========================================================================
    // Equipos semilla
    // =========================================================================

    /**
     * Crea las selecciones nacionales participantes si no existen.
     * Solo se cargan los datos básicos; los datos completos se sincronizan
     * desde la API externa usando {@code ExternalMatchService}.
     *
     * @param teamRepo Repositorio de equipos.
     */
    private void initTeams(TeamRepository teamRepo) {
        Object[][] teams = {
            {1L,  "Colombia",         "COL", "COL"},
            {2L,  "Brasil",           "BRA", "BRA"},
            {3L,  "Argentina",        "ARG", "ARG"},
            {4L,  "Francia",          "FRA", "FRA"},
            {5L,  "España",           "ESP", "ESP"},
            {6L,  "Alemania",         "GER", "GER"},
            {7L,  "Portugal",         "POR", "POR"},
            {8L,  "Marruecos",        "MAR", "MAR"},
            {9L,  "Estados Unidos",   "USA", "USA"},
            {10L, "México",           "MEX", "MEX"},
            {11L, "Canadá",           "CAN", "CAN"},
            {12L, "Inglaterra",       "ENG", "ENG"},
            {13L, "Croacia",          "CRO", "CRO"},
            {14L, "Países Bajos",     "NED", "NED"},
            {15L, "Italia",           "ITA", "ITA"},
            {16L, "Senegal",          "SEN", "SEN"},
        };

        for (Object[] t : teams) {
            if (teamRepo.existsByExternalId((Long) t[0])) {
                continue;
            }
            Team team = new Team(
                    (Long) t[0],
                    (String) t[1],
                    (String) t[2],
                    (String) t[3],
                    null
            );
            teamRepo.save(team);
            log.info("Precargando equipo: {}", t[1]);
        }
    }

    // =========================================================================
    // Partidos semilla
    // =========================================================================

    /**
     * Crea partidos de ejemplo de la fase de grupos si no existen.
     * Sirven para demostración del sistema durante el desarrollo y la
     * sustentación sin necesidad de conectar la API externa.
     *
     * @param matchRepo   Repositorio de partidos.
     * @param teamRepo    Repositorio de equipos.
     * @param stadiumRepo Repositorio de estadios.
     */
    private void initMatches(MatchRepository matchRepo,
                              TeamRepository teamRepo,
                              StadiumRepository stadiumRepo) {
        if (matchRepo.count() > 0) {
            log.info("Partidos ya existen — omitiendo carga de partidos semilla");
            return;
        }

        Optional<Team> colombia    = teamRepo.findByIsoCode("COL");
        Optional<Team> brasil      = teamRepo.findByIsoCode("BRA");
        Optional<Team> argentina   = teamRepo.findByIsoCode("ARG");
        Optional<Team> francia     = teamRepo.findByIsoCode("FRA");
        Optional<Team> espana      = teamRepo.findByIsoCode("ESP");
        Optional<Team> alemania    = teamRepo.findByIsoCode("GER");
        Optional<Team> portugal    = teamRepo.findByIsoCode("POR");
        Optional<Team> marruecos   = teamRepo.findByIsoCode("MAR");

        Optional<Stadium> metlife  = stadiumRepo.findByName("MetLife Stadium");
        Optional<Stadium> azteca   = stadiumRepo.findByName("Estadio Azteca");
        Optional<Stadium> roseBowl = stadiumRepo.findByName("Rose Bowl");
        Optional<Stadium> sofi     = stadiumRepo.findByName("SoFi Stadium");

        createMatch(matchRepo, 1001L,
                colombia.orElse(null), brasil.orElse(null), metlife.orElse(null),
                LocalDateTime.of(2026, 6, 15, 17, 0),
                Phase.GROUP_STAGE, "A", 1, MatchStatus.SCHEDULED);

        createMatch(matchRepo, 1002L,
                argentina.orElse(null), francia.orElse(null), azteca.orElse(null),
                LocalDateTime.of(2026, 6, 15, 20, 0),
                Phase.GROUP_STAGE, "B", 1, MatchStatus.LIVE);

        createMatch(matchRepo, 1003L,
                espana.orElse(null), alemania.orElse(null), roseBowl.orElse(null),
                LocalDateTime.of(2026, 6, 14, 20, 0),
                Phase.GROUP_STAGE, "C", 1, MatchStatus.FINISHED);

        createMatch(matchRepo, 1004L,
                portugal.orElse(null), marruecos.orElse(null), sofi.orElse(null),
                LocalDateTime.of(2026, 6, 16, 18, 0),
                Phase.GROUP_STAGE, "D", 1, MatchStatus.SCHEDULED);

        log.info("Precargando {} partidos de ejemplo", 4);
    }

    /**
     * Crea y persiste un partido si no existe ya uno con el mismo {@code externalId}.
     *
     * @param matchRepo   Repositorio de partidos.
     * @param externalId  Identificador externo del partido.
     * @param homeTeam    Equipo local.
     * @param awayTeam    Equipo visitante.
     * @param stadium     Estadio del partido.
     * @param scheduledAt Fecha y hora programada en UTC.
     * @param phase       Fase del torneo.
     * @param groupName   Nombre del grupo (ej. "A").
     * @param matchday    Número de jornada.
     * @param status      Estado inicial del partido.
     */
    private void createMatch(MatchRepository matchRepo, Long externalId,
                              Team homeTeam, Team awayTeam, Stadium stadium,
                              LocalDateTime scheduledAt, Phase phase,
                              String groupName, int matchday, MatchStatus status) {
        if (matchRepo.existsByExternalId(externalId)) return;
        if (homeTeam == null || awayTeam == null || stadium == null) return;

        Match match = new Match(externalId, homeTeam, awayTeam, stadium, scheduledAt, phase);
        match.setGroupName(groupName);
        match.setMatchday(matchday);
        match.setStatus(status);
        match.setDataStatus(DataStatus.CONFIRMED);

        if (status == MatchStatus.FINISHED) {
            match.setHomeScore(1);
            match.setAwayScore(1);
        } else if (status == MatchStatus.LIVE) {
            match.setHomeScore(2);
            match.setAwayScore(1);
        }

        matchRepo.save(match);
    }
}