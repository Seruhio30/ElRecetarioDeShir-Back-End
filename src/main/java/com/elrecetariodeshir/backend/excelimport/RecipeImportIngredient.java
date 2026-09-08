package com.elrecetariodeshir.backend.excelimport;

import java.math.BigDecimal;
import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;

public record RecipeImportIngredient(
        String ingredientName,
        BigDecimal quantity,
        BigDecimal quantityMax,
        RecipeIngredientUnit unit,
        String notes,
        String displayText,
        int position,
        List<String> warnings) {
}
