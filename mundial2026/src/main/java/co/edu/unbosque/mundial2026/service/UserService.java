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
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.mundial2026.dto.UserDTO;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.User.Role;
import co.edu.unbosque.mundial2026.model.VerificationCode;
import co.edu.unbosque.mundial2026.model.VerificationCode.Purpose;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.repository.VerificationCodeRepository;
import co.edu.unbosque.mundial2026.util.AESUtil;

/**
 * Servicio encargado de la lógica de negocio relacionada con la entidad
 * {@link User} en la plataforma Mundial 2026 Hub.
 * <p>
 * Implementa {@link CRUDOperation} para proveer operaciones estándar sobre
 * usuarios. La política de cifrado:
 * <ul>
 *   <li>{@code email} se encripta con AES antes de persistir.</li>
 *   <li>{@code username} y {@code name} se almacenan en texto plano.</li>
 *   <li>{@code password} se codifica siempre con BCrypt.</li>
 *   <li>Los códigos de verificación / recuperación se persisten encriptados
 *       en la tabla independiente {@link VerificationCode}, asociados al
 *       email encriptado y a un propósito específico.</li>
 * </ul>
 * </p>
 * <p>
 * Convenciones de códigos de retorno usados en este servicio:
 * <ul>
 *   <li>0 — Operación exitosa.</li>
 *   <li>1 — Dato duplicado (username o email ya en uso).</li>
 *   <li>2 — Entidad no encontrada.</li>
 *   <li>3 — Faltan campos requeridos.</li>
 *   <li>4 — Contraseña inválida (no cumple política).</li>
 *   <li>5 — Email inválido (formato incorrecto).</li>
 *   <li>6 — Campo contiene caracteres HTML no permitidos.</li>
 *   <li>7 — Código de verificación / recuperación incorrecto.</li>
 * </ul>
 * </p>
 */
@Service
public class UserService implements CRUDOperation<UserDTO, User> {

    /**
     * Repositorio JPA para operaciones de persistencia de usuarios.
     */
    @Autowired
    private UserRepository userRepo;

    /**
     * Repositorio JPA para códigos de verificación de un solo uso.
     */
    @Autowired
    private VerificationCodeRepository codeRepo;

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
     * Encripta el email con AES, codifica la contraseña con BCrypt, y guarda
     * el nombre y el username en texto plano. Valida unicidad antes de persistir.
     * <p>
     * El usuario queda con {@code enabled = false} hasta que verifique
     * su cuenta con el código de 6 dígitos que se envía al correo.
     * El código se persiste en {@link VerificationCode} con propósito
     * {@link Purpose#REGISTRATION}.
     * </p>
     *
     * @param data El DTO con los datos del nuevo usuario.
     * @return 0 si el registro fue exitoso; 1 si username o email ya existen;
     *         3 si faltan campos requeridos; 4 si la contraseña no cumple la
     *         política; 5 si el email es inválido; 6 si algún campo contiene HTML.
     */
    @Override
    @Transactional
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

        String encryptedEmail = AESUtil.encrypt(data.getEmail());

        if (userRepo.existsByUsername(data.getUsername()) || userRepo.existsByEmail(encryptedEmail)) {
            return 1;
        }

        User entity = new User();
        entity.setName(data.getName());
        entity.setUsername(data.getUsername());
        entity.setEmail(encryptedEmail);
        entity.setPassword(passwordEncoder.encode(data.getPassword()));
        entity.setRole(Role.USER);
        // El usuario no podrá hacer login hasta verificar su cuenta.
        entity.setEnabled(false);

        userRepo.save(entity);

        // Genera código de verificación de 6 dígitos, persiste encriptado
        // en la tabla aparte, y envía por correo.
        String codigoPlano = EmailService.generarCodigo6Digitos();
        issueCode(encryptedEmail, codigoPlano, Purpose.REGISTRATION);

        emailService.enviarCodigoVerificacion(data.getEmail(), data.getName(), codigoPlano);

        return 0;
    }

