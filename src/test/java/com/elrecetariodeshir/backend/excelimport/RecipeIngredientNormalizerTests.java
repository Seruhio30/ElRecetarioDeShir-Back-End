package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;

class RecipeIngredientNormalizerTests {

    @Test
    void parsesNumericExcelQuantity() {
        var result = RecipeIngredientNormalizer.normalize(
                "Harina",
                150,
                "gr",
                0);

        assertThat(result.deterministic()).isTrue();
        assertThat(result.ingredient().quantity())
                .isEqualByComparingTo("150");
        assertThat(result.ingredient().unit())
                .isEqualTo(RecipeIngredientUnit.GRAM);
        assertThat(result.ingredient().displayText())
                .isEqualTo("150 gr Harina");
    }

    @Test
    void parsesNumericTextQuantity() {
        var result = RecipeIngredientNormalizer.normalize(
                "Leche",
                "2.5",
                "ml",
                0);

        assertThat(result.deterministic()).isTrue();
        assertThat(result.ingredient().quantity())
                .isEqualByComparingTo("2.5");
    }

    @Test
    void parsesIngredientRangeWithDash() {
        var result = RecipeIngredientNormalizer.normalize(
                "Huevos",
                "3 - 4",
                "unidad",
                0);

        assertThat(result.deterministic()).isTrue();
        assertThat(result.ingredient().quantity())
                .isEqualByComparingTo("3");
        assertThat(result.ingredient().quantityMax())
                .isEqualByComparingTo("4");
    }

    @Test
    void parsesIngredientRangeWithA() {
        var result = RecipeIngredientNormalizer.normalize(
                "Huevos",
                "3 a 4",
                "unidad",
                0);

        assertThat(result.deterministic()).isTrue();
        assertThat(result.ingredient().quantity())
                .isEqualByComparingTo("3");
        assertThat(result.ingredient().quantityMax())
                .isEqualByComparingTo("4");
    }

    @Test
    void missingQuantityRequiresReview() {
        var result = RecipeIngredientNormalizer.normalize(
                "Salsa holandesa",
                null,
                null,
                0);

        assertThat(result.deterministic()).isFalse();
        assertThat(result.ingredient().quantity()).isNull();
        assertThat(result.warnings())
                .contains("Ingredient quantity is missing.");
    }

    @Test
    void zeroQuantityRequiresReview() {
        var result = RecipeIngredientNormalizer.normalize(
                "Agua fria",
                0,
                "und",
                0);

        assertThat(result.deterministic()).isFalse();
        assertThat(result.warnings())
                .contains("Ingredient quantity is zero and requires review.");
    }

    @Test
    void unknownUnitRequiresReview() {
        var result = RecipeIngredientNormalizer.normalize(
                "Agua",
                1.2,
                "la",
                0);

        assertThat(result.deterministic()).isFalse();
        assertThat(result.ingredient().unit())
                .isEqualTo(RecipeIngredientUnit.OTHER);
        assertThat(result.warnings())
                .contains("Unknown ingredient unit: la");
    }

    @Test
    void ambiguousUnaUnitRequiresReview() {
        var result = RecipeIngredientNormalizer.normalize(
                "Chile Guajillo",
                0.5,
                "una",
                0);

        assertThat(result.deterministic()).isFalse();
        assertThat(result.ingredient().unit())
                .isEqualTo(RecipeIngredientUnit.OTHER);
    }

    @Test
    void preservesReadableDisplayText() {
        var result = RecipeIngredientNormalizer.normalize(
                "Azúcar glass (en polvo)",
                120,
                "gr",
                3);

        assertThat(result.ingredient().displayText())
                .isEqualTo("120 gr Azúcar glass (en polvo)");
        assertThat(result.ingredient().position()).isEqualTo(3);
    }
}
