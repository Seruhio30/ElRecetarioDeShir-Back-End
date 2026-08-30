package com.elrecetariodeshir.backend.admin.recipe;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdminRecipeStepRequest(
        @Min(0)
        int position,

        @NotBlank
        String instruction) {
}
