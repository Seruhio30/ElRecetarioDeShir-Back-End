package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.excelimport.RecipeImportIngredient;
import com.elrecetariodeshir.backend.excelimport.RecipeImportStep;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeIngredient;
import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeStep;
import com.elrecetariodeshir.backend.recipe.RecipeType;

class RecipeExcelImportPlannerTests {

    private final RecipeExcelImportPlanner planner =
            new RecipeExcelImportPlanner();

    @Test
    void approvedCandidateWithoutExistingMatchIsNew() {
        var plan = planner.plan(
                List.of(candidate("paella-valenciana", "Paella Valenciana")),
                List.of());

        assertThat(plan.newRecipes()).isEqualTo(1);
        assertThat(plan.entries().getFirst().status())
                .isEqualTo(RecipeImportPlan.Status.NEW);
    }

    @Test
    void matchingExistingAggregateIsAlreadyImported() {
        var candidate = candidate(
                "paella-valenciana",
                "Paella Valenciana");

        var existing = recipe(
                "paella-valenciana",
                "Paella Valenciana",
                RecipeCategory.INTERNATIONAL);

        var plan = planner.plan(
                List.of(candidate),
                List.of(existing));

        assertThat(plan.alreadyImported()).isEqualTo(1);
        assertThat(plan.entries().getFirst().status())
                .isEqualTo(RecipeImportPlan.Status.ALREADY_IMPORTED);
    }

    @Test
    void slugAndNameMatchingDifferentRecipesIsConflict() {
        var candidate = candidate(
                "paella-valenciana",
                "Paella Valenciana");

        var slugMatch = recipe(
                "paella-valenciana",
                "Otro nombre",
                RecipeCategory.INTERNATIONAL);

        var nameMatch = recipe(
                "otro-slug",
                "Paella Valenciana",
                RecipeCategory.INTERNATIONAL);

        var plan = planner.plan(
                List.of(candidate),
                List.of(slugMatch, nameMatch));

        assertThat(plan.conflicts()).isEqualTo(1);
        assertThat(plan.entries().getFirst().status())
                .isEqualTo(RecipeImportPlan.Status.CONFLICT);
    }

    @Test
    void sameCountsWithDifferentIngredientContentIsPartialState() {
        var candidate = candidate(
                "paella-valenciana",
                "Paella Valenciana");

        var existing = recipe(
                "paella-valenciana",
                "Paella Valenciana",
                RecipeCategory.INTERNATIONAL);

        existing.getIngredients().getFirst()
                .setIngredientName("Ingrediente distinto");

        var plan = planner.plan(
                List.of(candidate),
                List.of(existing));

        assertThat(plan.partialStates()).isEqualTo(1);
        assertThat(plan.entries().getFirst().status())
                .isEqualTo(RecipeImportPlan.Status.PARTIAL_STATE);
    }

    @Test
    void identityMatchWithDifferentAggregateIsPartialState() {
        var candidate = candidate(
                "paella-valenciana",
                "Paella Valenciana");

        var existing = recipe(
                "paella-valenciana",
                "Paella Valenciana",
                RecipeCategory.NATIONAL);

        var plan = planner.plan(
                List.of(candidate),
                List.of(existing));

        assertThat(plan.partialStates()).isEqualTo(1);
        assertThat(plan.entries().getFirst().status())
                .isEqualTo(RecipeImportPlan.Status.PARTIAL_STATE);
    }

    private ReviewedRecipeImportCandidate candidate(
            String slug,
            String name) {

        return new ReviewedRecipeImportCandidate(
                "Recetas.xlsx",
                "37. Paella Valenciana",
                37,
                "test-source-fingerprint",
                name,
                name,
                slug,
                null,
                RecipeCategory.INTERNATIONAL,
                "ES",
                RecipeType.MAIN_COURSE,
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
                        "Preparar la receta.")),
                null,
                false,
                true,
                List.of(),
                List.of(),
                List.of(),
                ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);
    }

    private Recipe recipe(
            String slug,
            String name,
            RecipeCategory category) {

        Recipe recipe = new Recipe(
                slug,
                name,
                null,
                category,
                "ES",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.MEDIUM,
                null,
                RecipeStatus.DRAFT);

        recipe.addIngredient(new RecipeIngredient(
                0,
                "Arroz",
                new BigDecimal("500"),
                null,
                RecipeIngredientUnit.GRAM,
                null,
                "500 gr Arroz"));

        recipe.addStep(new RecipeStep(
                0,
                "Preparar la receta."));

        return recipe;
    }
}
