package com.elrecetariodeshir.backend.legacyimport;

import java.nio.file.Path;
import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record PreparedLegacyRecipe(
        String slug,
        String name,
        String cuisine,
        RecipeCategory category,
        String countryCode,
        RecipeType type,
        RecipeDifficulty difficulty,
        Integer time,
        List<String> ingredients,
        List<String> steps,
        Path imagePath,
        String imageExtension,
        String originalFilename,
        String mediaType) {
}
