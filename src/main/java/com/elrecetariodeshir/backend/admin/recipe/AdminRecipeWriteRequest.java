package com.elrecetariodeshir.backend.admin.recipe;

import java.math.BigDecimal;
import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;
import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AdminRecipeWriteRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 120)
        String cuisine,

        @NotNull
        RecipeCategory category,

        @NotBlank
        @Pattern(regexp = "(?i)[A-Z]{2}")
        String countryCode,

        @NotNull
        RecipeType type,

        @NotNull
        RecipeDifficulty difficulty,

        @PositiveOrZero
        Integer time,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal yieldQuantity,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal yieldMin,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal yieldMax,

        RecipeYieldUnit yieldUnit,

        @Size(max = 160)
        String yieldDisplay,

        @NotNull
        List<@Valid AdminRecipeIngredientRequest> ingredients,

        @NotNull
        List<@Valid AdminRecipeStepRequest> steps) {
}
