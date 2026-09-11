package com.elrecetariodeshir.backend.excelimport.review;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.elrecetariodeshir.backend.excelimport.ExcelRecipeEmbeddedImageReader;
import com.elrecetariodeshir.backend.excelimport.RecipeImportIngredient;
import com.elrecetariodeshir.backend.media.storage.MediaStorageService;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeImage;
import com.elrecetariodeshir.backend.recipe.RecipeIngredient;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeStep;

@Service
public class RecipeExcelImportService {

    private final RecipeRepository recipeRepository;
    private final MediaStorageService mediaStorageService;
    private final ExcelRecipeEmbeddedImageReader imageReader =
            new ExcelRecipeEmbeddedImageReader();
    private final RecipeExcelImportPlanner planner =
            new RecipeExcelImportPlanner();

    public RecipeExcelImportService(
            RecipeRepository recipeRepository,
            MediaStorageService mediaStorageService) {
        this.recipeRepository = recipeRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public RecipeExcelImportResult importApproved(
            Path workbookPath,
            List<ReviewedRecipeImportCandidate> candidates) {

        List<ReviewedRecipeImportCandidate> approved = candidates.stream()
                .filter(candidate -> candidate.status()
                        == ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT)
                .toList();

        Set<String> slugs = approved.stream()
                .map(ReviewedRecipeImportCandidate::slug)
                .collect(Collectors.toSet());

        Set<String> names = approved.stream()
                .map(ReviewedRecipeImportCandidate::finalName)
                .collect(Collectors.toSet());

        List<Recipe> existing = approved.isEmpty()
                ? List.of()
                : recipeRepository.findAllBySlugInOrNameIn(slugs, names);

        RecipeImportPlan plan = planner.plan(candidates, existing);

        if (plan.conflicts() > 0 || plan.partialStates() > 0) {
            throw new RecipeImportReviewException(
                    "Recipe import contains conflicts or partial states");
        }

        List<String> createdStorageKeys = new ArrayList<>();
        registerRollbackCleanup(createdStorageKeys);

        List<Recipe> toImport = new ArrayList<>();

        for (RecipeImportPlan.Entry entry : plan.entries()) {
            if (entry.status() == RecipeImportPlan.Status.NEW) {
                toImport.add(createAggregate(
                        workbookPath,
                        entry.candidate(),
                        createdStorageKeys));
            }
        }

        if (!toImport.isEmpty()) {
            recipeRepository.saveAll(toImport);
            recipeRepository.flush();
            verifyStoredImages(toImport);
        }

        return new RecipeExcelImportResult(
                toImport.size(),
                plan.alreadyImported(),
                plan.pending(),
                plan.conflicts(),
                plan.partialStates());
    }

    private Recipe createAggregate(
            Path workbookPath,
            ReviewedRecipeImportCandidate source,
            List<String> createdStorageKeys) {

        Recipe recipe = new Recipe(
                source.slug(),
                source.finalName(),
                source.cuisine(),
                source.category(),
                source.countryCode(),
                source.type(),
                source.difficulty(),
                source.timeMinutes(),
                RecipeStatus.DRAFT);

        applyYield(source, recipe);

        for (RecipeImportIngredient ingredient : source.ingredients()) {
            recipe.addIngredient(new RecipeIngredient(
                    ingredient.position(),
                    ingredient.ingredientName(),
                    ingredient.quantity(),
                    ingredient.quantityMax(),
                    ingredient.unit(),
                    ingredient.notes(),
                    ingredient.displayText()));
        }

        source.steps().forEach(step ->
                recipe.addStep(new RecipeStep(
                        step.position(),
                        step.instruction())));

        addImageIfPresent(
                workbookPath,
                source,
                recipe,
                createdStorageKeys);

        return recipe;
    }

    private void addImageIfPresent(
            Path workbookPath,
            ReviewedRecipeImportCandidate source,
            Recipe recipe,
            List<String> createdStorageKeys) {

        if (!source.imageExists()) {
            if (source.allowMissingImage()) {
                return;
            }

            throw new RecipeImportReviewException(
                    "Approved recipe requires an image: "
                            + source.sourceSheet());
        }

        var image = imageReader.read(
                workbookPath,
                source.sourceSheet(),
                source.imageReference());

        if (image == null) {
            throw new RecipeImportReviewException(
                    "Approved recipe image could not be resolved: "
                            + source.sourceSheet());
        }

        String storageKey = mediaStorageService.store(
                new ByteArrayInputStream(image.bytes()),
                image.extension());

        createdStorageKeys.add(storageKey);

        recipe.addImage(new RecipeImage(
                storageKey,
                source.slug() + "." + image.extension(),
                image.mediaType(),
                source.finalName(),
                0,
                true));
    }

    private void registerRollbackCleanup(
            List<String> createdStorageKeys) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new RecipeImportReviewException(
                    "Recipe Excel import requires active transaction synchronization");
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

    private void verifyStoredImages(
            List<Recipe> recipes) {

        for (Recipe recipe : recipes) {
            for (RecipeImage image : recipe.getImages()) {
                if (!mediaStorageService.exists(image.getStorageKey())) {
                    throw new RecipeImportReviewException(
                            "Stored recipe image could not be verified: "
                                    + recipe.getSlug());
                }
            }
        }
    }

    private void cleanupStorage(
            List<String> createdStorageKeys) {

        for (int index = createdStorageKeys.size() - 1;
                index >= 0;
                index--) {

            try {
                mediaStorageService.delete(
                        createdStorageKeys.get(index));
            } catch (RuntimeException ignored) {
                // Best-effort compensation. Original failure stays primary.
            }
        }
    }

    private void applyYield(
            ReviewedRecipeImportCandidate source,
            Recipe recipe) {

        if (source.yield() == null) {
            return;
        }

        recipe.setYieldQuantity(source.yield().quantity());
        recipe.setYieldMin(source.yield().min());
        recipe.setYieldMax(source.yield().max());
        recipe.setYieldUnit(source.yield().unit());
        recipe.setYieldDisplay(source.yield().displayText());
    }
}
