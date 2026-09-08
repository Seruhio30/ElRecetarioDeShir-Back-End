package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

class RecipeYieldNormalizerTests {

    @Test
    void parsesSimpleQuantity() {
        var result = RecipeYieldNormalizer.normalize("12");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().quantity()).isEqualByComparingTo("12");
        assertThat(result.yield().min()).isNull();
        assertThat(result.yield().max()).isNull();
        assertThat(result.yield().unit()).isNull();
        assertThat(result.yield().displayText()).isEqualTo("12");
    }

    @Test
    void parsesRange() {
        var result = RecipeYieldNormalizer.normalize("8 - 12");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().min()).isEqualByComparingTo("8");
        assertThat(result.yield().max()).isEqualByComparingTo("12");
    }

    @Test
    void parsesUnitQuantity() {
        var result = RecipeYieldNormalizer.normalize("18 u");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().quantity()).isEqualByComparingTo("18");
        assertThat(result.yield().unit()).isEqualTo(RecipeYieldUnit.UNIT);
    }

    @Test
    void parsesWeight() {
        var result = RecipeYieldNormalizer.normalize("600 gr");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().quantity()).isEqualByComparingTo("600");
        assertThat(result.yield().unit()).isEqualTo(RecipeYieldUnit.GRAM);
    }

    @Test
    void parsesVolumeCaseInsensitive() {
        var result = RecipeYieldNormalizer.normalize("1 Litro");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().quantity()).isEqualByComparingTo("1");
        assertThat(result.yield().unit()).isEqualTo(RecipeYieldUnit.LITER);
    }

    @Test
    void preservesQuantityWithDescription() {
        var result = RecipeYieldNormalizer.normalize("2 (22cm)");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().quantity()).isEqualByComparingTo("2");
        assertThat(result.yield().unit()).isEqualTo(RecipeYieldUnit.OTHER);
        assertThat(result.yield().displayText()).isEqualTo("2 (22cm)");
    }

    @Test
    void parsesWorkbookLabel() {
        var result = RecipeYieldNormalizer.normalize(
                "Numero de Porciones:      18 u");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().quantity()).isEqualByComparingTo("18");
        assertThat(result.yield().unit()).isEqualTo(RecipeYieldUnit.UNIT);
        assertThat(result.yield().displayText())
                .isEqualTo("Numero de Porciones:      18 u");
    }

    @Test
    void unknownUnitRequiresReview() {
        var result = RecipeYieldNormalizer.normalize("3 bandejas");

        assertThat(result.deterministic()).isFalse();
        assertThat(result.yield().quantity()).isEqualByComparingTo("3");
        assertThat(result.yield().unit()).isEqualTo(RecipeYieldUnit.OTHER);
        assertThat(result.warnings()).containsExactly(
                "Unknown yield unit: bandejas");
    }

    @Test
    void simpleNumberDoesNotInventServingUnit() {
        var result = RecipeYieldNormalizer.normalize("6");

        assertThat(result.deterministic()).isTrue();
        assertThat(result.yield().quantity()).isEqualByComparingTo(
                new BigDecimal("6"));
        assertThat(result.yield().unit()).isNull();
    }
}
