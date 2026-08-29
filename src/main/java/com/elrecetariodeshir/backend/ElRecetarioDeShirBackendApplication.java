package com.elrecetariodeshir.backend;

import com.elrecetariodeshir.backend.media.storage.MediaStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MediaStorageProperties.class)
public class ElRecetarioDeShirBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElRecetarioDeShirBackendApplication.class, args);
    }
}
