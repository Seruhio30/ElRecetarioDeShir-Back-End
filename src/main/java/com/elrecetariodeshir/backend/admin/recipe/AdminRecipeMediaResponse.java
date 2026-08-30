package com.elrecetariodeshir.backend.admin.recipe;

import java.io.InputStream;

public record AdminRecipeMediaResponse(
        InputStream content,
        String mediaType) {
}
