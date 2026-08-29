package com.elrecetariodeshir.backend.recipe.api;

import java.time.Instant;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record RecipeSummaryResponse(
        String slug,
        String name,
        String cuisine,
        RecipeCategory category,
        String countryCode,
        RecipeType type,
        RecipeDifficulty difficulty,
        Integer time,
        RecipeImageResponse primaryImage,
        Instant publishedAt) {
}
