/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.unbosque.mundial2026.dto.UserDTO;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.User.Role;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.util.AESUtil;

/**
 * Servicio encargado de la lógica de negocio relacionada con la entidad
 * {@link User} en la plataforma Mundial 2026 Hub.
 * Implementa {@link CRUDOperation} para proveer operaciones estándar sobre
 * usuarios. Los datos sensibles (nombre, username, email) se encriptan con AES
 * antes de persistir y se desencriptan al leer, siguiendo el mismo patrón del
 * proyecto VirusDetected. La contraseña se codifica siempre con BCrypt.
 * Convenciones de códigos de retorno usados en este servicio:
 * <ul>
 *   <li>0 — Operación exitosa.</li>
 *   <li>1 — Dato duplicado (username o email ya en uso).</li>
 *   <li>2 — Entidad no encontrada.</li>
 *   <li>3 — Error genérico / estado inalcanzable.</li>
 *   <li>4 — Contraseña inválida (no cumple política).</li>
 *   <li>5 — Email inválido (formato incorrecto).</li>
 *   <li>6 — Campo contiene caracteres HTML no permitidos.</li>
 *   <li>7 — Código de verificación / recuperación incorrecto.</li>
 * </ul>
 */
@Service
public class UserService implements CRUDOperation<UserDTO, User> {

    /**
     * Repositorio JPA para operaciones de persistencia de usuarios.
     */
    @Autowired
    private UserRepository userRepo;

    /**
     * Mapper para conversión entre objetos DTO y entidades JPA.
     */
    @Autowired
    private ModelMapper modelMapper;

    /**
     * Codificador de contraseñas BCrypt inyectado desde {@code SecurityConfig}.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Servicio de envío de correos para verificación y recuperación.
     */
    @Autowired
    private EmailService emailService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public UserService() {
    }

    // =========================================================================
    // Implementación de CRUDOperation
    // =========================================================================

    /**
     * Crea un nuevo usuario en el sistema con rol {@code USER} por defecto.
     * Encripta los campos sensibles con AES y la contraseña con BCrypt.
     * Valida unicidad de username y email antes de persistir.
     * <p>
     * El usuario queda con {@code enabled = false} hasta que verifique
     * su cuenta con el código de 6 dígitos que se envía al correo.
     * </p>
     *
     * @param data El DTO con los datos del nuevo usuario.
     * @return 0 si el registro fue exitoso; 1 si username o email ya existen;
     *         3 si faltan campos requeridos; 4 si la contraseña no cumple la
     *         política; 5 si el email es inválido; 6 si algún campo contiene HTML.
     */
    @Override
    public int create(UserDTO data) {
        if (data.getUsername() == null || data.getUsername().isEmpty()
                || data.getPassword() == null || data.getPassword().isEmpty()
                || data.getEmail() == null || data.getEmail().isEmpty()
                || data.getName() == null || data.getName().isEmpty()) {
            return 3;
        }

        if (containsHtmlSymbols(data.getName()) || containsHtmlSymbols(data.getUsername())) {
            return 6;
        }
        if (!isValidPassword(data.getPassword())) {
            return 4;
        }
        if (!isValidEmail(data.getEmail())) {
            return 5;
        }

        String encryptedUsername = AESUtil.encrypt(data.getUsername());
        String encryptedEmail = AESUtil.encrypt(data.getEmail());

        if (userRepo.existsByUsername(encryptedUsername) || userRepo.existsByEmail(encryptedEmail)) {
            return 1;
        }

        // Genera código de verificación de 6 dígitos y lo persiste encriptado.
        String codigoPlano = EmailService.generarCodigo6Digitos();

        User entity = modelMapper.map(data, User.class);
        entity.setUsername(encryptedUsername);
        entity.setName(AESUtil.encrypt(data.getName()));
        entity.setEmail(encryptedEmail);
        entity.setVerificationCode(AESUtil.encrypt(codigoPlano));
        entity.setPassword(passwordEncoder.encode(data.getPassword()));
        entity.setRole(Role.USER);
        // El usuario no podrá hacer login hasta verificar su cuenta.
        entity.setEnabled(false);

        userRepo.save(entity);

        // Dispara el correo. Si SMTP falla, el registro se completa igual
        // y queda log de error; el usuario podrá pedir reenvío del código.
        emailService.enviarCodigoVerificacion(data.getEmail(), codigoPlano);

        return 0;
    }

