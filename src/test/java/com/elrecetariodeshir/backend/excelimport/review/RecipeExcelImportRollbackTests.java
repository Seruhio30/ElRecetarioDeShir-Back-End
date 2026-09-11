package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.excelimport.RecipeExcelPreflightService;
import com.elrecetariodeshir.backend.media.storage.MediaStorageService;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeType;
import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

@DatabaseIntegrationTest
@Import(RecipeExcelImportRollbackTests.RollbackTestConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RecipeExcelImportRollbackTests {

    private static final Path WORKBOOK =
            Path.of(System.getenv("RECIPE_EXCEL_PATH"));

    private static final Path STORAGE_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "elrecetariodeshir-excel-import-rollback-tests");

    @Autowired
    private RecipeExcelImportService importService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws IOException {
        recipeRepository.deleteAll();
        recipeRepository.flush();
        cleanStorage();
    }

    @Test
    void rollsBackDatabaseAndDeletesStoredImageWhenVerificationFails()
            throws IOException {

        var source = new RecipeExcelPreflightService()
                .preflight(WORKBOOK)
                .candidates().stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.imageExists()))
                .findFirst()
                .orElseThrow();

        var candidate = new ReviewedRecipeImportCandidate(
                source.sourceWorkbook(),
                source.sourceSheet(),
                source.sourceRecipeNumber(),
                RecipeImportFingerprint.from(source),
                source.originalName(),
                "Excel Rollback Test",
                "excel-rollback-test",
                source.cuisine(),
                RecipeCategory.INTERNATIONAL,
                "CR",
                RecipeType.DESSERT,
                RecipeDifficulty.MEDIUM,
                source.timeMinutes(),
                source.yield(),
                source.ingredients(),
                source.steps(),
                source.imageReference(),
                true,
                false,
                source.warnings(),
                source.warnings(),
                List.of(),
                ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);

        assertThatThrownBy(() ->
                importService.importApproved(
                        WORKBOOK,
                        List.of(candidate)))
                .isInstanceOf(RecipeImportReviewException.class)
                .hasMessageContaining(
                        "Stored recipe image could not be verified");

        assertThat(count("recipe")).isZero();
        assertThat(count("recipe_ingredient")).isZero();
        assertThat(count("recipe_step")).isZero();
        assertThat(count("recipe_image")).isZero();

        assertThat(storageFileCount()).isZero();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Long.class);
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
                public String store(
                        InputStream content,
                        String extension) {

                    try {
                        Files.createDirectories(STORAGE_ROOT);

                        String normalizedExtension =
                                extension.startsWith(".")
                                        ? extension.substring(1)
                                        : extension;

                        String storageKey =
                                UUID.randomUUID()
                                        + "."
                                        + normalizedExtension;

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
