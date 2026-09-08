package com.elrecetariodeshir.backend.excelimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;

public final class RecipeIngredientNormalizer {

    private RecipeIngredientNormalizer() {
    }

    public static Result normalize(
            String ingredientName,
            Object rawQuantity,
            String rawUnit,
            int position) {

        List<String> warnings = new ArrayList<>();

        String normalizedName = ingredientName == null
                ? ""
                : ingredientName.trim();

        var parsedQuantity = RecipeImportQuantityParser.parse(rawQuantity);
        RecipeIngredientUnit unit =
                RecipeImportUnitNormalizer.normalizeIngredientUnit(rawUnit);

        if (normalizedName.isBlank()) {
            warnings.add("Ingredient name is missing.");
        }

        if (!parsedQuantity.parsed()) {
            if (rawQuantity == null
                    || rawQuantity.toString().trim().isEmpty()) {
                warnings.add("Ingredient quantity is missing.");
            } else {
                warnings.add(
                        "Ingredient quantity could not be parsed: "
                                + rawQuantity.toString().trim());
            }
        }

        if (parsedQuantity.quantity() != null
                && parsedQuantity.quantity().compareTo(BigDecimal.ZERO) == 0) {
            warnings.add("Ingredient quantity is zero and requires review.");
        }

        if (unit == null) {
            warnings.add("Ingredient unit is missing.");
        } else if (unit == RecipeIngredientUnit.OTHER) {
            warnings.add("Unknown ingredient unit: "
                    + (rawUnit == null ? "" : rawUnit.trim()));
        }

        String displayText = buildDisplayText(
                rawQuantity,
                rawUnit,
                normalizedName);

        boolean deterministic = !normalizedName.isBlank()
                && parsedQuantity.parsed()
                && parsedQuantity.quantity() != null
                && parsedQuantity.quantity().compareTo(BigDecimal.ZERO) > 0
                && unit != null
                && unit != RecipeIngredientUnit.OTHER;

        return new Result(
                new RecipeImportIngredient(
                        normalizedName,
                        parsedQuantity.quantity(),
                        parsedQuantity.quantityMax(),
                        unit,
                        null,
                        displayText,
                        position,
                        warnings),
                warnings,
                deterministic);
    }

    private static String buildDisplayText(
            Object rawQuantity,
            String rawUnit,
            String ingredientName) {

        String quantity = rawQuantity == null
                ? ""
                : rawQuantity.toString().trim();

        String unit = rawUnit == null
                ? ""
                : rawUnit.trim();

        return String.join(
                " ",
                List.of(quantity, unit, ingredientName).stream()
                        .filter(value -> value != null && !value.isBlank())
                        .toList());
    }

    public record Result(
            RecipeImportIngredient ingredient,
            List<String> warnings,
            boolean deterministic) {

        public Result {
            warnings = List.copyOf(warnings);
        }
    }
}
