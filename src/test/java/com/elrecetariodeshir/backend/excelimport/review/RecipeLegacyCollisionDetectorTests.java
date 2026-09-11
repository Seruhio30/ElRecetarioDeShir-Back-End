package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.excelimport.RecipeExcelPreflightService;

class RecipeLegacyCollisionDetectorTests {

    private static final Path WORKBOOK =
            Path.of(System.getenv("RECIPE_EXCEL_PATH"));

    private static final Path LEGACY =
            Path.of("src/test/resources/legacy/recipes.json");

    @Test
    void detectsKnownLegacyCollisionsFromCanonicalWorkbook() {
        var candidates =
                new RecipeExcelPreflightService()
                        .preflight(WORKBOOK)
                        .candidates();

        var collisions =
                new RecipeLegacyCollisionDetector()
                        .detect(candidates, LEGACY);

        assertThat(collisions).hasSize(12);

        assertThat(collisions)
                .extracting(RecipeLegacyCollision::sourceSheet)
                .containsExactlyInAnyOrder(
                        "38. Torta Española",
                        "40. Quiche Lorraine",
                        "41. Papa a la Huancaína",
                        "42. Arroz Chaufa",
                        "43. Causa Limeña",
                        "44. Totopos",
                        "45. Salsa Roja",
                        "46. Salsa Verde",
                        "47. Carne al Pastor",
                        "48. Chilaquiles",
                        "49. Huaraches",
                        "52. Enyucado de carne");

        assertThat(collisions)
                .allSatisfy(collision ->
                        assertThat(collision.matchType())
                                .isEqualTo(
                                        RecipeLegacyCollision.MatchType.NAME_AND_SLUG));
    }
}
