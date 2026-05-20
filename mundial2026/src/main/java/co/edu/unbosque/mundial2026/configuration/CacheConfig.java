/**
 * Paquete que contiene las clases de configuración de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import java.util.Arrays;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración que habilita y configura el caché en memoria
 * para la plataforma Mundial 2026 Hub.
 * <p>
 * El caché es especialmente importante en este proyecto porque la API externa
 * de datos deportivos (football-data.org, API-Football o WireMock) tiene
 * límites de cuota en sus planes gratuitos. Cachear las respuestas evita
 * llamadas redundantes al proveedor y garantiza que el sistema siga mostrando
 * datos aunque la API externa esté temporalmente no disponible (degradación
 * elegante requerida por el enunciado).
 * </p>
 * <p>
 * En esta implementación se usa {@link SimpleCacheManager} con
 * {@link ConcurrentMapCache} (caché en memoria, sin dependencias externas)
 * adecuada para el entorno académico del proyecto. En producción se
 * reemplazaría por Redis con TTL configurado.
 * </p>
 *
 * <h3>Cachés disponibles</h3>
 * <ul>
 *   <li>{@code matches} — Respuestas de fixtures y marcadores de la API externa.</li>
 *   <li>{@code teams} — Datos de selecciones nacionales.</li>
 *   <li>{@code stadiums} — Datos de estadios sede.</li>
 *   <li>{@code rankings} — Rankings calculados de grupos de polla.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Define el {@link CacheManager} con los cachés en memoria usados
     * por la aplicación.
     * <p>
     * Para invalidar manualmente un caché durante el desarrollo se puede
     * llamar a {@code cacheManager.getCache("matches").clear()} desde
     * cualquier componente que tenga el {@link CacheManager} inyectado.
     * </p>
     *
     * @return El {@link CacheManager} configurado con todos los cachés
     *         de la plataforma.
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("matches"),
                new ConcurrentMapCache("teams"),
                new ConcurrentMapCache("stadiums"),
                new ConcurrentMapCache("rankings")
        ));
        return cacheManager;
    }
}