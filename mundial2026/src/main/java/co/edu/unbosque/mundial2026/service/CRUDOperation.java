/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.util.List;

/**
 * Interfaz genérica que define el contrato de operaciones CRUD básicas para
 * los servicios de la aplicación Mundial 2026 Hub.
 * Sigue el mismo patrón del proyecto de referencia VirusDetected. Todos los
 * servicios que gestionan entidades con datos sensibles implementan esta
 * interfaz, garantizando un contrato común y facilitando las pruebas unitarias
 * mediante mocking.
 *
 * @param <T> El tipo del DTO (Data Transfer Object) usado para transferir datos.
 * @param <E> El tipo de la entidad JPA gestionada por el servicio.
 */
public interface CRUDOperation<T, E> {

    /**
     * Crea una nueva entidad a partir de un DTO.
     *
     * @param data El DTO con los datos para crear la entidad.
     * @return Código de resultado: 0 = éxito, valores positivos = fallo con motivo.
     */
    int create(T data);

    /**
     * Obtiene todos los registros de la entidad como lista de DTOs.
     *
     * @return Lista de DTOs con todos los registros.
     */
    List<T> getAll();

    /**
     * Elimina una entidad por su ID.
     *
     * @param id El ID de la entidad a eliminar.
     * @return Código de resultado: 0 = éxito, 1 = no encontrado.
     */
    int deleteById(Long id);

    /**
     * Actualiza una entidad existente por su ID.
     *
     * @param id      El ID de la entidad a actualizar.
     * @param newData El DTO con los nuevos datos.
     * @return Código de resultado: 0 = éxito, valores positivos = fallo con motivo.
     */
    int updateById(Long id, T newData);

    /**
     * Cuenta el número total de registros de la entidad.
     *
     * @return El número total de registros.
     */
    long count();

    /**
     * Verifica si existe una entidad con el ID especificado.
     *
     * @param id El ID a verificar.
     * @return {@code true} si existe la entidad.
     */
    boolean exist(Long id);
}