    /**
     * Obtiene todos los usuarios registrados con datos sensibles desencriptados.
     * La contraseña nunca se incluye en la respuesta.
     *
     * @return Lista de {@link UserDTO} con datos desencriptados.
     */
    @Override
    public List<UserDTO> getAll() {
        List<User> entities = userRepo.findAll();
        List<UserDTO> dtoList = new ArrayList<>();
        entities.forEach(entity -> {
            UserDTO dto = modelMapper.map(entity, UserDTO.class);
            dto.setPassword(null);
            decryptUserDTO(dto);
            dtoList.add(dto);
        });
        return dtoList;
    }

    /**
     * Elimina un usuario por su ID.
     *
     * @param id El ID del usuario a eliminar.
     * @return 0 si fue eliminado; 2 si no existe.
     */
    @Override
    public int deleteById(Long id) {
        Optional<User> found = userRepo.findById(id);
        if (found.isPresent()) {
            userRepo.delete(found.get());
            return 0;
        }
        return 2;
    }

    /**
     * Actualiza los datos del perfil de un usuario (nombre, ciudad favorita,
     * equipo favorito, preferencias de notificación). No actualiza contraseña
     * ni email por este método; esos tienen métodos dedicados.
     *
     * @param id      El ID del usuario a actualizar.
     * @param newData El DTO con los nuevos datos de perfil.
     * @return 0 si fue actualizado; 2 si no existe; 6 si hay HTML en los campos.
     */
    @Override
    public int updateById(Long id, UserDTO newData) {
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return 2;
        }

        if (newData.getName() != null && containsHtmlSymbols(newData.getName())) {
            return 6;
        }

        User entity = found.get();

        if (newData.getName() != null && !newData.getName().isEmpty()) {
            entity.setName(AESUtil.encrypt(newData.getName()));
        }
        if (newData.getFavoriteTeamCode() != null) {
            entity.setFavoriteTeamCode(newData.getFavoriteTeamCode());
        }
        if (newData.getPreferredCity() != null) {
            entity.setPreferredCity(newData.getPreferredCity());
        }
        entity.setPushNotificationsEnabled(newData.isPushNotificationsEnabled());
        entity.setEmailNotificationsEnabled(newData.isEmailNotificationsEnabled());

