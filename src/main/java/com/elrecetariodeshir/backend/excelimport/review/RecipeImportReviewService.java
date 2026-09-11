package com.elrecetariodeshir.backend.excelimport.review;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.elrecetariodeshir.backend.excelimport.RecipeImportCandidate;
import com.elrecetariodeshir.backend.excelimport.RecipeImportIngredient;
import com.elrecetariodeshir.backend.recipe.RecipeSlugifier;

public final class RecipeImportReviewService {

    private static final String CATEGORY_WARNING =
            "Recipe category requires review.";
    private static final String COUNTRY_WARNING =
            "Recipe countryCode requires review.";
    private static final String TYPE_WARNING =
            "Recipe type requires review.";
    private static final String DIFFICULTY_WARNING =
            "Recipe difficulty requires review.";
    private static final String MISSING_IMAGE_WARNING =
            "Recipe image is missing.";

    public List<ReviewedRecipeImportCandidate> review(
            List<RecipeImportCandidate> candidates,
            RecipeImportOverrides overrides) {

        validateOverrideSheets(candidates, overrides);

        return candidates.stream()
                .map(candidate -> reviewCandidate(
                        candidate,
                        overrides.recipes().get(candidate.sourceSheet())))
                .toList();
    }

    private ReviewedRecipeImportCandidate reviewCandidate(
            RecipeImportCandidate candidate,
            RecipeImportOverrides.RecipeOverride override) {

        if (override == null) {
            return pendingWithoutOverride(candidate);
        }

        String finalName = hasText(override.name())
                ? override.name().trim()
                : candidate.normalizedName();

        String cuisine = hasText(override.cuisine())
                ? override.cuisine().trim()
                : candidate.cuisine();

        var category = override.category() != null
                ? override.category()
                : candidate.category();

        String countryCode = hasText(override.countryCode())
                ? override.countryCode().trim().toUpperCase(Locale.ROOT)
                : candidate.countryCode();

        var type = override.type() != null
                ? override.type()
                : candidate.type();

        var difficulty = override.difficulty() != null
                ? override.difficulty()
                : candidate.difficulty();

        Integer timeMinutes = override.timeMinutes() != null
                ? override.timeMinutes()
                : candidate.timeMinutes();

        List<RecipeImportIngredient> ingredients =
                applyIngredientOverrides(candidate, override);

        Set<String> resolved = new HashSet<>(
                override.resolvedWarnings());

        resolveMetadataWarnings(
                candidate,
                category,
                countryCode,
                type,
                difficulty,
                resolved);

        if (Boolean.TRUE.equals(override.allowMissingImage())) {
            resolved.add(MISSING_IMAGE_WARNING);
        }

        if (hasText(override.name())) {
            candidate.warnings().stream()
                    .filter(warning -> warning.startsWith(
                            "Sheet recipe name does not match internal recipe name:"))
                    .forEach(resolved::add);
        }

        resolveIngredientWarnings(
                override,
                resolved);

        List<String> unresolved = candidate.warnings().stream()
                .filter(warning -> !resolved.contains(warning))
                .toList();

        boolean approved = Boolean.TRUE.equals(override.approved())
                && hasText(finalName)
                && category != null
                && hasText(countryCode)
                && countryCode.length() == 2
                && type != null
                && difficulty != null
                && !ingredients.isEmpty()
                && !candidate.steps().isEmpty()
                && unresolved.isEmpty();

        return new ReviewedRecipeImportCandidate(
                candidate.sourceWorkbook(),
                candidate.sourceSheet(),
                candidate.sourceRecipeNumber(),
                RecipeImportFingerprint.from(candidate),
                candidate.originalName(),
                finalName,
                RecipeSlugifier.slugify(finalName),
                cuisine,
                category,
                countryCode,
                type,
                difficulty,
                timeMinutes,
                candidate.yield(),
                ingredients,
                candidate.steps(),
                candidate.imageReference(),
                Boolean.TRUE.equals(candidate.imageExists()),
                Boolean.TRUE.equals(override.allowMissingImage()),
                candidate.warnings(),
                resolved.stream().sorted().toList(),
                unresolved,
                approved
                        ? ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT
                        : ReviewedRecipeImportCandidate.ReviewStatus.PENDING_REVIEW);
    }

