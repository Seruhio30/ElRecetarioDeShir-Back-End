package com.elrecetariodeshir.backend.excelimport.review;

import java.util.List;

import com.elrecetariodeshir.backend.excelimport.RecipeImportIngredient;
import com.elrecetariodeshir.backend.excelimport.RecipeImportStep;
import com.elrecetariodeshir.backend.excelimport.RecipeImportYield;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record ReviewedRecipeImportCandidate(
        String sourceWorkbook,
        String sourceSheet,
        Integer sourceRecipeNumber,
        String sourceFingerprint,
        String originalName,
        String finalName,
        String slug,
        String cuisine,
        RecipeCategory category,
        String countryCode,
        RecipeType type,
        RecipeDifficulty difficulty,
        Integer timeMinutes,
        RecipeImportYield yield,
        List<RecipeImportIngredient> ingredients,
        List<RecipeImportStep> steps,
        String imageReference,
        boolean imageExists,
        boolean allowMissingImage,
        List<String> originalWarnings,
        List<String> resolvedWarnings,
        List<String> unresolvedWarnings,
        ReviewStatus status) {

    public ReviewedRecipeImportCandidate {
        ingredients = List.copyOf(ingredients);
        steps = List.copyOf(steps);
        originalWarnings = List.copyOf(originalWarnings);
        resolvedWarnings = List.copyOf(resolvedWarnings);
        unresolvedWarnings = List.copyOf(unresolvedWarnings);
    }

    public enum ReviewStatus {
        APPROVED_FOR_IMPORT,
        PENDING_REVIEW
    }
}
