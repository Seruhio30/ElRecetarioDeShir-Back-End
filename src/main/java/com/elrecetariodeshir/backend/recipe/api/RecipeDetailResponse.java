package com.elrecetariodeshir.backend.recipe.api;

import java.time.Instant;
import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record RecipeDetailResponse(
        String slug,
        String name,
        String cuisine,
        RecipeCategory category,
        String countryCode,
        RecipeType type,
        RecipeDifficulty difficulty,
        Integer time,
        List<RecipeIngredientResponse> ingredients,
        List<RecipeStepResponse> steps,
        List<RecipeImageResponse> images,
        Instant publishedAt) {
}
