package com.elrecetariodeshir.backend.legacyimport;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.elrecetariodeshir.backend.media.storage.MediaStorageService;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeImage;
import com.elrecetariodeshir.backend.recipe.RecipeIngredient;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeStep;

@Service
public class LegacyRecipeImportService {

    private final LegacyRecipePreflightService preflightService;
    private final RecipeRepository recipeRepository;
    private final MediaStorageService mediaStorageService;

    public LegacyRecipeImportService(
            LegacyRecipePreflightService preflightService,
            RecipeRepository recipeRepository,
            MediaStorageService mediaStorageService) {
        this.preflightService = preflightService;
        this.recipeRepository = recipeRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public LegacyRecipeImportResult importRecipes(
            java.nio.file.Path jsonPath,
            java.nio.file.Path assetsRoot) {

        List<PreparedLegacyRecipe> prepared =
                preflightService.prepare(jsonPath, assetsRoot);

        List<Recipe> existing = findRelevantExisting(prepared);

        if (!existing.isEmpty()) {
            return handleExistingState(prepared, existing);
        }

        List<String> createdStorageKeys = new ArrayList<>();
        registerRollbackCleanup(createdStorageKeys);

        Instant publishedAt = Instant.now();
        List<Recipe> recipes = new ArrayList<>(prepared.size());

        for (PreparedLegacyRecipe source : prepared) {
            recipes.add(createAggregate(source, publishedAt, createdStorageKeys));
        }

        recipeRepository.saveAll(recipes);
        recipeRepository.flush();

        verifyImportedAggregates(prepared, recipes);

        return result(
                LegacyRecipeImportResult.Status.IMPORTED,
                recipes);
    }

    private List<Recipe> findRelevantExisting(
            List<PreparedLegacyRecipe> prepared) {

        Set<String> slugs = prepared.stream()
                .map(PreparedLegacyRecipe::slug)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> names = prepared.stream()
                .map(PreparedLegacyRecipe::name)
                .collect(java.util.stream.Collectors.toSet());

        return recipeRepository.findAllBySlugInOrNameIn(slugs, names);
    }

    private LegacyRecipeImportResult handleExistingState(
            List<PreparedLegacyRecipe> prepared,
            List<Recipe> existing) {

        if (existing.size() != LegacyRecipeCatalog.EXPECTED_RECIPE_COUNT) {
            throw new LegacyRecipeImportException(
                    "Legacy recipe database state is partial or conflicting");
        }

        Set<Long> ids = new HashSet<>();

        for (Recipe recipe : existing) {
            if (recipe.getId() == null || !ids.add(recipe.getId())) {
                throw new LegacyRecipeImportException(
                        "Legacy recipe database state is conflicting");
            }
        }

        for (PreparedLegacyRecipe expected : prepared) {
            Recipe actual = existing.stream()
                    .filter(recipe -> expected.slug().equals(recipe.getSlug()))
                    .findFirst()
                    .orElseThrow(() -> new LegacyRecipeImportException(
                            "Legacy recipe database state is partial or conflicting"));

            verifyExistingRecipe(expected, actual);
        }

        return result(
                LegacyRecipeImportResult.Status.NO_OP,
                existing);
    }

    private Recipe createAggregate(
            PreparedLegacyRecipe source,
            Instant publishedAt,
            List<String> createdStorageKeys) {

        Recipe recipe = new Recipe(
                source.slug(),
                source.name(),
                source.cuisine(),
                source.category(),
                source.countryCode(),
                source.type(),
                source.difficulty(),
                source.time(),
                RecipeStatus.PUBLISHED);

        recipe.setPublishedAt(publishedAt);

        for (int position = 0; position < source.ingredients().size(); position++) {
            recipe.addIngredient(new RecipeIngredient(
                    position,
                    source.ingredients().get(position)));
        }

        for (int position = 0; position < source.steps().size(); position++) {
            recipe.addStep(new RecipeStep(
                    position,
                    source.steps().get(position)));
        }

        String storageKey = storeImage(source);
        createdStorageKeys.add(storageKey);

        recipe.addImage(new RecipeImage(
                storageKey,
                source.originalFilename(),
                source.mediaType(),
                source.name(),
                0,
                true));

        return recipe;
    }

    private String storeImage(PreparedLegacyRecipe source) {
        try (InputStream inputStream =
                java.nio.file.Files.newInputStream(source.imagePath())) {
            return mediaStorageService.store(
                    inputStream,
                    source.imageExtension());
        } catch (IOException exception) {
            throw new LegacyRecipeImportException(
                    "Failed to read legacy image for recipe: " + source.name(),
                    exception);
        }
    }

    private void registerRollbackCleanup(List<String> createdStorageKeys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new LegacyRecipeImportException(
                    "Legacy import requires active transaction synchronization");
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            cleanupStorage(createdStorageKeys);
                        }
                    }
                });
    }

    private void cleanupStorage(List<String> createdStorageKeys) {
        for (int index = createdStorageKeys.size() - 1; index >= 0; index--) {
            String storageKey = createdStorageKeys.get(index);

            try {
                mediaStorageService.delete(storageKey);
            } catch (RuntimeException ignored) {
                // Best-effort compensation. Original failure must remain primary.
            }
        }
    }

    private void verifyImportedAggregates(
            List<PreparedLegacyRecipe> expected,
            List<Recipe> actual) {

        if (actual.size() != LegacyRecipeCatalog.EXPECTED_RECIPE_COUNT) {
            throw new LegacyRecipeImportException(
                    "Imported recipe count does not match expected count");
        }

        for (PreparedLegacyRecipe expectedRecipe : expected) {
            Recipe actualRecipe = actual.stream()
                    .filter(recipe -> expectedRecipe.slug().equals(recipe.getSlug()))
                    .findFirst()
                    .orElseThrow(() -> new LegacyRecipeImportException(
                            "Imported recipe missing: " + expectedRecipe.slug()));

            verifyExistingRecipe(expectedRecipe, actualRecipe);
        }
    }

    private void verifyExistingRecipe(
            PreparedLegacyRecipe expected,
            Recipe actual) {

        if (!expected.slug().equals(actual.getSlug())
                || !expected.name().equals(actual.getName())
                || !expected.cuisine().equals(actual.getCuisine())
                || expected.category() != actual.getCategory()
                || !expected.countryCode().equals(actual.getCountryCode())
                || expected.type() != actual.getType()
                || expected.difficulty() != actual.getDifficulty()
                || !java.util.Objects.equals(expected.time(), actual.getTime())
                || actual.getStatus() != RecipeStatus.PUBLISHED
                || actual.getPublishedAt() == null
                || actual.getArchivedAt() != null) {
            throw conflict(expected.slug());
        }

        if (actual.getIngredients().size() != expected.ingredients().size()) {
            throw conflict(expected.slug());
        }

        for (int index = 0; index < expected.ingredients().size(); index++) {
            RecipeIngredient ingredient = actual.getIngredients().get(index);

            if (ingredient.getPosition() != index
                    || !expected.ingredients().get(index).equals(ingredient.getText())) {
                throw conflict(expected.slug());
            }
        }

        if (actual.getSteps().size() != expected.steps().size()) {
            throw conflict(expected.slug());
        }

        for (int index = 0; index < expected.steps().size(); index++) {
            RecipeStep step = actual.getSteps().get(index);

            if (step.getPosition() != index
                    || !expected.steps().get(index).equals(step.getInstruction())) {
                throw conflict(expected.slug());
            }
        }

        if (actual.getImages().size() != 1) {
            throw conflict(expected.slug());
        }

        RecipeImage image = actual.getImages().getFirst();

        if (image.getPosition() != 0
                || !image.isPrimaryImage()
                || !expected.originalFilename().equals(image.getOriginalFilename())
                || !expected.mediaType().equals(image.getMediaType())
                || !expected.name().equals(image.getAltText())
                || image.getStorageKey() == null
                || image.getStorageKey().isBlank()
                || !mediaStorageService.exists(image.getStorageKey())) {
            throw conflict(expected.slug());
        }
    }

    private LegacyRecipeImportException conflict(String slug) {
        return new LegacyRecipeImportException(
                "Legacy recipe database state conflicts with expected import: " + slug);
    }

    private LegacyRecipeImportResult result(
            LegacyRecipeImportResult.Status status,
            List<Recipe> recipes) {

        int ingredientCount = recipes.stream()
                .mapToInt(recipe -> recipe.getIngredients().size())
                .sum();

        int stepCount = recipes.stream()
                .mapToInt(recipe -> recipe.getSteps().size())
                .sum();

        int imageCount = recipes.stream()
                .mapToInt(recipe -> recipe.getImages().size())
                .sum();

        return new LegacyRecipeImportResult(
                status,
                recipes.size(),
                ingredientCount,
                stepCount,
                imageCount);
    }
}
