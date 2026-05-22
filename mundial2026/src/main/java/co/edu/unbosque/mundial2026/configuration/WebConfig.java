/**
 * Paquete que contiene las clases de configuración de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC para exponer recursos estáticos externos
 * al JAR. Mapea la carpeta del filesystem que contiene las láminas del
 * álbum al endpoint público {@code /laminas/**}, permitiendo que cada
 * archivo se sirva como {@code GET /api/laminas/<archivo>.png}.
 * <p>
 * La ruta física se configura desde {@code application.properties} con la
 * propiedad {@code album.laminas.path}. Por defecto apunta a {@code ./laminas/}
 * (relativo al directorio de trabajo donde se arranca el back), de modo que
 * en el repo deben crearse las láminas en {@code mundial2026/laminas/}.
 * </p>
 * <p>
 * <b>Caché:</b> los archivos se sirven con un {@code Cache-Control} de
 * 30 días porque son recursos estáticos que rara vez cambian; el navegador
 * los reutiliza sin volver a pedirlos, reduciendo tráfico hacia el back.
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Ruta del sistema de archivos donde viven las láminas (con barra final).
     */
    @Value("${album.laminas.path:./laminas/}")
    private String laminasPath;

    /**
     * Registra el handler que mapea las URLs {@code /laminas/**} a archivos
     * físicos en la ruta configurada.
     *
     * @param registry Registro de handlers de recursos estáticos de Spring. 
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/laminas/**")
                .addResourceLocations("file:" + laminasPath)
                .setCachePeriod(2_592_000); // 30 días
    }
}