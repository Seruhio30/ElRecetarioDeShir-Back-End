package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeType;

class RecipeImportOverrideLoaderTests {

    private final RecipeImportOverrideLoader loader =
            new RecipeImportOverrideLoader();

    @TempDir
    Path tempDir;

    @Test
    void loadsValidOverrides() throws Exception {
        Path file = tempDir.resolve("recipe-import-overrides.json");

        Files.writeString(file, """
                {
                  "version": 1,
                  "recipes": {
                    "37. Paella Valenciana": {
                      "approved": true,
                      "category": "INTERNATIONAL",
                      "countryCode": "ES",
                      "type": "MAIN_COURSE",
                      "difficulty": "MEDIUM",
                      "ingredients": [
                        {
                          "position": 4,
                          "expectedName": "Agua",
                          "unit": "LITER",
                          "resolvedWarnings": [
                            "Unknown ingredient unit: la"
                          ]
                        }
                      ]
                    }
                  }
                }
                """);

        RecipeImportOverrides overrides = loader.load(file);

        var recipe = overrides.recipes()
                .get("37. Paella Valenciana");

        assertThat(overrides.version()).isEqualTo(1);
        assertThat(recipe.approved()).isTrue();
        assertThat(recipe.category())
                .isEqualTo(RecipeCategory.INTERNATIONAL);
        assertThat(recipe.countryCode()).isEqualTo("ES");
        assertThat(recipe.type())
                .isEqualTo(RecipeType.MAIN_COURSE);
        assertThat(recipe.difficulty())
                .isEqualTo(RecipeDifficulty.MEDIUM);

        assertThat(recipe.ingredients()).hasSize(1);
        assertThat(recipe.ingredients().getFirst().unit())
                .isEqualTo(RecipeIngredientUnit.LITER);
    }

    @Test
    void rejectsUnsupportedVersion() throws Exception {
        Path file = tempDir.resolve("recipe-import-overrides.json");

        Files.writeString(file, """
                {
                  "version": 2,
                  "recipes": {}
                }
                """);

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Unsupported recipe import overrides version");
    }

    @Test
    void rejectsDuplicateIngredientPositions() throws Exception {
        Path file = tempDir.resolve("recipe-import-overrides.json");

        Files.writeString(file, """
                {
                  "version": 1,
                  "recipes": {
                    "37. Paella Valenciana": {
                      "ingredients": [
                        {
                          "position": 4,
                          "expectedName": "Agua"
                        },
                        {
                          "position": 4,
                          "expectedName": "Agua"
                        }
                      ]
                    }
                  }
                }
                """);

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Duplicate ingredient override position 4");
    }
}
