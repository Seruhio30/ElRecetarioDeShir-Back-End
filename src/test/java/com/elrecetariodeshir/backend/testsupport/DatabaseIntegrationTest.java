package com.elrecetariodeshir.backend.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_SPRING_DATASOURCE_URL}",
        "spring.datasource.username=${TEST_SPRING_DATASOURCE_USERNAME}",
        "spring.datasource.password=${TEST_SPRING_DATASOURCE_PASSWORD}"
})
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@Import(TestDatabaseSafetyConfiguration.class)
public @interface DatabaseIntegrationTest {
}
