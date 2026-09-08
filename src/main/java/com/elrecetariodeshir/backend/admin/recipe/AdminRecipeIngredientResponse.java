package com.elrecetariodeshir.backend.admin.recipe;

import java.math.BigDecimal;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;

public record AdminRecipeIngredientResponse(
        int position,
        String text,
        String ingredientName,
        BigDecimal quantity,
        BigDecimal quantityMax,
        RecipeIngredientUnit unit,
        String notes,
        String displayText) {
}
