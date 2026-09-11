package com.elrecetariodeshir.backend.excelimport.review;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RecipeImportOverrideLoader {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public RecipeImportOverrides load(Path overridesPath) {
        if (overridesPath == null || !Files.isRegularFile(overridesPath)) {
            throw new IllegalArgumentException(
                    "Recipe import overrides file does not exist: "
                            + overridesPath);
        }

        try {
            RecipeImportOverrides overrides =
                    objectMapper.readValue(
                            overridesPath.toFile(),
                            RecipeImportOverrides.class);

            validate(overrides);

            return overrides;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read recipe import overrides: "
                            + overridesPath,
                    exception);
        }
    }

    private void validate(RecipeImportOverrides overrides) {
        if (overrides == null) {
            throw new IllegalArgumentException(
                    "Recipe import overrides are required");
        }

        if (overrides.version() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported recipe import overrides version: "
                            + overrides.version());
        }

        for (var entry : overrides.recipes().entrySet()) {
            String sourceSheet = entry.getKey();
            RecipeImportOverrides.RecipeOverride override =
                    entry.getValue();

            if (sourceSheet == null || sourceSheet.isBlank()) {
                throw new IllegalArgumentException(
                        "Recipe override sourceSheet is required");
            }

            if (override == null) {
                throw new IllegalArgumentException(
                        "Recipe override is missing for sheet: "
                                + sourceSheet);
            }

            validateIngredientOverrides(
                    sourceSheet,
                    override);
        }
    }

    private void validateIngredientOverrides(
            String sourceSheet,
            RecipeImportOverrides.RecipeOverride override) {

        java.util.Set<Integer> positions = new java.util.HashSet<>();

        for (var ingredient : override.ingredients()) {
            if (ingredient.position() < 0) {
                throw new IllegalArgumentException(
                        "Ingredient override position must be non-negative for sheet: "
                                + sourceSheet);
            }

            if (!positions.add(ingredient.position())) {
                throw new IllegalArgumentException(
                        "Duplicate ingredient override position "
                                + ingredient.position()
                                + " for sheet: "
                                + sourceSheet);
            }

            if (ingredient.expectedName() == null
                    || ingredient.expectedName().isBlank()) {
                throw new IllegalArgumentException(
                        "Ingredient override expectedName is required for sheet: "
                                + sourceSheet);
            }
        }
    }
}