        userRepo.save(entity);
        return 0;
    }

    /**
     * Cuenta el total de usuarios registrados.
     *
     * @return Número total de usuarios en la base de datos.
     */
    @Override
    public long count() {
        return userRepo.count();
    }

    /**
     * Verifica si existe un usuario con el ID indicado.
     *
     * @param id El ID a verificar.
     * @return {@code true} si existe.
     */
    @Override
    public boolean exist(Long id) {
        return userRepo.existsById(id);
    }

    // =========================================================================
    // Métodos específicos de usuario
    // =========================================================================

    /**
     * Actualiza la contraseña de un usuario verificado por su username.
     * Se usa en el flujo de recuperación de contraseña. La nueva contraseña
     * se valida y se codifica con BCrypt antes de persistir.
     *
     * @param username    El username (sin encriptar) del usuario.
     * @param newPassword La nueva contraseña (sin codificar).
     * @return 0 si fue actualizada; 2 si el usuario no existe; 4 si la contraseña
     *         no cumple la política de seguridad.
     */
    public int updatePassword(String username, String newPassword) {
        if (!isValidPassword(newPassword)) {
            return 4;
        }
        Optional<User> found = userRepo.findByUsername(AESUtil.encrypt(username));
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        entity.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(entity);
        return 0;
    }

    /**
     * Actualiza el correo electrónico de un usuario por su ID.
     * Valida formato y unicidad antes de persistir.
     *
     * @param id       El ID del usuario.
     * @param newEmail El nuevo correo electrónico (sin encriptar).
     * @return 0 si fue actualizado; 1 si el email ya está en uso; 2 si no existe;
     *         5 si el email tiene formato inválido.
     */
    public int updateEmail(Long id, String newEmail) {
        if (!isValidEmail(newEmail)) {
            return 5;
        }
        String encryptedEmail = AESUtil.encrypt(newEmail);
        if (userRepo.existsByEmail(encryptedEmail)) {
            return 1;
        }
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        entity.setEmail(encryptedEmail);
        userRepo.save(entity);
        return 0;
    }

    /**
     * Bloquea la cuenta de un usuario (HU17 — Bloquear usuario).
     * Establece {@code accountNonLocked = false} y {@code enabled = false}.
     *
     * @param id El ID del usuario a bloquear.
     * @return 0 si fue bloqueado; 2 si no existe.
     */
    public int blockUser(Long id) {
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        entity.setAccountNonLocked(false);
        entity.setEnabled(false);
        userRepo.save(entity);
        return 0;
    }

    /**
     * Desbloquea la cuenta de un usuario (HU18 — Desbloquear usuario).
     * Restablece {@code accountNonLocked = true} y {@code enabled = true}.
     *
     * @param id El ID del usuario a desbloquear.
     * @return 0 si fue desbloqueado; 2 si no existe.
     */
    public int unblockUser(Long id) {
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        entity.setAccountNonLocked(true);
        entity.setEnabled(true);
        userRepo.save(entity);
        return 0;
    }

    /**
     * Asigna un rol específico a un usuario (HU26 — Gestionar roles).
     *
     * @param id   El ID del usuario.
     * @param role El nuevo rol a asignar.
     * @return 0 si fue actualizado; 2 si no existe.
     */
    public int updateRole(Long id, Role role) {
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        entity.setRole(role);
        userRepo.save(entity);
        return 0;
    }

    /**
     * Actualiza el código de verificación de un usuario.
     * Se usa en el flujo de recuperación de contraseña: se genera un código,
     * se envía al correo del usuario y se guarda encriptado.
     *
     * @param id   El ID del usuario.
     * @param code El nuevo código de verificación (sin encriptar).
     * @return 0 si fue actualizado; 2 si no existe.
     */
    public int updateVerificationCode(Long id, String code) {
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        entity.setVerificationCode(AESUtil.encrypt(code));
        userRepo.save(entity);
        return 0;
    }

    /**
     * Busca un usuario por su username y lo retorna con datos desencriptados.
     * Se usa en el servicio de autenticación para obtener detalles del usuario
     * tras el login exitoso.
     *
     * @param username El username sin encriptar.
     * @return El {@link UserDTO} desencriptado, o {@code null} si no existe.
     */
    public UserDTO getByUsername(String username) {
        Optional<User> found = userRepo.findByUsername(AESUtil.encrypt(username));
        if (found.isEmpty()) {
            return null;
        }
        UserDTO dto = modelMapper.map(found.get(), UserDTO.class);
        dto.setPassword(null);
        decryptUserDTO(dto);
        return dto;
    }

    /**
     * Busca un usuario por su email y lo retorna con datos desencriptados.
     * Se usa en el flujo de recuperación de contraseña y como fallback de login.
     *
     * @param email El email sin encriptar.
     * @return El {@link UserDTO} desencriptado, o {@code null} si no existe.
     */
    public UserDTO getByEmail(String email) {
        Optional<User> found = userRepo.findByEmail(AESUtil.encrypt(email));
        if (found.isEmpty()) {
            return null;
        }
        UserDTO dto = modelMapper.map(found.get(), UserDTO.class);
        dto.setPassword(null);
        decryptUserDTO(dto);
        return dto;
    }

    /**
     * Obtiene un usuario por su ID con datos desencriptados.
     *
     * @param id El ID del usuario.
     * @return El {@link UserDTO} desencriptado, o {@code null} si no existe.
     */
    public UserDTO getById(Long id) {
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return null;
        }
        UserDTO dto = modelMapper.map(found.get(), UserDTO.class);
        dto.setPassword(null);
        decryptUserDTO(dto);
        return dto;
    }

    /**
     * Verifica si existe un usuario con el username dado (sin encriptar).
     *
     * @param username El username sin encriptar.
     * @return {@code true} si el username ya está registrado.
     */
    public boolean usernameExists(String username) {
        return userRepo.existsByUsername(AESUtil.encrypt(username));
    }

    /**
     * Encripta los campos sensibles de un DTO y retorna la entidad resultante.
     * Usado por {@code UserDetailsServiceImpl} en el flujo de autenticación.
     *
     * @param data El DTO con los datos sin encriptar.
     * @return Entidad {@link User} con los campos sensibles encriptados.
     */
    public User encrypt(UserDTO data) {
        User entity = modelMapper.map(data, User.class);
        if (entity.getUsername() != null) {
            entity.setUsername(AESUtil.encrypt(entity.getUsername()));
        }
        if (entity.getName() != null) {
            entity.setName(AESUtil.encrypt(entity.getName()));
        }
        if (entity.getEmail() != null) {
            entity.setEmail(AESUtil.encrypt(entity.getEmail()));
        }
        return entity;
    }

    // =========================================================================
    // Verificación de cuenta y recuperación de contraseña
    // =========================================================================

    /**
     * Resuelve el identificador del usuario (username o email) y retorna su
     * username encriptado, que es lo que el AuthenticationManager necesita
     * para autenticar.
     * <p>
     * Acepta como entrada texto plano sin saber si es un username o un email,
     * encripta ambas variantes y prueba en BD. Si alguna calza, retorna el
     * username encriptado correspondiente a ese usuario.
     * </p>
     *
     * @param identifier Texto plano que puede ser username o email.
     * @return El username encriptado del usuario si se encontró, o
     *         {@code null} si ningún usuario coincide.
     */
    public String resolveIdentifierToEncryptedUsername(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }
        String encrypted = AESUtil.encrypt(identifier);

        Optional<User> byUsername = userRepo.findByUsername(encrypted);
        if (byUsername.isPresent()) {
            return byUsername.get().getUsername();
        }
        Optional<User> byEmail = userRepo.findByEmail(encrypted);
        if (byEmail.isPresent()) {
            return byEmail.get().getUsername();
        }
        return null;
    }

    /**
     * Valida el código de verificación recibido durante el flujo de registro.
     * Si el código coincide, activa la cuenta del usuario (enabled = true)
     * y consume el código reemplazándolo por "0" para que no pueda usarse
     * nuevamente.
     *
     * @param email  Email del usuario que se registró.
     * @param codigo Código de 6 dígitos en texto plano enviado por el usuario.
     * @return 0 si el código es válido y la cuenta se activó;
     *         7 si el código no coincide;
     *         2 si no existe el email.
     */
    public int verifyRegistrationCode(String email, String codigo) {
        Optional<User> found = userRepo.findByEmail(AESUtil.encrypt(email));
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        String stored = entity.getVerificationCode();
        if (stored == null) {
            return 7;
        }
        String storedPlain = AESUtil.decrypt(stored);
        if (!codigo.equals(storedPlain)) {
            return 7;
        }
        // Consume el código y activa la cuenta.
        entity.setVerificationCode(AESUtil.encrypt("0"));
        entity.setEnabled(true);
        userRepo.save(entity);
        return 0;
    }

    /**
     * Genera y envía un código de 6 dígitos al correo del usuario para
     * iniciar el flujo de recuperación de contraseña. El código se persiste
     * encriptado en el campo {@code verificationCode} del usuario.
     *
     * @param email Email del usuario que solicita la recuperación.
     * @return 0 si el código se generó y envió; 2 si no existe el email.
     */
    public int requestRecoveryCode(String email) {
        Optional<User> found = userRepo.findByEmail(AESUtil.encrypt(email));
        if (found.isEmpty()) {
            return 2;
        }
        String codigoPlano = EmailService.generarCodigo6Digitos();
        User entity = found.get();
        entity.setVerificationCode(AESUtil.encrypt(codigoPlano));
        userRepo.save(entity);

        emailService.enviarCodigoRecuperacion(email, codigoPlano);
        return 0;
    }

    /**
     * Valida que el código de recuperación coincida con el que está guardado
     * para el email indicado, SIN consumirlo. Esto permite que el frontend
     * confirme que el código es válido en un paso intermedio antes de pedir
     * la nueva contraseña.
     *
     * @param email  Email del usuario.
     * @param codigo Código de 6 dígitos que el usuario ingresó.
     * @return 0 si el código es válido; 7 si no coincide; 2 si no existe el email.
     */
    public int validateRecoveryCode(String email, String codigo) {
        Optional<User> found = userRepo.findByEmail(AESUtil.encrypt(email));
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        String stored = entity.getVerificationCode();
        if (stored == null) {
            return 7;
        }
        String storedPlain = AESUtil.decrypt(stored);
        if (!codigo.equals(storedPlain)) {
            return 7;
        }
        return 0;
    }

    /**
     * Completa el flujo de recuperación de contraseña: valida el código,
     * actualiza la contraseña del usuario y consume el código.
     *
     * @param email           Email del usuario.
     * @param codigo          Código de recuperación que llegó al correo.
     * @param nuevaContrasena Nueva contraseña en texto plano.
     * @return 0 si la contraseña se actualizó correctamente;
     *         7 si el código no coincide;
     *         2 si no existe el email;
     *         4 si la nueva contraseña no cumple la política de seguridad.
     */
    public int resetPasswordWithCode(String email, String codigo, String nuevaContrasena) {
        if (!isValidPassword(nuevaContrasena)) {
            return 4;
        }
        Optional<User> found = userRepo.findByEmail(AESUtil.encrypt(email));
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        String stored = entity.getVerificationCode();
        if (stored == null) {
            return 7;
        }
        String storedPlain = AESUtil.decrypt(stored);
        if (!codigo.equals(storedPlain)) {
            return 7;
        }
        entity.setPassword(passwordEncoder.encode(nuevaContrasena));
        entity.setVerificationCode(AESUtil.encrypt("0"));
        userRepo.save(entity);
        return 0;
    }

    // =========================================================================
    // Helpers de validación privados
    // =========================================================================

    /**
     * Verifica si una cadena contiene caracteres HTML que podrían usarse
     * para inyección. Se bloquean {@code <}, {@code >} y {@code :}.
     *
     * @param value La cadena a verificar.
     * @return {@code true} si la cadena contiene caracteres HTML no permitidos.
     */
    private boolean containsHtmlSymbols(String value) {
        return value != null && (value.contains("<") || value.contains(">") || value.contains(":"));
    }

    /**
     * Valida que la contraseña cumpla la política de seguridad:
     * mínimo 8 caracteres, al menos una mayúscula, una minúscula,
     * un número y un símbolo especial (no {@code <}, {@code >} ni {@code :}).
     *
     * @param password La contraseña a validar.
     * @return {@code true} si la contraseña cumple la política.
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (c != '<' && c != '>' && c != ':') hasSymbol = true;
        }
        return hasUpper && hasLower && hasDigit && hasSymbol;
    }

    /**
     * Valida que el email tenga un formato básico válido con {@code @} y dominio.
     *
     * @param email El correo electrónico a validar.
     * @return {@code true} si el formato es válido.
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Desencripta los campos sensibles de un {@link UserDTO} en el lugar.
     * No desencripta la contraseña.
     *
     * @param dto El DTO cuyos campos se van a desencriptar.
     */
    private void decryptUserDTO(UserDTO dto) {
        if (dto.getUsername() != null) {
            dto.setUsername(AESUtil.decrypt(dto.getUsername()));
        }
        if (dto.getName() != null) {
            dto.setName(AESUtil.decrypt(dto.getName()));
        }
        if (dto.getEmail() != null) {
            dto.setEmail(AESUtil.decrypt(dto.getEmail()));
        }
    }
}