package com.elrecetariodeshir.backend.excelimport;

import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record RecipeImportCandidate(
        String sourceWorkbook,
        String sourceSheet,
        Integer sourceRecipeNumber,
        String originalName,
        String normalizedName,
        String slugCandidate,
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
        Boolean imageExists,
        List<String> warnings,
        RecipePreflightStatus status) {
}
