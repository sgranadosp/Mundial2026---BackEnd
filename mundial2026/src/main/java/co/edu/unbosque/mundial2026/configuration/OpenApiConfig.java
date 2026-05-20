/**
 * Paquete que contiene las clases de configuración de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Clase de configuración para la documentación interactiva de la API REST
 * de la plataforma Mundial 2026 Hub usando OpenAPI 3 / Swagger UI.
 * <p>
 * Sigue el mismo patrón del proyecto VirusDetected. Define el bean
 * {@link OpenAPI} con la información general de la API, el esquema de
 * seguridad JWT Bearer y las respuestas de error globales reutilizables.
 * La documentación queda disponible en {@code /swagger-ui/index.html} y
 * la spec en {@code /v3/api-docs}.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Define y configura el objeto {@link OpenAPI} con toda la información de la
     * API de Mundial 2026 Hub: título, versión, descripción del flujo de uso,
     * datos de contacto, esquema de seguridad JWT Bearer y respuestas de error
     * globales estandarizadas.
     *
     * @return El objeto {@link OpenAPI} completamente configurado para Swagger UI.
     */
    @Bean
    public OpenAPI customOpenAPI() {

        String mainDescription =
                "<h2>Mundial 2026 Hub — API REST</h2>"
                + "<p>Plataforma web y móvil para la experiencia digital del "
                + "Mundial de Fútbol 2026. Cubre seguimiento de partidos, "
                + "pollas futboleras, álbum digital de láminas y gestión de entradas.</p>"
                + "<h3>Módulos disponibles</h3>"
                + "<ul>"
                + "<li><strong>/auth</strong> — Registro, login y recuperación de contraseña (público)</li>"
                + "<li><strong>/users</strong> — Perfil de usuario y administración de cuentas</li>"
                + "<li><strong>/matches</strong> — Consulta de partidos, filtros y resultados</li>"
                + "<li><strong>/polls</strong> — Grupos de polla, pronósticos y ranking</li>"
                + "<li><strong>/album</strong> — Álbum digital, paquetes e intercambios de láminas</li>"
                + "<li><strong>/tickets</strong> — Entradas con ciclo de vida completo (sandbox)</li>"
                + "<li><strong>/ranking</strong> — Clasificaciones y estadísticas personales</li>"
                + "<li><strong>/notifications</strong> — Notificaciones push y email (solo ADMIN)</li>"
                + "<li><strong>/admin/audit</strong> — Panel de trazabilidad y auditoría (solo ADMIN)</li>"
                + "</ul>"
                + "<h3>Flujo básico de uso</h3>"
                + "<ol>"
                + "<li>Regístrate con <code>POST /auth/register</code></li>"
                + "<li>Inicia sesión con <code>POST /auth/login</code> y copia el token JWT</li>"
                + "<li>Haz clic en <strong>Authorize</strong> e ingresa: <code>Bearer &lt;tu_token&gt;</code></li>"
                + "<li>Usa los endpoints con el token activo</li>"
                + "</ol>"
                + "<h3>Roles del sistema</h3>"
                + "<ul>"
                + "<li><strong>USER</strong> — Aficionado registrado. Accede a partidos, pollas, álbum y entradas.</li>"
                + "<li><strong>ADMIN</strong> — Operador. Accede a todo lo anterior más administración, "
                + "notificaciones masivas y auditoría.</li>"
                + "</ul>"
                + "<h3>Credenciales de prueba (datos semilla)</h3>"
                + "<ul>"
                + "<li><code>admin</code> / <code>Admin2026!</code> — rol ADMIN</li>"
                + "<li><code>testuser</code> / <code>Test2026!</code> — rol USER</li>"
                + "</ul>"
                + "<h3>Política de contraseñas</h3>"
                + "<p>Mínimo 8 caracteres, al menos una mayúscula, una minúscula, "
                + "un número y un símbolo especial (no &lt; &gt; :).</p>"
                + "<h3>Códigos HTTP usados</h3>"
                + "<ul>"
                + "<li><strong>201</strong> — Recurso creado</li>"
                + "<li><strong>202</strong> — Operación exitosa con datos</li>"
                + "<li><strong>204</strong> — Operación exitosa sin datos</li>"
                + "<li><strong>400</strong> — Datos inválidos</li>"
                + "<li><strong>401</strong> — No autenticado o token expirado</li>"
                + "<li><strong>403</strong> — Sin permisos o estado no permite la operación</li>"
                + "<li><strong>404</strong> — Recurso no encontrado</li>"
                + "<li><strong>409</strong> — Conflicto (duplicado o límite de negocio)</li>"
                + "<li><strong>500</strong> — Error de servicio externo</li>"
                + "</ul>";

        String securityDescription =
                "Autenticación mediante JWT (JSON Web Token) con validez de 8 horas."
                + "<p>Para autenticarte:</p>"
                + "<ol>"
                + "<li>Llama a <code>POST /auth/login</code> con tu username y password</li>"
                + "<li>Copia el campo <code>token</code> de la respuesta</li>"
                + "<li>Haz clic en el botón <strong>Authorize</strong> en la parte superior</li>"
                + "<li>Escribe: <code>Bearer &lt;tu_token_jwt&gt;</code></li>"
                + "<li>Haz clic en <strong>Authorize</strong> y luego en <strong>Close</strong></li>"
                + "</ol>"
                + "<p>El token incluye tu rol (USER o ADMIN) y se valida automáticamente "
                + "en cada petición a endpoints protegidos.</p>";

        Info info = new Info()
                .title("Mundial 2026 Hub — API REST")
                .version("1.0.0")
                .description(mainDescription)
                .contact(new Contact()
                        .name("Equipo de Desarrollo — Universidad El Bosque")
                        .email("soporte@mundial2026hub.com")
                        .url("https://github.com/unbosque/mundial2026hub"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(securityDescription);

        return new OpenAPI()
                .info(info)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme)
                        .addResponses("UnauthorizedError",
                                new ApiResponse()
                                        .description("No autenticado — token JWT inválido o expirado")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().addExamples("error",
                                                        new Example().value(
                                                                "{\"message\": \"Credenciales incorrectas\", \"success\": false}")))))
                        .addResponses("ForbiddenError",
                                new ApiResponse()
                                        .description("Acceso prohibido — sin permisos para esta operación")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().addExamples("error",
                                                        new Example().value(
                                                                "{\"message\": \"No tienes permiso para esta acción\", \"success\": false}")))))
                        .addResponses("NotFoundError",
                                new ApiResponse()
                                        .description("Recurso no encontrado")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().addExamples("error",
                                                        new Example().value(
                                                                "{\"message\": \"Recurso no encontrado\", \"success\": false}")))))
                        .addResponses("ConflictError",
                                new ApiResponse()
                                        .description("Conflicto — dato duplicado o restricción de negocio violada")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().addExamples("error",
                                                        new Example().value(
                                                                "{\"message\": \"El username o email ya están en uso\", \"success\": false}")))))
                        .addResponses("BadRequestError",
                                new ApiResponse()
                                        .description("Datos inválidos o error en la solicitud")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().addExamples("error",
                                                        new Example().value(
                                                                "{\"message\": \"Error al procesar la solicitud\", \"success\": false}"))))));
    }
}