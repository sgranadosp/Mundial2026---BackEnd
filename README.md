# ⚙️ Mundial 2026 — Backend

API REST del proyecto **Mundial 2026 Hub**, desarrollada con Spring Boot 3. Provee todos los servicios necesarios para la gestión de usuarios, partidos, predicciones, álbum de láminas, entradas, notificaciones push y más.

---

## 🛠️ Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.2.2 | Framework base |
| Spring Security | 6 | Seguridad y autenticación |
| JWT (jjwt) | 0.11.5 | Tokens de autenticación |
| Spring Data JPA / Hibernate | — | ORM y persistencia |
| MySQL | — | Base de datos relacional |
| HikariCP | — | Pool de conexiones |
| Firebase Admin SDK | 9.2.0 | Push notifications (FCM) |
| MercadoPago SDK | 2.8.0 | Pasarela de pagos |
| Spring Mail | — | Envío de correos (Gmail SMTP) |
| SpringDoc OpenAPI | 2.3.0 | Documentación Swagger |
| ModelMapper | 3.1.1 | Mapeo DTO ↔ Entidad |
| Gson | — | Serialización JSON |
| Maven | — | Gestión de dependencias y build |

---

## 📁 Estructura del proyecto

```
mundial2026/
└── src/main/java/co/edu/unbosque/mundial2026/
    ├── configuration/       # Beans de configuración (Firebase, Cache, CORS, OpenAPI, etc.)
    ├── controller/          # Controladores REST
    ├── dto/                 # Data Transfer Objects
    │   ├── auth/            # DTOs de autenticación
    │   ├── album/           # DTOs del álbum de láminas
    │   ├── pack/            # DTOs de packs de láminas
    │   └── trade/           # DTOs de intercambios
    ├── exception/           # Excepciones personalizadas y handler global
    ├── model/               # Entidades JPA
    ├── repository/          # Interfaces Spring Data JPA
    ├── security/            # SecurityConfig, JwtAuthenticationFilter
    ├── service/             # Lógica de negocio
    └── util/                # Utilidades (AES, Scoring, DateTime, etc.)
laminas/                     # 294 imágenes PNG de láminas del álbum
```

---

## 🔐 Seguridad

La autenticación es **stateless** mediante JWT.

- Las contraseñas se encriptan con **BCrypt**.
- Cada request protegido debe incluir el header:
  ```
  Authorization: Bearer <token>
  ```
- El filtro `JwtAuthenticationFilter` intercepta cada petición, valida el token y establece el contexto de seguridad.
- Control de acceso por roles: `ROLE_USER` y `ROLE_ADMIN`.

### Rutas públicas (sin autenticación)
- Todo el contexto `/api/auth/**`
- Documentación Swagger: `/api/`

---

## 🗂️ Modelos de datos (Entidades)

| Entidad | Descripción |
|---|---|
| `User` | Usuario de la plataforma |
| `VerificationCode` | Código de verificación de email / recuperación |
| `Match` | Partido del mundial |
| `Team` | Selección participante |
| `Stadium` | Estadio sede |
| `Prediction` | Predicción de un usuario sobre un partido |
| `PollGroup` | Grupo de predicciones (polla) |
| `Ticket` | Entrada a un partido |
| `StickerPack` | Pack de láminas |
| `Sticker` | Lámina del álbum |
| `UserSticker` | Relación usuario ↔ lámina (colección) |
| `TradeRequest` | Solicitud de intercambio de láminas |
| `TradeRejection` | Registro de rechazo de intercambio |
| `NotificationInbox` | Notificación recibida por el usuario |
| `AuditEvent` | Evento de auditoría del sistema |
| `SupportTicket` | Ticket de soporte al usuario |

---

## 🌐 Endpoints principales

La API está documentada en Swagger. Una vez levantado el servidor, accede a:

```
http://localhost:9090/api/
```

### Resumen de controladores

| Controlador | Prefijo | Descripción |
|---|---|---|
| `AuthController` | `/auth` | Registro, login, verificación, recuperación de contraseña |
| `UserController` | `/users` | CRUD de usuarios, perfil |
| `MatchController` | `/matches` | Partidos, resultados, sincronización |
| `TeamController` | `/teams` | Selecciones participantes |
| `StadiumController` | `/stadiums` | Estadios sede |
| `PollController` | `/polls` | Grupos de predicciones (pollas) |
| `RankingController` | `/ranking` | Ranking de usuarios en pollas |
| `TicketController` | `/tickets` | Entradas a partidos |
| `PaymentController` | `/payments` | Integración MercadoPago |
| `AlbumController` | `/album` | Álbum de láminas del usuario |
| `PackController` | `/packs` | Apertura y gestión de packs |
| `TradeController` | `/trades` | Solicitudes de intercambio de láminas |
| `NotificationController` | `/notifications` | Envío y gestión de notificaciones push |
| `FcmTokenController` | `/fcm` | Registro de tokens FCM por dispositivo |
| `SupportTicketController` | `/support` | Tickets de soporte |
| `AuditController` | `/audit` | Logs de auditoría (solo ADMIN) |
| `DashboardController` | `/dashboard` | Resumen y métricas para el panel admin |
| `AdminSyncController` | `/admin/sync` | Sincronización manual de datos externos |

