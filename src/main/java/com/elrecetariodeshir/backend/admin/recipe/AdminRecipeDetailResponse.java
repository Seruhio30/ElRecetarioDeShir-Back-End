package com.elrecetariodeshir.backend.admin.recipe;

import java.time.Instant;
import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record AdminRecipeDetailResponse(
        Long id,
        String slug,
        String name,
        String cuisine,
        RecipeCategory category,
        String countryCode,
        RecipeType type,
        RecipeDifficulty difficulty,
        Integer time,
        RecipeStatus status,
        List<AdminRecipeIngredientResponse> ingredients,
        List<AdminRecipeStepResponse> steps,
        List<AdminRecipeImageResponse> images,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant archivedAt) {
}
