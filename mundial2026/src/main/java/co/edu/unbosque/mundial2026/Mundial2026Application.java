package co.edu.unbosque.mundial2026;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación Mundial 2026 Hub.
 * <p>
 * Habilitamos {@code @EnableScheduling} para que el job de
 * {@code MatchReminderService} (que notifica partidos próximos) se ejecute
 * periódicamente.
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class Mundial2026Application {

	public static void main(String[] args) {
		SpringApplication.run(Mundial2026Application.class, args);
	}

}