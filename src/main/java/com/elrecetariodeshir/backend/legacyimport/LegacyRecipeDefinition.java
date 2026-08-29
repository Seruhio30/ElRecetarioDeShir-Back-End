package com.elrecetariodeshir.backend.legacyimport;

import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record LegacyRecipeDefinition(
        String name,
        String slug,
        RecipeType type,
        RecipeDifficulty difficultyOverride) {
}
