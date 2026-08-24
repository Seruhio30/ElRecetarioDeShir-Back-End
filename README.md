# El Recetario de Shir - Back End

Backend de **El Recetario de Shir**, construido como una aplicación Spring Boot independiente del frontend.

## Stack actual

- Java 21
- Spring Boot 4.1.1
- Maven
- JUnit
- Git

## Ejecutar el proyecto

Para iniciar la aplicación:

    ./mvnw spring-boot:run

Para ejecutar las pruebas:

    ./mvnw test

## Estado actual

Foundation inicial del backend completada con una aplicación Spring Boot mínima.

Todavía no incluye:

- base de datos;
- JPA;
- Flyway;
- Spring Security;
- APIs de negocio;
- gestión de recetas.

## Siguiente bloque

feat/database-foundation

Incorporará MySQL, configuración del datasource mediante variables de entorno, Flyway y validación de migraciones.
