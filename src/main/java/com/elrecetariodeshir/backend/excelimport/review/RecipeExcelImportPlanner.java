package com.elrecetariodeshir.backend.excelimport.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.elrecetariodeshir.backend.recipe.Recipe;

public final class RecipeExcelImportPlanner {

    public RecipeImportPlan plan(
            List<ReviewedRecipeImportCandidate> candidates,
            List<Recipe> existingRecipes) {

        Map<String, Recipe> bySlug = existingRecipes.stream()
                .filter(recipe -> recipe.getSlug() != null)
                .collect(Collectors.toMap(
                        recipe -> recipe.getSlug().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, ignored) -> first));

        Map<String, Recipe> byName = existingRecipes.stream()
                .filter(recipe -> recipe.getName() != null)
                .collect(Collectors.toMap(
                        recipe -> normalize(recipe.getName()),
                        Function.identity(),
                        (first, ignored) -> first));

        List<RecipeImportPlan.Entry> entries = new ArrayList<>();

        for (ReviewedRecipeImportCandidate candidate : candidates) {
            entries.add(classify(candidate, bySlug, byName));
        }

        return summarize(entries);
    }

    private RecipeImportPlan.Entry classify(
            ReviewedRecipeImportCandidate candidate,
            Map<String, Recipe> bySlug,
            Map<String, Recipe> byName) {

        if (candidate.status()
                != ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT) {
            return new RecipeImportPlan.Entry(
                    candidate,
                    RecipeImportPlan.Status.PENDING_REVIEW,
                    null,
                    "Recipe still requires human review.");
        }

        Recipe slugMatch = bySlug.get(
                candidate.slug().toLowerCase(Locale.ROOT));

        Recipe nameMatch = byName.get(
                normalize(candidate.finalName()));

        if (slugMatch == null && nameMatch == null) {
            return new RecipeImportPlan.Entry(
                    candidate,
                    RecipeImportPlan.Status.NEW,
                    null,
                    "No existing recipe matches slug or name.");
        }

        if (slugMatch != null
                && nameMatch != null
                && !sameRecipe(slugMatch, nameMatch)) {
            return new RecipeImportPlan.Entry(
                    candidate,
                    RecipeImportPlan.Status.CONFLICT,
                    null,
                    "Slug and name match different existing recipes.");
        }

        Recipe existing = slugMatch != null
                ? slugMatch
                : nameMatch;

        boolean slugMatches = candidate.slug()
                .equalsIgnoreCase(existing.getSlug());

        boolean nameMatches = candidate.finalName()
                .equalsIgnoreCase(existing.getName());

        if (!slugMatches || !nameMatches) {
            return new RecipeImportPlan.Entry(
                    candidate,
                    RecipeImportPlan.Status.CONFLICT,
                    existing.getId(),
                    "Existing recipe matches only one identity field.");
        }

        if (aggregateMatches(candidate, existing)) {
            return new RecipeImportPlan.Entry(
                    candidate,
                    RecipeImportPlan.Status.ALREADY_IMPORTED,
                    existing.getId(),
                    "Existing recipe matches approved import candidate.");
        }

        return new RecipeImportPlan.Entry(
                candidate,
                RecipeImportPlan.Status.PARTIAL_STATE,
                existing.getId(),
                "Existing recipe identity matches but aggregate differs.");
    }

    private boolean aggregateMatches(
            ReviewedRecipeImportCandidate candidate,
            Recipe existing) {

        if (existing.getStatus()
                        != com.elrecetariodeshir.backend.recipe.RecipeStatus.DRAFT
                || !java.util.Objects.equals(
                        candidate.cuisine(),
                        existing.getCuisine())
                || existing.getCategory() != candidate.category()
                || !candidate.countryCode().equalsIgnoreCase(existing.getCountryCode())
                || existing.getType() != candidate.type()
                || existing.getDifficulty() != candidate.difficulty()
                || !java.util.Objects.equals(
                        candidate.timeMinutes(),
                        existing.getTime())
                || !yieldMatches(candidate, existing)
                || !ingredientsMatch(candidate, existing)
                || !stepsMatch(candidate, existing)) {
            return false;
        }

        return true;
    }

    private boolean yieldMatches(
            ReviewedRecipeImportCandidate candidate,
            Recipe existing) {

        var source = candidate.yield();

        if (source == null) {
            return existing.getYieldQuantity() == null
                    && existing.getYieldMin() == null
                    && existing.getYieldMax() == null
                    && existing.getYieldUnit() == null
                    && existing.getYieldDisplay() == null;
        }

        return java.util.Objects.equals(
                        source.quantity(),
                        existing.getYieldQuantity())
                && java.util.Objects.equals(
                        source.min(),
                        existing.getYieldMin())
                && java.util.Objects.equals(
                        source.max(),
                        existing.getYieldMax())
                && source.unit() == existing.getYieldUnit()
                && java.util.Objects.equals(
                        source.displayText(),
                        existing.getYieldDisplay());
    }

    private boolean ingredientsMatch(
            ReviewedRecipeImportCandidate candidate,
            Recipe existing) {

        if (existing.getIngredients().size()
                != candidate.ingredients().size()) {
            return false;
        }

        for (int index = 0;
                index < candidate.ingredients().size();
                index++) {

            var source = candidate.ingredients().get(index);
            var actual = existing.getIngredients().get(index);

            if (source.position() != actual.getPosition()
                    || !java.util.Objects.equals(
                            source.ingredientName(),
                            actual.getIngredientName())
                    || !java.util.Objects.equals(
                            source.quantity(),
                            actual.getQuantity())
                    || !java.util.Objects.equals(
                            source.quantityMax(),
                            actual.getQuantityMax())
                    || source.unit() != actual.getUnit()
                    || !java.util.Objects.equals(
                            source.notes(),
                            actual.getNotes())
                    || !java.util.Objects.equals(
                            source.displayText(),
                            actual.getDisplayText())) {
                return false;
            }
        }

        return true;
    }

    private boolean stepsMatch(
            ReviewedRecipeImportCandidate candidate,
            Recipe existing) {

        if (existing.getSteps().size()
                != candidate.steps().size()) {
            return false;
        }

        for (int index = 0;
                index < candidate.steps().size();
                index++) {

            var source = candidate.steps().get(index);
            var actual = existing.getSteps().get(index);

            if (source.position() != actual.getPosition()
                    || !java.util.Objects.equals(
                            source.instruction(),
                            actual.getInstruction())) {
                return false;
            }
        }

        return true;
    }

    private boolean sameRecipe(Recipe first, Recipe second) {
        if (first.getId() != null && second.getId() != null) {
            return first.getId().equals(second.getId());
        }

        return first == second;
    }

    private RecipeImportPlan summarize(
            List<RecipeImportPlan.Entry> entries) {

        int approved = (int) entries.stream()
                .filter(entry -> entry.candidate().status()
                        == ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT)
                .count();

        int pending = count(entries, RecipeImportPlan.Status.PENDING_REVIEW);
        int newRecipes = count(entries, RecipeImportPlan.Status.NEW);
        int alreadyImported = count(
                entries,
                RecipeImportPlan.Status.ALREADY_IMPORTED);
        int conflicts = count(entries, RecipeImportPlan.Status.CONFLICT);
        int partialStates = count(
                entries,
                RecipeImportPlan.Status.PARTIAL_STATE);

        int withImage = (int) entries.stream()
                .filter(entry -> entry.candidate().imageExists())
                .count();

        int withoutImage = entries.size() - withImage;

        int ingredients = entries.stream()
                .mapToInt(entry ->
                        entry.candidate().ingredients().size())
                .sum();

        int steps = entries.stream()
                .mapToInt(entry ->
                        entry.candidate().steps().size())
                .sum();

        return new RecipeImportPlan(
                entries.size(),
                approved,
                pending,
                newRecipes,
                alreadyImported,
                conflicts,
                partialStates,
                withImage,
                withoutImage,
                ingredients,
                steps,
                entries);
    }

    private int count(
            List<RecipeImportPlan.Entry> entries,
            RecipeImportPlan.Status status) {

        return (int) entries.stream()
                .filter(entry -> entry.status() == status)
                .count();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
