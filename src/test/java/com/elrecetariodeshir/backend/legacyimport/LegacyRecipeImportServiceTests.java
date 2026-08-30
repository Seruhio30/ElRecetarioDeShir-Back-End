package com.elrecetariodeshir.backend.legacyimport;

import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.media.storage.MediaStorageProperties;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeType;

@DatabaseIntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LegacyRecipeImportServiceTests {

    @TempDir
    Path tempDirectory;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private LegacyRecipeImportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MediaStorageProperties mediaStorageProperties;

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
    void importsTwentySixRecipesAndImages() throws IOException {
        LegacyRecipeImportResult result =
                service.importRecipes(jsonPath, assetsRoot);

        assertThat(result.status())
                .isEqualTo(LegacyRecipeImportResult.Status.IMPORTED);
        assertThat(result.recipes()).isEqualTo(26);
        assertThat(result.ingredients()).isEqualTo(239);
        assertThat(result.steps()).isEqualTo(176);
        assertThat(result.images()).isEqualTo(26);

        assertThat(count("recipe")).isEqualTo(26);
        assertThat(count("recipe_ingredient")).isEqualTo(239);
        assertThat(count("recipe_step")).isEqualTo(176);
        assertThat(count("recipe_image")).isEqualTo(26);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT slug) FROM recipe",
                Long.class))
                .isEqualTo(26);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe WHERE status = 'PUBLISHED'",
                Long.class))
                .isEqualTo(26);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe WHERE published_at IS NOT NULL",
                Long.class))
                .isEqualTo(26);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe WHERE archived_at IS NOT NULL",
                Long.class))
                .isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM recipe_image
                WHERE primary_image = TRUE
                  AND position = 0
                  AND storage_key IS NOT NULL
                  AND storage_key <> ''
                """,
                Long.class))
                .isEqualTo(26);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT ri.original_filename
                FROM recipe_image ri
                JOIN recipe r ON r.id = ri.recipe_id
                WHERE r.slug = 'paella'
                """,
                String.class))
                .isEqualTo("paella.jpg");

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT ri.media_type
                FROM recipe_image ri
                JOIN recipe r ON r.id = ri.recipe_id
                WHERE r.slug = 'paella'
                """,
                String.class))
                .isEqualTo("image/jpeg");

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT ri.alt_text
                FROM recipe_image ri
                JOIN recipe r ON r.id = ri.recipe_id
                WHERE r.slug = 'paella'
                """,
                String.class))
                .isEqualTo("Paella");

        assertThat(jdbcTemplate.queryForList(
                """
                SELECT i.text
                FROM recipe_ingredient i
                JOIN recipe r ON r.id = i.recipe_id
                WHERE r.slug = 'paella'
                ORDER BY i.position
                """,
                String.class))
                .startsWith(
                        "400 g de muslito de pollo",
                        "400 g de magro o costillas de cerdo cortadas en trozos pequeños");

        assertThat(jdbcTemplate.queryForList(
                """
                SELECT s.instruction
                FROM recipe_step s
                JOIN recipe r ON r.id = s.recipe_id
                WHERE r.slug = 'paella'
                ORDER BY s.position
                """,
                String.class))
                .startsWith(
                        "Salpimentar el pollo y el cerdo.",
                        "Colocar aceite de oliva en una sartén y dorar el pollo y el cerdo a fuego medio hasta que estén bien cocinados.");

        assertThat(storageFileCount()).isEqualTo(26);
    }

    @Test
    void secondExecutionIsNoOpAndCreatesNoNewImages() throws IOException {
        LegacyRecipeImportResult first =
                service.importRecipes(jsonPath, assetsRoot);

        assertThat(first.status())
                .isEqualTo(LegacyRecipeImportResult.Status.IMPORTED);

        long filesBefore = storageFileCount();

        LegacyRecipeImportResult second =
                service.importRecipes(jsonPath, assetsRoot);

        assertThat(second.status())
                .isEqualTo(LegacyRecipeImportResult.Status.NO_OP);
        assertThat(second.recipes()).isEqualTo(26);
        assertThat(second.ingredients()).isEqualTo(239);
        assertThat(second.steps()).isEqualTo(176);
        assertThat(second.images()).isEqualTo(26);

        assertThat(storageFileCount()).isEqualTo(filesBefore);
        assertThat(count("recipe")).isEqualTo(26);
        assertThat(count("recipe_image")).isEqualTo(26);
    }

    @Test
    void abortsOnPartialDatabaseStateBeforeCreatingImages() throws IOException {
        Recipe partial = new Recipe(
                "paella",
                "Paella",
                "Española",
                RecipeCategory.INTERNATIONAL,
                "ES",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.MEDIUM,
                null,
                RecipeStatus.PUBLISHED);

        partial.setPublishedAt(java.time.Instant.now());

        recipeRepository.saveAndFlush(partial);

        assertThatThrownBy(() -> service.importRecipes(jsonPath, assetsRoot))
                .isInstanceOf(LegacyRecipeImportException.class)
                .hasMessageContaining("partial or conflicting");

        assertThat(count("recipe")).isEqualTo(1);
        assertThat(storageFileCount()).isZero();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Long.class);
    }

    private long storageFileCount() throws IOException {
        if (!Files.exists(storageRoot())) {
            return 0;
        }

        try (var files = Files.list(storageRoot())) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private Path storageRoot() {
        return Path.of(mediaStorageProperties.getRoot())
                .toAbsolutePath()
                .normalize();
    }

    private void cleanStorage() throws IOException {
        if (!Files.exists(storageRoot())) {
            return;
        }

        try (var paths = Files.walk(storageRoot())) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(storageRoot())) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
