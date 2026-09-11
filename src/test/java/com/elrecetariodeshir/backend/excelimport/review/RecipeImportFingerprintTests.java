package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.excelimport.RecipeImportCandidate;
import com.elrecetariodeshir.backend.excelimport.RecipeImportIngredient;
import com.elrecetariodeshir.backend.excelimport.RecipeImportStep;
import com.elrecetariodeshir.backend.excelimport.RecipePreflightStatus;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;

class RecipeImportFingerprintTests {

    @Test
    void sameSourceProducesStableFingerprint() {
        var candidate = candidate("Preparar.");

        String first = RecipeImportFingerprint.from(candidate);
        String second = RecipeImportFingerprint.from(candidate);

        assertThat(first)
                .hasSize(64)
                .isEqualTo(second);
    }

    @Test
    void sourceChangeProducesDifferentFingerprint() {
        String first = RecipeImportFingerprint.from(
                candidate("Preparar."));

        String second = RecipeImportFingerprint.from(
                candidate("Preparar de otra forma."));

        assertThat(first).isNotEqualTo(second);
    }

    private RecipeImportCandidate candidate(String instruction) {
        return new RecipeImportCandidate(
                "Recetas.xlsx",
                "37. Paella Valenciana",
                37,
                "Paella Valenciana",
                "Paella Valenciana",
                "paella-valenciana",
                null,
                null,
                null,
                null,
                RecipeDifficulty.MEDIUM,
                null,
                null,
                List.of(new RecipeImportIngredient(
                        "Arroz",
                        new BigDecimal("500"),
                        null,
                        RecipeIngredientUnit.GRAM,
                        null,
                        "500 gr Arroz",
                        0,
                        List.of())),
                List.of(new RecipeImportStep(
                        0,
                        instruction)),
                "embedded-image@1:1.jpeg",
                true,
                List.of(),
                RecipePreflightStatus.REVIEW_REQUIRED);
    }
}