    /**
     * Obtiene todos los usuarios registrados con el email desencriptado.
     * La contraseña nunca se incluye en la respuesta.
     *
     * @return Lista de {@link UserDTO} con email desencriptado.
     */
    @Override
    public List<UserDTO> getAll() {
        List<User> entities = userRepo.findAll();
        List<UserDTO> dtoList = new ArrayList<>();
        entities.forEach(entity -> {
            UserDTO dto = modelMapper.map(entity, UserDTO.class);
            dto.setPassword(null);
            decryptEmail(dto);
            dtoList.add(dto);
        });
        return dtoList;
    }

    /**
     * Elimina un usuario por su ID. También limpia cualquier código de
     * verificación pendiente asociado al email del usuario.
     *
     * @param id El ID del usuario a eliminar.
     * @return 0 si fue eliminado; 2 si no existe.
     */
    @Override
    @Transactional
    public int deleteById(Long id) {
        Optional<User> found = userRepo.findById(id);
        if (found.isPresent()) {
            User u = found.get();
            codeRepo.deleteByEmail(u.getEmail());
            userRepo.delete(u);
            return 0;
        }
        return 2;
    }

    /**
     * Actualiza los datos básicos del perfil de un usuario (nombre y
     * preferencias de notificación). No actualiza contraseña ni email
     * por este método; esos tienen flujos dedicados.
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
            entity.setName(newData.getName());
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
     * Actualiza la contraseña de un usuario por su username. La nueva contraseña
     * se valida y se codifica con BCrypt antes de persistir.
     *
     * @param username    El username del usuario (texto plano).
     * @param newPassword La nueva contraseña (sin codificar).
     * @return 0 si fue actualizada; 2 si el usuario no existe; 4 si la contraseña
     *         no cumple la política de seguridad.
     */
    public int updatePassword(String username, String newPassword) {
        if (!isValidPassword(newPassword)) {
            return 4;
        }
        Optional<User> found = userRepo.findByUsername(username);
        if (found.isEmpty()) {
            return 2;
        }
        User entity = found.get();
        entity.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(entity);
        return 0;
    }

    /**
     * Actualiza el correo electrónico de un usuario por su ID. Valida formato
     * y unicidad antes de persistir.
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
     * Bloquea la cuenta de un usuario (HU17). Establece
     * {@code accountNonLocked = false} y {@code enabled = false}.
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
     * Desbloquea la cuenta de un usuario (HU18). Restablece
     * {@code accountNonLocked = true} y {@code enabled = true}.
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
     * Asigna un rol específico a un usuario (HU26).
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
     * Busca un usuario por su username y lo retorna con el email desencriptado.
     * Se usa en el servicio de autenticación para obtener detalles del usuario
     * tras el login exitoso.
     *
     * @param username El username del usuario (texto plano).
     * @return El {@link UserDTO} con email desencriptado, o {@code null} si no existe.
     */
    public UserDTO getByUsername(String username) {
        Optional<User> found = userRepo.findByUsername(username);
        if (found.isEmpty()) {
            return null;
        }
        UserDTO dto = modelMapper.map(found.get(), UserDTO.class);
        dto.setPassword(null);
        decryptEmail(dto);
        return dto;
    }

    /**
     * Busca un usuario por su email (sin encriptar) y lo retorna con el email
     * desencriptado en el DTO. Se usa en el flujo de recuperación de contraseña.
     *
     * @param email El email del usuario (texto plano).
     * @return El {@link UserDTO} desencriptado, o {@code null} si no existe.
     */
    public UserDTO getByEmail(String email) {
        Optional<User> found = userRepo.findByEmail(AESUtil.encrypt(email));
        if (found.isEmpty()) {
            return null;
        }
        UserDTO dto = modelMapper.map(found.get(), UserDTO.class);
        dto.setPassword(null);
        decryptEmail(dto);
        return dto;
    }

