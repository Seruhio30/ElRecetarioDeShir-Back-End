package com.elrecetariodeshir.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PublicApiCorsConfig implements WebMvcConfigurer {

    private static final String DEVELOPMENT_ORIGIN = "http://localhost:5501";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/recipes/**")
                .allowedOrigins(DEVELOPMENT_ORIGIN)
                .allowedMethods("GET")
                .allowedHeaders("*");
    }
}
