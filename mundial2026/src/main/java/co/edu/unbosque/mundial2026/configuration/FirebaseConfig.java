/**
 * Paquete con clases de configuración de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Configuración de Firebase Admin SDK para el backend.
 * <p>
 * Inicializa Firebase con las credenciales del service account al arranque
 * de la aplicación y expone {@link FirebaseMessaging} como bean para que
 * los servicios puedan inyectarlo y enviar notificaciones push.
 * </p>
 * <p>
 * El archivo del service account debe estar en {@code src/main/resources}
 * con el nombre {@code firebase-service-account.json} (path configurado en
 * {@code application.properties} como {@code firebase.service-account-path}).
 * </p>
 * <p>
 * <b>Seguridad:</b> el JSON del service account contiene una clave privada
 * que permite enviar push notifications a cualquier usuario y acceder a
 * otros recursos de Firebase. NUNCA debe subirse al repositorio público;
 * agregarlo a {@code .gitignore}.
 * </p>
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    /** Ruta al archivo de credenciales, inyectada desde application.properties. */
    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    private final ResourceLoader resourceLoader;

    /**
     * Constructor con inyección del {@link ResourceLoader} de Spring para
     * poder leer archivos del classpath (donde está el JSON).
     */
    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Inicializa la instancia única de {@link FirebaseApp} a partir del
     * service account. Si ya hay una app inicializada (caso de hot reload
     * con DevTools), la reutiliza para evitar el error
     * {@code FirebaseApp name [DEFAULT] already exists}.
     *
     * @return La instancia única de FirebaseApp.
     * @throws IOException si no se puede leer el archivo de credenciales.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        Resource resource = resourceLoader.getResource(serviceAccountPath);
        if (!resource.exists()) {
            log.error("[firebase] Service account no encontrado en {}", serviceAccountPath);
            throw new IOException("Service account JSON no encontrado en " + serviceAccountPath
                    + ". Coloca el archivo en src/main/resources/firebase-service-account.json");
        }

        try (InputStream stream = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("[firebase] Inicializado correctamente. Project: {}",
                    app.getOptions().getProjectId());
            return app;
        }
    }

    /**
     * Expone {@link FirebaseMessaging} como bean inyectable.
     *
     * @param firebaseApp La FirebaseApp inicializada arriba.
     * @return Cliente de FCM listo para enviar push notifications.
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}