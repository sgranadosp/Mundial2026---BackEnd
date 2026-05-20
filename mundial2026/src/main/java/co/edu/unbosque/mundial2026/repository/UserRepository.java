/**
 * Paquete que contiene las interfaces de repositorio utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.model.User.Role;

/**
 * Interfaz de repositorio para la entidad {@link User}.
 * Extiende {@link JpaRepository} para proveer operaciones CRUD estándar
 * sobre la tabla {@code users}. Los métodos personalizados permiten buscar
 * usuarios por los campos únicos usados en autenticación y en validaciones
 * de unicidad durante el registro. Todos los campos sensibles (username,
 * email, name) se almacenan encriptados con AES en la base de datos, por lo
 * que las búsquedas deben recibir el valor ya encriptado antes de consultar.
 * Spring Data JPA genera las implementaciones en tiempo de ejecución
 * a partir de la convención de nombres de los métodos.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su nombre de usuario (encriptado con AES).
     * Se usa en el proceso de autenticación JWT y en validaciones de unicidad.
     *
     * @param username El nombre de usuario encriptado a buscar.
     * @return Un {@link Optional} con el usuario encontrado, o vacío si no existe.
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca un usuario por su dirección de correo electrónico (encriptada con AES).
     * Se usa en validaciones de unicidad durante el registro y en recuperación
     * de contraseña.
     *
     * @param email El correo electrónico encriptado a buscar.
     * @return Un {@link Optional} con el usuario encontrado, o vacío si no existe.
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por su nombre completo (encriptado con AES).
     * Se usa en validaciones de unicidad durante el registro.
     *
     * @param name El nombre completo encriptado a buscar.
     * @return Un {@link Optional} con el usuario encontrado, o vacío si no existe.
     */
    Optional<User> findByName(String name);

    /**
     * Busca todos los usuarios que tengan un rol específico.
     * Se usa en el panel de administración para listar usuarios por rol
     * (HU16 — Ver lista de usuarios).
     *
     * @param role El rol por el que se filtra.
     * @return Lista de usuarios con el rol especificado.
     */
    List<User> findByRole(Role role);

    /**
     * Busca todos los usuarios cuya cuenta está habilitada o deshabilitada.
     * Se usa en el panel de administración para filtrar cuentas activas
     * o bloqueadas (HU17, HU18 — Bloquear / Desbloquear usuario).
     *
     * @param enabled {@code true} para cuentas habilitadas, {@code false} para bloqueadas.
     * @return Lista de usuarios con el estado indicado.
     */
    List<User> findByEnabled(boolean enabled);

    /**
     * Busca todos los usuarios cuya cuenta no está bloqueada según el flag
     * {@code accountNonLocked}. Se usa para listar cuentas en estado normal
     * o bloqueado independientemente del flag {@code enabled}.
     *
     * @param accountNonLocked {@code true} para cuentas no bloqueadas,
     *                         {@code false} para bloqueadas.
     * @return Lista de usuarios con el estado de bloqueo indicado.
     */
    List<User> findByAccountNonLocked(boolean accountNonLocked);

    /**
     * Comprueba si existe un usuario con el nombre de usuario dado (encriptado).
     * Más eficiente que {@code findByUsername} cuando solo se necesita verificar
     * existencia, sin cargar el objeto completo.
     *
     * @param username El nombre de usuario encriptado a verificar.
     * @return {@code true} si existe al menos un usuario con ese username.
     */
    boolean existsByUsername(String username);

    /**
     * Comprueba si existe un usuario con el correo electrónico dado (encriptado).
     * Se usa en la validación de unicidad de email durante el registro.
     *
     * @param email El correo electrónico encriptado a verificar.
     * @return {@code true} si existe al menos un usuario con ese email.
     */
    boolean existsByEmail(String email);

    /**
     * Elimina un usuario por su nombre de usuario (encriptado).
     * Operación disponible para administradores (HU19 — Eliminar usuario).
     *
     * @param username El nombre de usuario encriptado del usuario a eliminar.
     */
    void deleteByUsername(String username);
}