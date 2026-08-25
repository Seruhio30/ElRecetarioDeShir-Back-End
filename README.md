# El Recetario de Shir - Back End

Backend de **El Recetario de Shir**, construido como una aplicación Spring Boot independiente del frontend.

## Stack actual

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring Data JPA
- MySQL
- Flyway
- JUnit
- Git

## Configuración de base de datos

La aplicación obtiene la conexión mediante variables de entorno:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

No se versionan credenciales reales.

Flyway es la fuente de verdad del esquema de base de datos. Hibernate está configurado para validar el esquema, no para crearlo ni modificarlo.

## Ejecutar el proyecto

Con las variables de entorno configuradas:

    ./mvnw spring-boot:run

Para ejecutar las pruebas de integración contra MySQL:

    ./mvnw test

## Estado actual

Persistence foundation completada con:

- datasource externo para MySQL;
- Spring Data JPA;
- Flyway como fuente de verdad del esquema;
- Hibernate configurado con `ddl-auto=validate`;
- modelo persistente `Recipe`, `RecipeIngredient`, `RecipeStep` y `RecipeImage`;
- enums de dominio para estado, categoría, tipo y dificultad;
- migraciones V1 y V2 aplicadas;
- relaciones, orden persistente, timestamps y constraints validados contra MySQL real.

## Siguiente bloque

El siguiente bloque se decidirá desde el chat maestro después de revisar el modelo persistente.
