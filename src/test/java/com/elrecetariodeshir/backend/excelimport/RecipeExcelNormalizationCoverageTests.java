package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

class RecipeExcelNormalizationCoverageTests {

    private static final Path WORKBOOK =
            ExcelRecipeTestWorkbook.canonicalWorkbook();

    @Test
    void normalizesAllApprovedIngredientUnitVariants() {
        assertIngredientUnits(
                RecipeIngredientUnit.GRAM,
                "gr", "grs");

        assertIngredientUnits(
                RecipeIngredientUnit.KILOGRAM,
                "kg");

        assertIngredientUnits(
                RecipeIngredientUnit.MILLILITER,
                "ml");

        assertIngredientUnits(
                RecipeIngredientUnit.LITER,
                "Litro", "litro", "lt");

        assertIngredientUnits(
                RecipeIngredientUnit.UNIT,
                "und", "ud", "unidad", "unidades");

        assertIngredientUnits(
                RecipeIngredientUnit.CUP,
                "taza", "tazas");

        assertIngredientUnits(
                RecipeIngredientUnit.TABLESPOON,
                "cda", "cdas", "cucharadas.");

        assertIngredientUnits(
                RecipeIngredientUnit.TEASPOON,
                "cdta");

        assertIngredientUnits(
                RecipeIngredientUnit.PACKAGE,
                "paquete", "paquetes");

        assertIngredientUnits(
                RecipeIngredientUnit.PORTION,
                "porción");

        assertIngredientUnits(
                RecipeIngredientUnit.RECIPE,
                "receta");

        assertIngredientUnits(
                RecipeIngredientUnit.BRANCH,
                "ramas");

        assertIngredientUnits(
                RecipeIngredientUnit.STALK,
                "tallos");

        assertThat(RecipeImportUnitNormalizer
                .normalizeIngredientUnit("una"))
                .isEqualTo(RecipeIngredientUnit.OTHER);

        assertThat(RecipeImportUnitNormalizer
                .normalizeIngredientUnit("la"))
                .isEqualTo(RecipeIngredientUnit.OTHER);
    }

    @Test
    void normalizesApprovedYieldUnits() {
        assertYieldUnits(RecipeYieldUnit.UNIT,
                "u", "und", "unidad", "unidades");

        assertYieldUnits(RecipeYieldUnit.GRAM,
                "gr", "grs");

        assertYieldUnits(RecipeYieldUnit.KILOGRAM,
                "kg");

        assertYieldUnits(RecipeYieldUnit.MILLILITER,
                "ml");

        assertYieldUnits(RecipeYieldUnit.LITER,
                "litro", "Litro", "lt");
    }

    @Test
    void parsesYieldRangeWithUnit() {
        var result = RecipeYieldNormalizer.normalize("8-10 u");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().min())
                .isEqualByComparingTo("8");
        assertThat(result.yield().max())
                .isEqualByComparingTo("10");
        assertThat(result.yield().unit())
                .isEqualTo(RecipeYieldUnit.UNIT);
    }

    @Test
    void realWorkbookStepsHaveSequentialPositions() {
        var result = new RecipeExcelPreflightService()
                .preflight(WORKBOOK);

        assertThat(result.candidates()).allSatisfy(candidate -> {
            List<Integer> positions = candidate.steps().stream()
                    .map(RecipeImportStep::position)
                    .toList();

            assertThat(positions)
                    .containsExactlyElementsOf(
                            java.util.stream.IntStream
                                    .range(0, positions.size())
                                    .boxed()
                                    .toList());
        });
    }

    private void assertIngredientUnits(
            RecipeIngredientUnit expected,
            String... rawValues) {

        for (String raw : rawValues) {
            assertThat(RecipeImportUnitNormalizer
                    .normalizeIngredientUnit(raw))
                    .as(raw)
                    .isEqualTo(expected);
        }
    }

    private void assertYieldUnits(
            RecipeYieldUnit expected,
            String... rawValues) {

        for (String raw : rawValues) {
            assertThat(RecipeImportUnitNormalizer
                    .normalizeYieldUnit(raw))
                    .as(raw)
                    .isEqualTo(expected);
        }
    }
}
