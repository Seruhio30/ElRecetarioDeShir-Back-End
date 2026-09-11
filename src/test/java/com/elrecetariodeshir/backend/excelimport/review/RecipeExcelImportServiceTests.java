package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.excelimport.RecipeImportIngredient;
import com.elrecetariodeshir.backend.excelimport.RecipeImportStep;
import com.elrecetariodeshir.backend.excelimport.RecipeImportYield;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeType;
import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;
import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

@DatabaseIntegrationTest
@Transactional
class RecipeExcelImportServiceTests {

    private static final Path WORKBOOK =
            Path.of(System.getenv("RECIPE_EXCEL_PATH"));


    @Autowired
    private RecipeExcelImportService importService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void importsApprovedRecipeAsDraftWithStructuredData() {
        var candidate = candidate("excel-import-test");

        RecipeExcelImportResult result =
                importService.importApproved(WORKBOOK, List.of(candidate));

        assertThat(result.imported()).isEqualTo(1);

        Recipe persisted = recipeRepository
                .findAllBySlugInOrNameIn(
                        List.of("excel-import-test"),
                        List.of("Excel Import Test"))
                .getFirst();

        assertThat(persisted.getStatus())
                .isEqualTo(RecipeStatus.DRAFT);
        assertThat(persisted.getPublishedAt()).isNull();

        assertThat(persisted.getYieldMin())
                .isEqualByComparingTo("8");
        assertThat(persisted.getYieldMax())
                .isEqualByComparingTo("10");
        assertThat(persisted.getYieldUnit())
                .isEqualTo(RecipeYieldUnit.UNIT);
        assertThat(persisted.getYieldDisplay())
                .isEqualTo("8-10 unidades");

        assertThat(persisted.getIngredients()).hasSize(2);

        var first = persisted.getIngredients().get(0);
        assertThat(first.getIngredientName()).isEqualTo("Harina");
        assertThat(first.getQuantity())
                .isEqualByComparingTo("250");
        assertThat(first.getUnit())
                .isEqualTo(RecipeIngredientUnit.GRAM);
        assertThat(first.getNotes())
                .isEqualTo("tamizada");
        assertThat(first.getDisplayText())
                .isEqualTo("250 gr Harina tamizada");

        var second = persisted.getIngredients().get(1);
        assertThat(second.getIngredientName()).isEqualTo("Huevos");
        assertThat(second.getQuantity())
                .isEqualByComparingTo("2");
        assertThat(second.getQuantityMax())
                .isEqualByComparingTo("3");
        assertThat(second.getUnit())
                .isEqualTo(RecipeIngredientUnit.UNIT);

        assertThat(persisted.getSteps())
                .extracting(step -> step.getInstruction())
                .containsExactly(
                        "Mezclar los ingredientes.",
                        "Hornear.");
    }

    @Test
    void conflictingExistingRecipeBlocksImport() {
        Recipe existing = new Recipe(
                "excel-conflict",
                "Another Recipe",
                null,
                RecipeCategory.INTERNATIONAL,
                "CR",
                RecipeType.DESSERT,
                RecipeDifficulty.MEDIUM,
                45,
                RecipeStatus.DRAFT);

        existing.addIngredient(new com.elrecetariodeshir.backend.recipe.RecipeIngredient(
                0,
                "Harina",
                new BigDecimal("250"),
                null,
                RecipeIngredientUnit.GRAM,
                null,
                "250 gr Harina"));

        existing.addStep(new com.elrecetariodeshir.backend.recipe.RecipeStep(
                0,
                "Preparar."));

        recipeRepository.saveAndFlush(existing);

        var candidate = candidate("excel-conflict");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> importService.importApproved(WORKBOOK, List.of(candidate)))
                .isInstanceOf(RecipeImportReviewException.class)
                .hasMessageContaining("conflicts or partial states");

