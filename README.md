# El Recetario de Shir - Back End

Backend de **El Recetario de Shir**, construido como una aplicación Spring Boot independiente del frontend.

## Stack actual

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring Data JPA
- Spring Web
- Spring Security
- Bean Validation
- MySQL
- Flyway
- JUnit
- Git

## Configuración de base de datos

La aplicación obtiene la conexión mediante variables de entorno:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `MEDIA_STORAGE_ROOT`

Para crear una cuenta Admin inicial de forma explícita:

- `ADMIN_BOOTSTRAP_ENABLED=true`
- `ADMIN_BOOTSTRAP_USERNAME`
- `ADMIN_BOOTSTRAP_PASSWORD`

El bootstrap está deshabilitado por defecto, almacena únicamente el hash BCrypt y no modifica una cuenta que ya exista. Puede ejecutarse de forma controlada para crear inicialmente las cuentas necesarias, cambiando las variables entre ejecuciones y deshabilitándolo después.

`MEDIA_STORAGE_ROOT` debe apuntar a un directorio privado y persistente fuera del repositorio. El backend crea el directorio si no existe y falla al iniciar si la configuración es inválida o el directorio no es utilizable.

No se versionan credenciales reales ni contenido multimedia.

Flyway es la fuente de verdad del esquema de base de datos. Hibernate está configurado para validar el esquema, no para crearlo ni modificarlo.

## Ejecutar el proyecto

Con las variables de entorno configuradas:

    ./mvnw spring-boot:run

Para ejecutar las pruebas de integración contra MySQL:

    ./mvnw test

### Import legacy de recetas

El importer legacy no se ejecuta durante un inicio normal. Requiere activación explícita mediante el profile `legacy-import` y la propiedad habilitadora.

Variables requeridas:

- `LEGACY_RECIPE_IMPORT_ENABLED=true`
- `LEGACY_RECIPE_JSON`
- `LEGACY_RECIPE_ASSETS_ROOT`
- `MEDIA_STORAGE_ROOT`
- variables de conexión MySQL habituales

Ejemplo:

    SPRING_PROFILES_ACTIVE=legacy-import \
    LEGACY_RECIPE_IMPORT_ENABLED=true \
    LEGACY_RECIPE_JSON=/ruta/recipes.json \
    LEGACY_RECIPE_ASSETS_ROOT=/ruta/assets \
    ./mvnw spring-boot:run

La primera ejecución importa las 26 recetas si la base no contiene estado legacy previo. Una segunda ejecución válida termina como `NO_OP`. Los estados parciales o incompatibles abortan el import.

## Estado actual

Persistence foundation completada con:

- datasource externo para MySQL;
- Spring Data JPA;
- Flyway como fuente de verdad del esquema;
- Hibernate configurado con `ddl-auto=validate`;
- modelo persistente `Recipe`, `RecipeIngredient`, `RecipeStep` y `RecipeImage`;
- enums de dominio para estado, categoría, tipo y dificultad;
- migraciones V1, V2 y V3 aplicadas;
- relaciones, orden persistente, timestamps y constraints validados contra MySQL real;
- foundation de almacenamiento multimedia privado local mediante `MediaStorageService`;
- storage keys opacos generados por backend y protección contra acceso fuera del storage root;
- importer legacy explícito para las 26 recetas, con preflight, idempotencia y cleanup compensatorio del storage;
- API pública de solo lectura para recetas `PUBLISHED`:
  - `GET /api/recipes`
  - `GET /api/recipes/{slug}`
  - `GET /api/recipes/{slug}/images/{imageId}`
- listado con paginación zero-based, tamaño máximo controlado, filtros por `category`, `country`, `type`, `difficulty` y búsqueda case-insensitive por nombre mediante `q`;
- DTOs públicos separados para listado y detalle, sin exponer entidades JPA ni `storageKey`;
- entrega segura de imágenes mediante `MediaStorageService`, validando receta publicada y pertenencia de la imagen;
- autenticación Admin mediante sesión HTTP, BCrypt, CSRF y protección contra session fixation;
- `AdminUser` persistente mediante Flyway V3;
- endpoints de autenticación:
  - `GET /api/admin/auth/csrf`
  - `POST /api/admin/auth/login`
  - `GET /api/admin/auth/session`
  - `POST /api/admin/auth/logout`
- `/api/admin/**` restringido a autoridad `ADMIN`;
- CORS Admin de desarrollo restringido a `http://localhost:5501` con credentials;
- API administrativa de recetas:
  - `GET /api/admin/recipes`
  - `GET /api/admin/recipes/{id}`
  - `POST /api/admin/recipes`
  - `PATCH /api/admin/recipes/{id}`
  - `POST /api/admin/recipes/{id}/publish`
  - `POST /api/admin/recipes/{id}/archive`
  - `POST /api/admin/recipes/{id}/restore`
- creación siempre en `DRAFT`, slug generado por backend y estable tras renombrar;
- edición transaccional con reemplazo completo de ingredientes y pasos;
- lifecycle explícito `DRAFT -> PUBLISHED -> ARCHIVED -> DRAFT`;
- API pública sincronizada inmediatamente con publish/archive/restore.

Para peticiones Admin que modifican estado, el frontend debe obtener primero el token mediante `GET /api/admin/auth/csrf` y enviarlo en el header `X-CSRF-TOKEN`. Las peticiones cross-origin de Admin deben usar la cookie de sesión con credentials.

## Siguiente bloque

Frontend Admin.