    /**
     * Obtiene un usuario por su ID con el email desencriptado.
     *
     * @param id El ID del usuario.
     * @return El {@link UserDTO} con email desencriptado, o {@code null} si no existe.
     */
    public UserDTO getById(Long id) {
        Optional<User> found = userRepo.findById(id);
        if (found.isEmpty()) {
            return null;
        }
        UserDTO dto = modelMapper.map(found.get(), UserDTO.class);
        dto.setPassword(null);
        decryptEmail(dto);
        return dto;
    }

    /**
     * Verifica si existe un usuario con el username dado (texto plano).
     *
     * @param username El username (texto plano).
     * @return {@code true} si el username ya está registrado.
     */
    public boolean usernameExists(String username) {
        return userRepo.existsByUsername(username);
    }

    /**
     * Resuelve el identificador del usuario (username o email) y retorna su
     * username en texto plano, listo para entregar al AuthenticationManager.
     * <p>
     * Acepta como entrada texto plano sin saber si es un username o un email,
     * y prueba ambos formatos contra la BD. Si alguno calza, retorna el
     * username correspondiente.
     * </p>
     *
     * @param identifier Texto plano que puede ser username o email.
     * @return El username del usuario si se encontró, o {@code null} si ningún
     *         usuario coincide.
     */
    public String resolveIdentifierToEncryptedUsername(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }

