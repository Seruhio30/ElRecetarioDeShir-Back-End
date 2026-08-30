package com.elrecetariodeshir.backend.legacyimport;

import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.media.storage.MediaStorageService;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;

@DatabaseIntegrationTest
@Import(LegacyRecipeImportRollbackTests.RollbackTestConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LegacyRecipeImportRollbackTests {

    private static final Path STORAGE_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "elrecetariodeshir-import-rollback-tests");

    @TempDir
    Path tempDirectory;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private LegacyRecipeImportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Path jsonPath;
    private Path assetsRoot;

    @BeforeEach
    void setUp() throws IOException {
        recipeRepository.deleteAll();
        recipeRepository.flush();

        cleanStorage();

        jsonPath = Path.of("src/test/resources/legacy/recipes.json");
        assetsRoot = tempDirectory.resolve("assets");
        Files.createDirectories(assetsRoot);

        List<LegacyRecipeSource> recipes = new ObjectMapper().readValue(
                jsonPath.toFile(),
                new com.fasterxml.jackson.core.type.TypeReference<List<LegacyRecipeSource>>() {
                });

        for (LegacyRecipeSource recipe : recipes) {
            Path filename = Path.of(recipe.image()).getFileName();

            Files.write(
                    assetsRoot.resolve(filename),
                    ("image-" + recipe.name()).getBytes());
        }
    }

    @Test
    void rollsBackDatabaseAndDeletesCreatedFilesWhenPostPersistVerificationFails()
            throws IOException {

        assertThatThrownBy(() -> service.importRecipes(jsonPath, assetsRoot))
                .isInstanceOf(LegacyRecipeImportException.class)
                .hasMessageContaining("conflicts with expected import");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe",
                Long.class))
                .isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_ingredient",
                Long.class))
                .isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_step",
                Long.class))
                .isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_image",
                Long.class))
                .isZero();

        assertThat(storageFileCount()).isZero();
    }

    private long storageFileCount() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return 0;
        }

        try (var files = Files.list(STORAGE_ROOT)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private void cleanStorage() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }

        try (var paths = Files.walk(STORAGE_ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(STORAGE_ROOT)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @TestConfiguration
    static class RollbackTestConfiguration {

        @Bean
        @Primary
        MediaStorageService rollbackVerificationStorage() {
            return new MediaStorageService() {

                @Override
                public String store(InputStream content, String extension) {
                    try {
                        Files.createDirectories(STORAGE_ROOT);

                        String normalizedExtension =
                                extension.startsWith(".")
                                        ? extension.substring(1)
                                        : extension;

                        String storageKey =
                                UUID.randomUUID() + "." + normalizedExtension;

                        Files.copy(
                                content,
                                STORAGE_ROOT.resolve(storageKey));

                        return storageKey;
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                }

                @Override
                public InputStream read(String storageKey) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean exists(String storageKey) {
                    // Deliberately force post-persist verification failure.
                    return false;
                }

                @Override
                public boolean delete(String storageKey) {
                    try {
                        return Files.deleteIfExists(
                                STORAGE_ROOT.resolve(storageKey));
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                }
            };
        }
    }
}
