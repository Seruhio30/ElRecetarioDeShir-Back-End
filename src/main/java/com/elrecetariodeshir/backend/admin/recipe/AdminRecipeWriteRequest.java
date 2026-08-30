package com.elrecetariodeshir.backend.admin.recipe;

import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

import jakarta.validation.Valid;
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

        @NotNull
        List<@Valid AdminRecipeIngredientRequest> ingredients,

        @NotNull
        List<@Valid AdminRecipeStepRequest> steps) {
}
