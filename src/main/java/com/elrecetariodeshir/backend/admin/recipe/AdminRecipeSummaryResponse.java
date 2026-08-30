package com.elrecetariodeshir.backend.admin.recipe;

import java.time.Instant;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record AdminRecipeSummaryResponse(
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
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant archivedAt) {
}
