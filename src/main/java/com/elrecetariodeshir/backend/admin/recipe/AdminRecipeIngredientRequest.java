package com.elrecetariodeshir.backend.admin.recipe;

import java.math.BigDecimal;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminRecipeIngredientRequest(
        @Min(0)
        int position,

        @Size(max = 1000)
        String text,

        @Size(max = 300)
        String ingredientName,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal quantity,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal quantityMax,

        RecipeIngredientUnit unit,

        @Size(max = 500)
        String notes,

        @Size(max = 1000)
        String displayText) {
}
