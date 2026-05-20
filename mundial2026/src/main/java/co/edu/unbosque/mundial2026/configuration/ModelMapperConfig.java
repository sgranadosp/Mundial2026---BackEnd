/**
 * Paquete que contiene las clases de configuración de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración que expone el bean {@link ModelMapper} para la
 * conversión entre entidades JPA y DTOs en la plataforma Mundial 2026 Hub.
 * <p>
 * {@link ModelMapper} se usa en los servicios para mapear campos entre
 * la entidad y su DTO correspondiente sin necesidad de setters manuales
 * campo por campo. Se configura con la estrategia {@code STRICT} para
 * evitar mapeos ambiguos que puedan sobrescribir campos no deseados,
 * especialmente relevante en entidades como {@code User} donde hay campos
 * de Spring Security ({@code username}, {@code password}) que no deben
 * mapearse de forma accidental desde el DTO.
 * </p>
 */
@Configuration
public class ModelMapperConfig {

    /**
     * Define el bean {@link ModelMapper} compartido en toda la aplicación.
     * <p>
     * La estrategia {@link MatchingStrategies#STRICT} garantiza que solo
     * se mapeen campos cuyos nombres coincidan exactamente entre el objeto
     * origen y el destino, reduciendo el riesgo de mapeos no intencionales.
     * </p>
     *
     * @return Una instancia configurada de {@link ModelMapper} lista para
     *         ser inyectada en los servicios vía {@code @Autowired}.
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
              .setMatchingStrategy(MatchingStrategies.STRICT)
              .setSkipNullEnabled(true)
              .setFieldMatchingEnabled(true)
              .setFieldAccessLevel(
                      org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
        return mapper;
    }
}