package com.elrecetariodeshir.backend.excelimport.review;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public record RecipeImportOverrides(
        int version,
        Map<String, RecipeOverride> recipes) {

    public RecipeImportOverrides {
        recipes = recipes == null ? Map.of() : Map.copyOf(recipes);
    }

    public record RecipeOverride(
            Boolean approved,
            String name,
            String cuisine,
            RecipeCategory category,
            String countryCode,
            RecipeType type,
            RecipeDifficulty difficulty,
            Integer timeMinutes,
            Boolean allowMissingImage,
            List<String> resolvedWarnings,
            List<IngredientOverride> ingredients) {

        public RecipeOverride {
            resolvedWarnings = resolvedWarnings == null
                    ? List.of()
                    : List.copyOf(resolvedWarnings);

            ingredients = ingredients == null
                    ? List.of()
                    : List.copyOf(ingredients);
        }
    }

    public record IngredientOverride(
            int position,
            String expectedName,
            BigDecimal quantity,
            BigDecimal quantityMax,
            RecipeIngredientUnit unit,
            Boolean clearQuantity,
            Boolean clearUnit,
            String notes,
            String displayText,
            List<String> resolvedWarnings) {

        public IngredientOverride {
            resolvedWarnings = resolvedWarnings == null
                    ? List.of()
                    : List.copyOf(resolvedWarnings);
        }
    }
}
