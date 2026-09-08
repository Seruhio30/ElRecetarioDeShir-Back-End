package com.elrecetariodeshir.backend.excelimport;

import java.util.Map;

import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

final class ExcelRecipeApprovedMetadata {

    private static final Map<String, Metadata> BY_NAME = Map.ofEntries(
            Map.entry(
                    "Torta Española",
                    new Metadata("ES", RecipeType.MAIN_COURSE, RecipeDifficulty.EASY, 30)),
            Map.entry(
                    "Quiche Lorraine",
                    new Metadata("FR", RecipeType.MAIN_COURSE, RecipeDifficulty.MEDIUM, null)),
            Map.entry(
                    "Papa a la Huancaína",
                    new Metadata("PE", RecipeType.SIDE_DISH, RecipeDifficulty.EASY, null)),
            Map.entry(
                    "Arroz Chaufa",
                    new Metadata("PE", RecipeType.MAIN_COURSE, RecipeDifficulty.EASY, null)),
            Map.entry(
                    "Causa Limeña",
                    new Metadata("PE", RecipeType.SIDE_DISH, RecipeDifficulty.MEDIUM, null)),
            Map.entry(
                    "Totopos",
                    new Metadata("MX", RecipeType.SIDE_DISH, RecipeDifficulty.EASY, null)),
            Map.entry(
                    "Carne al Pastor",
                    new Metadata("MX", RecipeType.MAIN_COURSE, RecipeDifficulty.MEDIUM, null)),
            Map.entry(
                    "Chilaquiles",
                    new Metadata("MX", RecipeType.MAIN_COURSE, RecipeDifficulty.EASY, null)),
            Map.entry(
                    "Huaraches",
                    new Metadata("MX", RecipeType.MAIN_COURSE, RecipeDifficulty.MEDIUM, null)));

    private ExcelRecipeApprovedMetadata() {
    }

    static Metadata find(String exactName) {
        return exactName == null ? null : BY_NAME.get(exactName);
    }

    record Metadata(
            String countryCode,
            RecipeType type,
            RecipeDifficulty difficulty,
            Integer timeMinutes) {
    }
}