    private ReviewedRecipeImportCandidate pendingWithoutOverride(
            RecipeImportCandidate candidate) {

        return new ReviewedRecipeImportCandidate(
                candidate.sourceWorkbook(),
                candidate.sourceSheet(),
                candidate.sourceRecipeNumber(),
                RecipeImportFingerprint.from(candidate),
                candidate.originalName(),
                candidate.normalizedName(),
                candidate.slugCandidate(),
                candidate.cuisine(),
                candidate.category(),
                candidate.countryCode(),
                candidate.type(),
                candidate.difficulty(),
                candidate.timeMinutes(),
                candidate.yield(),
                candidate.ingredients(),
                candidate.steps(),
                candidate.imageReference(),
                Boolean.TRUE.equals(candidate.imageExists()),
                false,
                candidate.warnings(),
                List.of(),
                candidate.warnings(),
                ReviewedRecipeImportCandidate.ReviewStatus.PENDING_REVIEW);
    }

    private List<RecipeImportIngredient> applyIngredientOverrides(
            RecipeImportCandidate candidate,
            RecipeImportOverrides.RecipeOverride override) {

        List<RecipeImportIngredient> ingredients =
                new ArrayList<>(candidate.ingredients());

        for (var ingredientOverride : override.ingredients()) {
            int position = ingredientOverride.position();

            if (position >= ingredients.size()) {
                throw new RecipeImportReviewException(
                        "Ingredient override position "
                                + position
                                + " is outside recipe ingredients for sheet: "
                                + candidate.sourceSheet());
            }

            RecipeImportIngredient current = ingredients.get(position);

            if (!current.ingredientName()
                    .equalsIgnoreCase(ingredientOverride.expectedName().trim())) {
                throw new RecipeImportReviewException(
                        "Ingredient override expectedName mismatch for sheet "
                                + candidate.sourceSheet()
                                + " at position "
                                + position
                                + ": expected "
                                + ingredientOverride.expectedName()
                                + " but found "
                                + current.ingredientName());
            }

            ingredients.set(
                    position,
                    new RecipeImportIngredient(
                            current.ingredientName(),
                            Boolean.TRUE.equals(ingredientOverride.clearQuantity())
                                    ? null
                                    : ingredientOverride.quantity() != null
                                            ? ingredientOverride.quantity()
                                            : current.quantity(),
                            Boolean.TRUE.equals(ingredientOverride.clearQuantity())
                                    ? null
                                    : ingredientOverride.quantityMax() != null
                                            ? ingredientOverride.quantityMax()
                                            : current.quantityMax(),
                            Boolean.TRUE.equals(ingredientOverride.clearUnit())
                                    ? null
                                    : ingredientOverride.unit() != null
                                            ? ingredientOverride.unit()
                                            : current.unit(),
                            ingredientOverride.notes() != null
                                    ? ingredientOverride.notes()
                                    : current.notes(),
                            hasText(ingredientOverride.displayText())
                                    ? ingredientOverride.displayText().trim()
                                    : current.displayText(),
                            current.position(),
                            current.warnings()));
        }

        return List.copyOf(ingredients);
    }

    private void resolveMetadataWarnings(
            RecipeImportCandidate candidate,
            Object category,
            String countryCode,
            Object type,
            Object difficulty,
            Set<String> resolved) {

        if (category != null && candidate.warnings().contains(CATEGORY_WARNING)) {
            resolved.add(CATEGORY_WARNING);
        }

        if (hasText(countryCode)
                && candidate.warnings().contains(COUNTRY_WARNING)) {
            resolved.add(COUNTRY_WARNING);
        }

        if (type != null && candidate.warnings().contains(TYPE_WARNING)) {
            resolved.add(TYPE_WARNING);
        }

        if (difficulty != null
                && candidate.warnings().contains(DIFFICULTY_WARNING)) {
            resolved.add(DIFFICULTY_WARNING);
        }
    }

    private void resolveIngredientWarnings(
            RecipeImportOverrides.RecipeOverride override,
            Set<String> resolved) {

        for (var ingredient : override.ingredients()) {
            for (String warning : ingredient.resolvedWarnings()) {
                resolved.add(
                        "Ingredient "
                                + ingredient.expectedName().trim()
                                + ": "
                                + warning);
            }
        }
    }

    private void validateOverrideSheets(
            List<RecipeImportCandidate> candidates,
            RecipeImportOverrides overrides) {

        Set<String> knownSheets = candidates.stream()
                .map(RecipeImportCandidate::sourceSheet)
                .collect(java.util.stream.Collectors.toSet());

        for (String sourceSheet : overrides.recipes().keySet()) {
            if (!knownSheets.contains(sourceSheet)) {
                throw new RecipeImportReviewException(
                        "Recipe override references unknown sheet: "
                                + sourceSheet);
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
