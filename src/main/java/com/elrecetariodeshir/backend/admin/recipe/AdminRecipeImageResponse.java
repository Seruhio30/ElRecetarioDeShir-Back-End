package com.elrecetariodeshir.backend.admin.recipe;

import java.time.Instant;

public record AdminRecipeImageResponse(
        Long id,
        String originalFilename,
        String mediaType,
        String altText,
        int position,
        boolean primaryImage,
        Instant createdAt) {
}
