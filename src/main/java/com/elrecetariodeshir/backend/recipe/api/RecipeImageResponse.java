package com.elrecetariodeshir.backend.recipe.api;

public record RecipeImageResponse(
        Long id,
        String url,
        String altText,
        boolean primary) {
}
