package com.elrecetariodeshir.backend.recipe.api;

public class PublicRecipeNotFoundException extends RuntimeException {

    public PublicRecipeNotFoundException() {
        super("Recipe not available");
    }
}