        assertThat(recipeRepository.findAllBySlugInOrNameIn(
                List.of("excel-conflict"),
                List.of("Excel Import Test")))
                .hasSize(1);
    }

    @Test
    void partialExistingAggregateBlocksImport() {
        Recipe existing = new Recipe(
                "excel-partial",
                "Excel Import Test",
                "Test cuisine",
                RecipeCategory.INTERNATIONAL,
                "CR",
                RecipeType.DESSERT,
                RecipeDifficulty.MEDIUM,
                45,
                RecipeStatus.DRAFT);

        existing.addIngredient(new com.elrecetariodeshir.backend.recipe.RecipeIngredient(
                0,
                "Ingrediente distinto",
                new BigDecimal("1"),
                null,
                RecipeIngredientUnit.UNIT,
                null,
                "1 und Ingrediente distinto"));

        existing.addStep(new com.elrecetariodeshir.backend.recipe.RecipeStep(
                0,
                "Paso distinto."));

        recipeRepository.saveAndFlush(existing);

        var candidate = candidate("excel-partial");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> importService.importApproved(WORKBOOK, List.of(candidate)))
                .isInstanceOf(RecipeImportReviewException.class)
                .hasMessageContaining("conflicts or partial states");

        assertThat(recipeRepository.findAllBySlugInOrNameIn(
                List.of("excel-partial"),
                List.of("Excel Import Test")))
                .hasSize(1);
    }

    @Test
    void importsEmbeddedImageIntoStorage() {
        var preflight =
                new com.elrecetariodeshir.backend.excelimport.RecipeExcelPreflightService()
                        .preflight(WORKBOOK);

        var source = preflight.candidates().stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.imageExists()))
                .findFirst()
                .orElseThrow();

        var candidate = new ReviewedRecipeImportCandidate(
                source.sourceWorkbook(),
                source.sourceSheet(),
                source.sourceRecipeNumber(),
                RecipeImportFingerprint.from(source),
                source.originalName(),
                "Excel Image Import Test",
                "excel-image-import-test",
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

        RecipeExcelImportResult result =
                importService.importApproved(
                        WORKBOOK,
                        List.of(candidate));

        assertThat(result.imported()).isEqualTo(1);

        Recipe persisted = recipeRepository
                .findAllBySlugInOrNameIn(
                        List.of("excel-image-import-test"),
                        List.of("Excel Image Import Test"))
                .getFirst();

        assertThat(persisted.getImages()).hasSize(1);

        var image = persisted.getImages().getFirst();

        assertThat(image.getStorageKey()).isNotBlank();
        assertThat(image.getMediaType())
                .isIn("image/jpeg", "image/png");
        assertThat(image.getAltText())
                .isEqualTo("Excel Image Import Test");
        assertThat(image.isPrimaryImage()).isTrue();
    }

    @Test
    void approvedMissingImageImportsDraftWithoutImage() {
        var source =
                new com.elrecetariodeshir.backend.excelimport.RecipeExcelPreflightService()
                        .preflight(WORKBOOK)
                        .candidates().stream()
                        .filter(candidate ->
                                "23. Tres Leches de Coco"
                                        .equals(candidate.sourceSheet()))
                        .findFirst()
                        .orElseThrow();

        var candidate = new ReviewedRecipeImportCandidate(
                source.sourceWorkbook(),
                source.sourceSheet(),
                source.sourceRecipeNumber(),
                RecipeImportFingerprint.from(source),
                source.originalName(),
                "Tres Leches de Coco Test",
                "tres-leches-coco-test",
                source.cuisine(),
                RecipeCategory.INTERNATIONAL,
                "CR",
                RecipeType.DESSERT,
                RecipeDifficulty.MEDIUM,
                source.timeMinutes(),
                source.yield(),
                source.ingredients(),
                source.steps(),
                null,
                false,
                true,
                source.warnings(),
                source.warnings(),
                List.of(),
                ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);

        RecipeExcelImportResult result =
                importService.importApproved(
                        WORKBOOK,
                        List.of(candidate));

        assertThat(result.imported()).isEqualTo(1);

        Recipe persisted = recipeRepository
                .findAllBySlugInOrNameIn(
                        List.of("tres-leches-coco-test"),
                        List.of("Tres Leches de Coco Test"))
                .getFirst();

        assertThat(persisted.getStatus())
                .isEqualTo(RecipeStatus.DRAFT);
        assertThat(persisted.getImages()).isEmpty();
    }

    @Test
    void secondExecutionIsNoOpForMatchingDraft() {
        var candidate = candidate("excel-import-idempotent");

        RecipeExcelImportResult first =
                importService.importApproved(WORKBOOK, List.of(candidate));

        RecipeExcelImportResult second =
                importService.importApproved(WORKBOOK, List.of(candidate));

        assertThat(first.imported()).isEqualTo(1);
        assertThat(second.imported()).isZero();
        assertThat(second.skippedAlreadyImported()).isEqualTo(1);

        assertThat(recipeRepository.findAllBySlugInOrNameIn(
                List.of("excel-import-idempotent"),
                List.of("Excel Import Test")))
                .hasSize(1);
    }

    private ReviewedRecipeImportCandidate candidate(String slug) {
        return new ReviewedRecipeImportCandidate(
                "Recetas.xlsx",
                "99. Excel Import Test",
                99,
                "test-fingerprint-" + slug,
                "Excel Import Test",
                "Excel Import Test",
                slug,
                "Test cuisine",
                RecipeCategory.INTERNATIONAL,
                "CR",
                RecipeType.DESSERT,
                RecipeDifficulty.MEDIUM,
                45,
                new RecipeImportYield(
                        null,
                        new BigDecimal("8"),
                        new BigDecimal("10"),
                        RecipeYieldUnit.UNIT,
                        "8-10 unidades"),
                List.of(
                        new RecipeImportIngredient(
                                "Harina",
                                new BigDecimal("250"),
                                null,
                                RecipeIngredientUnit.GRAM,
                                "tamizada",
                                "250 gr Harina tamizada",
                                0,
                                List.of()),
                        new RecipeImportIngredient(
                                "Huevos",
                                new BigDecimal("2"),
                                new BigDecimal("3"),
                                RecipeIngredientUnit.UNIT,
                                null,
                                "2-3 und Huevos",
                                1,
                                List.of())),
                List.of(
                        new RecipeImportStep(
                                0,
                                "Mezclar los ingredientes."),
                        new RecipeImportStep(
                                1,
                                "Hornear.")),
                null,
                false,
                true,
                List.of(),
                List.of(),
                List.of(),
                ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);
    }
}