        // Probar primero como username (texto plano).
        Optional<User> byUsername = userRepo.findByUsername(identifier);
        if (byUsername.isPresent()) {
            return byUsername.get().getUsername();
        }
        // Si no calzó, probar como email (encriptado para la búsqueda).
        Optional<User> byEmail = userRepo.findByEmail(AESUtil.encrypt(identifier));
        if (byEmail.isPresent()) {
            return byEmail.get().getUsername();
        }
        return null;
    }

    // =========================================================================
    // Verificación de cuenta y recuperación de contraseña
    // =========================================================================

    /**
     * Valida el código de verificación recibido durante el flujo de registro.
     * Si el código coincide, activa la cuenta del usuario y consume el código
     * eliminándolo de la tabla {@link VerificationCode}.
     *
     * @param email  Email del usuario que se registró.
     * @param codigo Código de 6 dígitos en texto plano enviado por el usuario.
     * @return 0 si el código es válido y la cuenta se activó;
     *         7 si el código no coincide;
     *         2 si no existe el email.
     */
    @Transactional
    public int verifyRegistrationCode(String email, String codigo) {
        String encryptedEmail = AESUtil.encrypt(email);
        Optional<User> foundUser = userRepo.findByEmail(encryptedEmail);
        if (foundUser.isEmpty()) {
            return 2;
        }
        if (!checkCode(encryptedEmail, codigo, Purpose.REGISTRATION)) {
            return 7;
        }
        // Consume el código y activa la cuenta.
        codeRepo.deleteByEmailAndPurpose(encryptedEmail, Purpose.REGISTRATION);
        User entity = foundUser.get();
        entity.setEnabled(true);
        userRepo.save(entity);
        return 0;
    }

    /**
     * Genera y envía un código de 6 dígitos al correo del usuario para
     * iniciar el flujo de recuperación de contraseña. El código se persiste
     * encriptado en {@link VerificationCode} con propósito
     * {@link Purpose#PASSWORD_RECOVERY}.
     *
     * @param email Email del usuario que solicita la recuperación.
     * @return 0 si el código se generó y envió; 2 si no existe el email.
     */
    @Transactional
    public int requestRecoveryCode(String email) {
        String encryptedEmail = AESUtil.encrypt(email);
        Optional<User> found = userRepo.findByEmail(encryptedEmail);
        if (found.isEmpty()) {
            return 2;
        }
        String codigoPlano = EmailService.generarCodigo6Digitos();
        issueCode(encryptedEmail, codigoPlano, Purpose.PASSWORD_RECOVERY);
        // Recuperamos el nombre del usuario (en texto plano) para personalizar el saludo del correo.
        String nombrePlano = found.get().getName();
        emailService.enviarCodigoRecuperacion(email, nombrePlano, codigoPlano);
        return 0;
    }

    /**
     * Valida que el código de recuperación coincida con el que está guardado
     * para el email indicado, SIN consumirlo. Permite que el frontend confirme
     * la validez del código en un paso intermedio antes de pedir la nueva
     * contraseña.
     *
     * @param email  Email del usuario.
     * @param codigo Código de 6 dígitos que el usuario ingresó.
     * @return 0 si el código es válido; 7 si no coincide; 2 si no existe el email.
     */
    public int validateRecoveryCode(String email, String codigo) {
        String encryptedEmail = AESUtil.encrypt(email);
        Optional<User> found = userRepo.findByEmail(encryptedEmail);
        if (found.isEmpty()) {
            return 2;
        }
        if (!checkCode(encryptedEmail, codigo, Purpose.PASSWORD_RECOVERY)) {
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
    @Transactional
    public int resetPasswordWithCode(String email, String codigo, String nuevaContrasena) {
        if (!isValidPassword(nuevaContrasena)) {
            return 4;
        }
        String encryptedEmail = AESUtil.encrypt(email);
        Optional<User> found = userRepo.findByEmail(encryptedEmail);
        if (found.isEmpty()) {
            return 2;
        }
        if (!checkCode(encryptedEmail, codigo, Purpose.PASSWORD_RECOVERY)) {
            return 7;
        }
        User entity = found.get();
        entity.setPassword(passwordEncoder.encode(nuevaContrasena));
        userRepo.save(entity);
        codeRepo.deleteByEmailAndPurpose(encryptedEmail, Purpose.PASSWORD_RECOVERY);
        return 0;
    }

    // =========================================================================
    // Helpers privados de códigos
    // =========================================================================

    /**
     * Emite un código nuevo para un email y propósito, eliminando primero
     * cualquier código anterior del mismo propósito para asegurar unicidad.
     *
     * @param encryptedEmail Email encriptado del usuario.
     * @param plainCode      Código de 6 dígitos en texto plano.
     * @param purpose        Propósito del código.
     */
    private void issueCode(String encryptedEmail, String plainCode, Purpose purpose) {
        codeRepo.deleteByEmailAndPurpose(encryptedEmail, purpose);
        VerificationCode entry = new VerificationCode(
                encryptedEmail, AESUtil.encrypt(plainCode), purpose);
        codeRepo.save(entry);
    }

    /**
     * Verifica si el código en texto plano coincide con el guardado para un
     * email y propósito específico.
     *
     * @param encryptedEmail Email encriptado del usuario.
     * @param plainCode      Código en texto plano a verificar.
     * @param purpose        Propósito a comparar.
     * @return {@code true} si el código coincide; {@code false} si no existe
     *         o no coincide.
     */
    private boolean checkCode(String encryptedEmail, String plainCode, Purpose purpose) {
        Optional<VerificationCode> stored = codeRepo.findByEmailAndPurpose(encryptedEmail, purpose);
        if (stored.isEmpty()) {
            return false;
        }
        String decrypted = AESUtil.decrypt(stored.get().getCode());
        return plainCode.equals(decrypted);
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
     * Desencripta el campo email de un {@link UserDTO} en el lugar.
     * No toca password (siempre nulo en respuestas) ni los demás campos
     * que ya están en texto plano.
     *
     * @param dto El DTO cuyo email se va a desencriptar.
     */
    private void decryptEmail(UserDTO dto) {
        if (dto.getEmail() != null) {
            dto.setEmail(AESUtil.decrypt(dto.getEmail()));
        }
    }
}