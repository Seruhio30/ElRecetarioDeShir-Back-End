package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.excelimport.RecipeImportCandidate;
import com.elrecetariodeshir.backend.excelimport.RecipeImportIngredient;
import com.elrecetariodeshir.backend.excelimport.RecipeImportStep;
import com.elrecetariodeshir.backend.excelimport.RecipePreflightStatus;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeType;

class RecipeImportReviewServiceTests {

    private final RecipeImportReviewService service =
            new RecipeImportReviewService();

    @Test
    void missingMetadataKeepsCandidatePending() {
        var candidate = candidate(
                List.of(
                        "Recipe category requires review.",
                        "Recipe countryCode requires review.",
                        "Recipe type requires review."));

        var reviewed = service.review(
                List.of(candidate),
                new RecipeImportOverrides(1, Map.of()))
                .getFirst();

        assertThat(reviewed.status())
                .isEqualTo(
                        ReviewedRecipeImportCandidate.ReviewStatus.PENDING_REVIEW);

        assertThat(reviewed.unresolvedWarnings())
                .containsExactlyElementsOf(candidate.warnings());
    }

    @Test
    void completeOverrideApprovesCandidate() {
        var candidate = candidate(
                List.of(
                        "Recipe category requires review.",
                        "Recipe countryCode requires review.",
                        "Recipe type requires review."));

        var override = new RecipeImportOverrides.RecipeOverride(
                true,
                null,
                null,
                RecipeCategory.INTERNATIONAL,
                "ES",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.MEDIUM,
                null,
                false,
                List.of(),
                List.of());

        var reviewed = service.review(
                List.of(candidate),
                new RecipeImportOverrides(
                        1,
                        Map.of(candidate.sourceSheet(), override)))
                .getFirst();

        assertThat(reviewed.status())
                .isEqualTo(
                        ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);

        assertThat(reviewed.category())
                .isEqualTo(RecipeCategory.INTERNATIONAL);
        assertThat(reviewed.countryCode()).isEqualTo("ES");
        assertThat(reviewed.type())
                .isEqualTo(RecipeType.MAIN_COURSE);
        assertThat(reviewed.unresolvedWarnings()).isEmpty();
    }

    @Test
    void unresolvedWarningBlocksApproval() {
        var candidate = candidate(
                List.of(
                        "Recipe category requires review.",
                        "Unexpected structural warning."));

        var override = new RecipeImportOverrides.RecipeOverride(
                true,
                null,
                null,
                RecipeCategory.INTERNATIONAL,
                "ES",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.MEDIUM,
                null,
                false,
                List.of(),
                List.of());

        var reviewed = service.review(
                List.of(candidate),
                new RecipeImportOverrides(
                        1,
                        Map.of(candidate.sourceSheet(), override)))
                .getFirst();

        assertThat(reviewed.status())
                .isEqualTo(
                        ReviewedRecipeImportCandidate.ReviewStatus.PENDING_REVIEW);

        assertThat(reviewed.unresolvedWarnings())
                .contains("Unexpected structural warning.");
    }

    @Test
    void ingredientOverrideResolvesAmbiguousUnit() {
        var candidate = candidateWithIngredientWarning(
                "Ingredient Agua: Unknown ingredient unit: la");

        var ingredientOverride =
                new RecipeImportOverrides.IngredientOverride(
                        0,
                        "Agua",
                        null,
                        null,
                        RecipeIngredientUnit.LITER,
                        false,
                        false,
                        null,
                        "1 litro Agua",
                        List.of("Unknown ingredient unit: la"));

        var override = approvedOverride(
                List.of(ingredientOverride),
                false);

        var reviewed = service.review(
                List.of(candidate),
                new RecipeImportOverrides(
                        1,
                        Map.of(candidate.sourceSheet(), override)))
                .getFirst();

        assertThat(reviewed.status())
                .isEqualTo(
                        ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);

        assertThat(reviewed.ingredients().getFirst().unit())
                .isEqualTo(RecipeIngredientUnit.LITER);

        assertThat(reviewed.ingredients().getFirst().displayText())
                .isEqualTo("1 litro Agua");

        assertThat(reviewed.unresolvedWarnings()).isEmpty();
    }

    @Test
    void ingredientOverrideCanExplicitlyClearQuantityAndUnit() {
        var candidate = candidateWithIngredientWarning(
                "Ingredient Agua: Unknown ingredient unit: la");

        var ingredientOverride =
                new RecipeImportOverrides.IngredientOverride(
                        0,
                        "Agua",
                        null,
                        null,
                        null,
                        true,
                        true,
                        null,
                        "Agua",
                        List.of("Unknown ingredient unit: la"));

        var override = approvedOverride(
                List.of(ingredientOverride),
                false);

        var reviewed = service.review(
                List.of(candidate),
                new RecipeImportOverrides(
                        1,
                        Map.of(candidate.sourceSheet(), override)))
                .getFirst();

        var ingredient = reviewed.ingredients().getFirst();

        assertThat(ingredient.quantity()).isNull();
        assertThat(ingredient.quantityMax()).isNull();
        assertThat(ingredient.unit()).isNull();
        assertThat(ingredient.displayText()).isEqualTo("Agua");
        assertThat(reviewed.unresolvedWarnings()).isEmpty();
        assertThat(reviewed.status())
                .isEqualTo(
                        ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);
    }

    @Test
    void approvedMissingImageCanRemainImportable() {
        var candidate = candidate(
                List.of(
                        "Recipe category requires review.",
                        "Recipe image is missing."));

        var override = approvedOverride(
                List.of(),
                true);

        var reviewed = service.review(
                List.of(candidate),
                new RecipeImportOverrides(
                        1,
                        Map.of(candidate.sourceSheet(), override)))
                .getFirst();

        assertThat(reviewed.allowMissingImage()).isTrue();
        assertThat(reviewed.unresolvedWarnings()).isEmpty();
        assertThat(reviewed.status())
                .isEqualTo(
                        ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT);
    }

    private RecipeImportOverrides.RecipeOverride approvedOverride(
            List<RecipeImportOverrides.IngredientOverride> ingredients,
            boolean allowMissingImage) {

        return new RecipeImportOverrides.RecipeOverride(
                true,
                null,
                null,
                RecipeCategory.INTERNATIONAL,
                "ES",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.MEDIUM,
                null,
                allowMissingImage,
                List.of(),
                ingredients);
    }

    private RecipeImportCandidate candidate(List<String> warnings) {
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
                        "Agua",
                        BigDecimal.ONE,
                        null,
                        RecipeIngredientUnit.LITER,
                        null,
                        "1 litro Agua",
                        0,
                        List.of())),
                List.of(new RecipeImportStep(
                        0,
                        "Preparar la receta.")),
                null,
                false,
                warnings,
                RecipePreflightStatus.REVIEW_REQUIRED);
    }

    private RecipeImportCandidate candidateWithIngredientWarning(
            String warning) {

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
                        "Agua",
                        BigDecimal.ONE,
                        null,
                        RecipeIngredientUnit.OTHER,
                        null,
                        "1 la Agua",
                        0,
                        List.of("Unknown ingredient unit: la"))),
                List.of(new RecipeImportStep(
                        0,
                        "Preparar la receta.")),
                null,
                true,
                List.of(
                        "Recipe category requires review.",
                        "Recipe countryCode requires review.",
                        "Recipe type requires review.",
                        warning),
                RecipePreflightStatus.REVIEW_REQUIRED);
    }
}
