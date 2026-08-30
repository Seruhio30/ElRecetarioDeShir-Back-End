package com.elrecetariodeshir.backend.admin.recipe;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRecipeIngredientRequest(
        @Min(0)
        int position,

        @NotBlank
        @Size(max = 1000)
        String text) {
}
