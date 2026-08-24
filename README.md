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

Database foundation completada con:

- datasource externo para MySQL;
- Spring Data JPA;
- MySQL Connector/J;
- Flyway;
- migración técnica inicial;
- validación real de conexión y migraciones contra MySQL.

## Siguiente bloque

`feat/recipe-persistence-foundation`

Será responsable del modelo de persistencia de recetas y sus migraciones de dominio.
