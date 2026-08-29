package com.elrecetariodeshir.backend.legacyimport;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("legacy-import")
@ConditionalOnProperty(
        name = "app.legacy-import.enabled",
        havingValue = "true")
@EnableConfigurationProperties(LegacyRecipeImportProperties.class)
public class LegacyRecipeImportRunner implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LegacyRecipeImportRunner.class);

    private final LegacyRecipeImportProperties properties;
    private final LegacyRecipeImportService importService;

    public LegacyRecipeImportRunner(
            LegacyRecipeImportProperties properties,
            LegacyRecipeImportService importService) {
        this.properties = properties;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path jsonPath = requiredPath(
                properties.getJson(),
                "LEGACY_RECIPE_JSON");

        Path assetsRoot = requiredPath(
                properties.getAssetsRoot(),
                "LEGACY_RECIPE_ASSETS_ROOT");

        LOGGER.info("Starting explicitly enabled legacy recipe import");

        LegacyRecipeImportResult result =
                importService.importRecipes(jsonPath, assetsRoot);

        LOGGER.info(
                "Legacy recipe import finished: status={}, recipes={}, ingredients={}, steps={}, images={}",
                result.status(),
                result.recipes(),
                result.ingredients(),
                result.steps(),
                result.images());
    }

    private Path requiredPath(String value, String configurationName) {
        if (value == null || value.isBlank()) {
            throw new LegacyRecipeImportException(
                    configurationName + " must be configured");
        }

        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new LegacyRecipeImportException(
                    configurationName + " is invalid",
                    exception);
        }
    }
}
