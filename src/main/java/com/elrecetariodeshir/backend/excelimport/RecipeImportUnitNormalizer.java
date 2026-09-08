package com.elrecetariodeshir.backend.excelimport;

import java.util.Locale;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

public final class RecipeImportUnitNormalizer {

    private RecipeImportUnitNormalizer() {
    }

    public static RecipeIngredientUnit normalizeIngredientUnit(String rawUnit) {
        String unit = normalize(rawUnit);

        return switch (unit) {
            case "gr", "grs" -> RecipeIngredientUnit.GRAM;
            case "kg" -> RecipeIngredientUnit.KILOGRAM;
            case "ml" -> RecipeIngredientUnit.MILLILITER;
            case "litro", "lt" -> RecipeIngredientUnit.LITER;
            case "und", "ud", "unidad", "unidades" -> RecipeIngredientUnit.UNIT;
            case "taza", "tazas" -> RecipeIngredientUnit.CUP;
            case "cda", "cdas", "cucharadas." -> RecipeIngredientUnit.TABLESPOON;
            case "cdta" -> RecipeIngredientUnit.TEASPOON;
            case "paquete", "paquetes" -> RecipeIngredientUnit.PACKAGE;
            case "porción" -> RecipeIngredientUnit.PORTION;
            case "receta" -> RecipeIngredientUnit.RECIPE;
            case "ramas" -> RecipeIngredientUnit.BRANCH;
            case "tallos" -> RecipeIngredientUnit.STALK;
            case "" -> null;
            default -> RecipeIngredientUnit.OTHER;
        };
    }

    public static RecipeYieldUnit normalizeYieldUnit(String rawUnit) {
        String unit = normalize(rawUnit);

        return switch (unit) {
            case "u", "und", "ud", "unidad", "unidades" -> RecipeYieldUnit.UNIT;
            case "gr", "grs" -> RecipeYieldUnit.GRAM;
            case "kg" -> RecipeYieldUnit.KILOGRAM;
            case "ml" -> RecipeYieldUnit.MILLILITER;
            case "litro", "lt" -> RecipeYieldUnit.LITER;
            case "receta" -> RecipeYieldUnit.RECIPE;
            case "" -> null;
            default -> RecipeYieldUnit.OTHER;
        };
    }

    public static boolean isKnownIngredientUnit(String rawUnit) {
        RecipeIngredientUnit unit = normalizeIngredientUnit(rawUnit);
        return unit != null && unit != RecipeIngredientUnit.OTHER;
    }

    public static boolean isKnownYieldUnit(String rawUnit) {
        RecipeYieldUnit unit = normalizeYieldUnit(rawUnit);
        return unit != null && unit != RecipeYieldUnit.OTHER;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
