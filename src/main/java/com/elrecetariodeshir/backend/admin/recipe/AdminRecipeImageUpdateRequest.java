package com.elrecetariodeshir.backend.admin.recipe;

import jakarta.validation.constraints.Size;

public record AdminRecipeImageUpdateRequest(
        @Size(max = 500)
        String altText,
        Boolean primaryImage) {
}
