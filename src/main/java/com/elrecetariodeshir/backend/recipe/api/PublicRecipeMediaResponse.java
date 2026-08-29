package com.elrecetariodeshir.backend.recipe.api;

import java.io.InputStream;

public record PublicRecipeMediaResponse(
        InputStream content,
        String mediaType) {
}
