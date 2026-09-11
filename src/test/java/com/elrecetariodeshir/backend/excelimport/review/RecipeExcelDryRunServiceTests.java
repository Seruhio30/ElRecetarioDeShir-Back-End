package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

@DatabaseIntegrationTest
@Transactional
class RecipeExcelDryRunServiceTests {

    private static final Path WORKBOOK =
            Path.of(System.getenv("RECIPE_EXCEL_PATH"));

    private static final Path OVERRIDES =
            Path.of(System.getenv("EXCEL_RECIPE_IMPORT_OVERRIDES"));

    private static final Path LEGACY =
            Path.of("src/test/resources/legacy/recipes.json");

    private static final Path REPORT =
            Path.of("target/recipe-import-dry-run.json");

    @Autowired
    private RecipeExcelDryRunService dryRunService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void realWorkbookDryRunGeneratesPlanWithoutPersisting() {
        long recipesBefore = count("recipe");
        long ingredientsBefore = count("recipe_ingredient");
        long stepsBefore = count("recipe_step");
        long imagesBefore = count("recipe_image");

        RecipeExcelDryRunReport report =
                dryRunService.dryRun(
                        WORKBOOK,
                        OVERRIDES,
                        LEGACY);

        new RecipeImportPlanJsonWriter()
                .write(report, REPORT);

        RecipeImportPlan plan = report.plan();

        assertThat(plan.detected()).isEqualTo(62);
        assertThat(plan.approved()).isEqualTo(62);
        assertThat(plan.pending()).isZero();
        assertThat(plan.withImage()).isEqualTo(60);
        assertThat(plan.withoutImage()).isEqualTo(2);
        assertThat(plan.ingredients()).isEqualTo(450);
        assertThat(plan.steps()).isEqualTo(518);

        assertThat(report.legacyCollisionCount()).isEqualTo(12);
        assertThat(report.legacyCollisions()).hasSize(12);
        assertThat(report.legacyCollisions())
                .extracting(RecipeLegacyCollision::sourceSheet)
                .contains(
                        "38. Torta Española",
                        "43. Causa Limeña",
                        "52. Enyucado de carne");

        assertThat(count("recipe")).isEqualTo(recipesBefore);
        assertThat(count("recipe_ingredient")).isEqualTo(ingredientsBefore);
        assertThat(count("recipe_step")).isEqualTo(stepsBefore);
        assertThat(count("recipe_image")).isEqualTo(imagesBefore);

        assertThat(REPORT).isRegularFile();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Long.class);
    }
}