---

## ⚙️ Instalación y ejecución

### Requisitos previos
- Java 21
- Maven 3.9+
- MySQL 8+

### Pasos

```bash
# 1. Clonar el repositorio
git clone <url-del-repo>
cd Mundial2026---BackEnd/mundial2026

# 2. Configurar la base de datos
# Crear la base de datos en MySQL:
CREATE DATABASE `mundial2026-BD`;

# 3. Ajustar las propiedades de conexión en:
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://<host>:3306/mundial2026-BD?...
spring.datasource.username=<usuario>
spring.datasource.password=<contraseña>

# 4. Compilar y ejecutar
./mvnw spring-boot:run
```

El servidor arranca en: `http://localhost:9090/api`

### Build para producción (WAR)

```bash
./mvnw clean package
```

El archivo `.war` se genera en `target/` y puede desplegarse en Tomcat.

---

## 🔧 Variables de configuración (`application.properties`)

| Propiedad | Descripción |
|---|---|
| `spring.datasource.url` | URL de conexión a MySQL |
| `spring.datasource.username` | Usuario de la BD |
| `spring.datasource.password` | Contraseña de la BD |
| `jwt.secret` | Clave secreta para firmar los JWT |
| `jwt.expiration` | Tiempo de expiración del token en ms (por defecto 86400000 = 24h) |
| `server.port` | Puerto del servidor (por defecto `9090`) |
| `server.servlet.context-path` | Contexto base de la API (`/api`) |
| `spring.mail.username` | Correo remitente (Gmail) |
| `spring.mail.password` | App password de Gmail |
| `album.laminas.path` | Ruta del directorio con los PNG de láminas |
| `apifootball.key` | API key de API-Football para sincronización de datos |

> ⚠️ **Seguridad**: antes de desplegar en producción, mueve las credenciales sensibles (JWT secret, contraseñas, API keys) a variables de entorno o un gestor de secretos. No subas `application.properties` con valores reales al repositorio.

---

## 🔄 Sincronización de datos de partidos

El backend consume datos de partidos desde fuentes externas:

- **API-Football** (`FootballApiClient`, `FootballApiSyncService`): sincronización de resultados y fixture del mundial.
- **OpenFootball** (`OpenFootballClient`, `OpenFootballSyncService`): fuente alternativa de datos.
- **`MatchReminderService`**: servicio programado que envía recordatorios antes del inicio de cada partido.

La sincronización manual puede dispararse desde el panel admin a través de `AdminSyncController`.

---

## 📬 Notificaciones Push

Se usa **Firebase Cloud Messaging (FCM)** a través del Firebase Admin SDK.

- Los tokens de dispositivo se registran mediante `FcmTokenController`.
- `NotificationService` gestiona el envío masivo o individual de notificaciones.
- `NotificationInboxService` mantiene el historial de notificaciones por usuario.

Para configurar Firebase, coloca el archivo `serviceAccountKey.json` de tu proyecto de Firebase en la ruta que espera `FirebaseConfig.java`.

---

## 💳 Pagos con MercadoPago

`PaymentController` y `PaymentService` integran el SDK de MercadoPago para procesar la compra de entradas a los partidos. El flujo genera una preferencia de pago y redirige al checkout de MercadoPago.

---

## 🔍 Documentación Swagger

Una vez levantada la aplicación, la documentación interactiva de todos los endpoints está disponible en:

```
http://localhost:9090/api/
```

Generada automáticamente con **SpringDoc OpenAPI 2.3.0**.

---

## 🏛️ Arquitectura

```
Cliente (Frontend React)
        │
        ▼
   API REST (Spring Boot)
        │
        ├── Spring Security (JWT filter)
        ├── Controllers → Services → Repositories
        ├── Firebase Admin SDK (push)
        ├── MercadoPago SDK (pagos)
        ├── Spring Mail (email)
        └── MySQL (Google Cloud SQL)
```

---

## 📌 Consideraciones

- El proyecto está configurado para desplegarse como archivo WAR sobre Tomcat, pero también puede correr embebido con `spring-boot:run`.
- La base de datos usada en el entorno original es **Google Cloud SQL** (MySQL 8).
- La estrategia de Hibernate es `ddl-auto=update`: crea o actualiza tablas automáticamente al iniciar.
- El álbum de láminas requiere que el directorio `laminas/` con los 294 PNG esté disponible en la ruta configurada en `album.laminas.path`.
