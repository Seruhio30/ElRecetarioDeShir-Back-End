package com.elrecetariodeshir.backend.excelimport;

import java.math.BigDecimal;

import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

public record RecipeImportYield(
        BigDecimal quantity,
        BigDecimal min,
        BigDecimal max,
        RecipeYieldUnit unit,
        String displayText) {
}
