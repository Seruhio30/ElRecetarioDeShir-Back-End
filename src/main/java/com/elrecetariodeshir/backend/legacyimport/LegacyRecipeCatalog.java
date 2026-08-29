package com.elrecetariodeshir.backend.legacyimport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

final class LegacyRecipeCatalog {

    static final int EXPECTED_RECIPE_COUNT = 26;

    private static final List<LegacyRecipeDefinition> DEFINITIONS = List.of(
            definition("Paella", "paella", RecipeType.MAIN_COURSE, RecipeDifficulty.MEDIUM),
            definition("Torta Española", "torta-espanola", RecipeType.MAIN_COURSE),
            definition("Coq au Vin", "coq-au-vin", RecipeType.MAIN_COURSE, RecipeDifficulty.HARD),
            definition("Puré de Papa", "pure-de-papa", RecipeType.SIDE_DISH),
            definition("Quiche Lorraine", "quiche-lorraine", RecipeType.MAIN_COURSE),

            definition("Causa Limeña", "causa-limena", RecipeType.SIDE_DISH),
            definition("Arroz Chaufa", "arroz-chaufa", RecipeType.MAIN_COURSE),
            definition("Papa a la Huancaína", "papa-a-la-huancaina", RecipeType.SIDE_DISH),

            definition("Salsa Roja", "salsa-roja", RecipeType.SAUCE),
            definition("Salsa Verde", "salsa-verde", RecipeType.SAUCE),
            definition("Guacamole", "guacamole", RecipeType.SAUCE),
            definition("Totopos", "totopos", RecipeType.SIDE_DISH),
            definition("Carne al Pastor", "carne-al-pastor", RecipeType.MAIN_COURSE),
            definition("Chilaquiles", "chilaquiles", RecipeType.MAIN_COURSE),
            definition("Quesadillas", "quesadillas", RecipeType.MAIN_COURSE),
            definition("Huaraches", "huaraches", RecipeType.MAIN_COURSE),
            definition("Tacos de Birria con Consomé", "tacos-de-birria-con-consome", RecipeType.MAIN_COURSE),
            definition("Sopa Azteca", "sopa-azteca", RecipeType.MAIN_COURSE),

            definition("Arroz con Pollo", "arroz-con-pollo", RecipeType.MAIN_COURSE),
            definition("Picadillo de Papa", "picadillo-de-papa", RecipeType.SIDE_DISH),
            definition("Ensalada Rusa", "ensalada-rusa", RecipeType.SIDE_DISH),
            definition("Enyucado de Carne", "enyucado-de-carne", RecipeType.MAIN_COURSE),

            definition("Arroz para Sushi", "arroz-para-sushi", RecipeType.BASE),
            definition("Salsa Teriyaki", "salsa-teriyaki", RecipeType.SAUCE),
            definition("Salsa de Anguila", "salsa-de-anguila", RecipeType.SAUCE),
            definition("Sushi California Roll", "sushi-california-roll", RecipeType.MAIN_COURSE));

    private static final Map<String, LegacyRecipeDefinition> BY_NAME = buildByName();

    private LegacyRecipeCatalog() {
    }

    static List<LegacyRecipeDefinition> definitions() {
        return DEFINITIONS;
    }

    static LegacyRecipeDefinition definitionFor(String name) {
        return BY_NAME.get(name);
    }

    private static LegacyRecipeDefinition definition(
            String name,
            String slug,
            RecipeType type) {
        return definition(name, slug, type, null);
    }

    private static LegacyRecipeDefinition definition(
            String name,
            String slug,
            RecipeType type,
            RecipeDifficulty difficultyOverride) {
        return new LegacyRecipeDefinition(name, slug, type, difficultyOverride);
    }

    private static Map<String, LegacyRecipeDefinition> buildByName() {
        Map<String, LegacyRecipeDefinition> definitions = new LinkedHashMap<>();

        for (LegacyRecipeDefinition definition : DEFINITIONS) {
            LegacyRecipeDefinition previous = definitions.put(definition.name(), definition);

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate legacy recipe definition: " + definition.name());
            }
        }

        return Map.copyOf(definitions);
    }
}
