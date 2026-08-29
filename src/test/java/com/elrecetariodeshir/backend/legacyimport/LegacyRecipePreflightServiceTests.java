package com.elrecetariodeshir.backend.legacyimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeType;

class LegacyRecipePreflightServiceTests {

    @TempDir
    Path tempDirectory;

    private LegacyRecipePreflightService service;
    private Path jsonPath;
    private Path assetsRoot;

    @BeforeEach
    void setUp() throws IOException {
        service = new LegacyRecipePreflightService();
        jsonPath = Path.of("src/test/resources/legacy/recipes.json");
        assetsRoot = tempDirectory.resolve("assets");
        Files.createDirectories(assetsRoot);

        List<LegacyRecipeSource> recipes = new ObjectMapper().readValue(
                jsonPath.toFile(),
                new com.fasterxml.jackson.core.type.TypeReference<List<LegacyRecipeSource>>() {
                });

        for (LegacyRecipeSource recipe : recipes) {
            Path filename = Path.of(recipe.image()).getFileName();
            Files.write(assetsRoot.resolve(filename), new byte[] {1, 2, 3});
        }
    }

    @Test
    void preparesCompleteApprovedDataset() {
        List<PreparedLegacyRecipe> recipes =
                service.prepare(jsonPath, assetsRoot);

        assertThat(recipes).hasSize(26);

        assertThat(recipes)
                .extracting(PreparedLegacyRecipe::slug)
                .doesNotHaveDuplicates()
                .containsExactlyElementsOf(
                        LegacyRecipeCatalog.definitions().stream()
                                .map(LegacyRecipeDefinition::slug)
                                .toList());

        assertThat(recipes)
                .flatExtracting(PreparedLegacyRecipe::ingredients)
                .hasSize(239);

        assertThat(recipes)
                .flatExtracting(PreparedLegacyRecipe::steps)
                .hasSize(176);

        assertThat(recipes)
                .allSatisfy(recipe -> {
                    assertThat(recipe.imagePath()).isRegularFile();
                    assertThat(recipe.originalFilename()).isNotBlank();
                    assertThat(recipe.mediaType()).isEqualTo("image/jpeg");
                });
    }

    @Test
    void appliesApprovedClassificationMappings() {
        Map<String, PreparedLegacyRecipe> byName =
                service.prepare(jsonPath, assetsRoot).stream()
                        .collect(Collectors.toMap(
                                PreparedLegacyRecipe::name,
                                recipe -> recipe));

        assertRecipe(
                byName.get("Paella"),
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.MEDIUM);

        assertRecipe(
                byName.get("Coq au Vin"),
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.HARD);

        assertThat(byName.get("Causa Limeña").type())
                .isEqualTo(RecipeType.SIDE_DISH);
        assertThat(byName.get("Papa a la Huancaína").type())
                .isEqualTo(RecipeType.SIDE_DISH);
        assertThat(byName.get("Guacamole").type())
                .isEqualTo(RecipeType.SAUCE);
        assertThat(byName.get("Totopos").type())
                .isEqualTo(RecipeType.SIDE_DISH);
        assertThat(byName.get("Enyucado de Carne").type())
                .isEqualTo(RecipeType.MAIN_COURSE);
        assertThat(byName.get("Sopa Azteca").type())
                .isEqualTo(RecipeType.MAIN_COURSE);
        assertThat(byName.get("Puré de Papa").type())
                .isEqualTo(RecipeType.SIDE_DISH);

        assertThat(byName.get("Arroz con Pollo").category())
                .isEqualTo(RecipeCategory.NATIONAL);
        assertThat(byName.get("Arroz con Pollo").countryCode())
                .isEqualTo("CR");

        assertThat(byName.get("Torta Española").time())
                .isEqualTo(30);
        assertThat(byName.get("Paella").time())
                .isNull();
    }

    @Test
    void preservesIngredientAndStepOrderAndText() {
        PreparedLegacyRecipe paella =
                service.prepare(jsonPath, assetsRoot).stream()
                        .filter(recipe -> recipe.name().equals("Paella"))
                        .findFirst()
                        .orElseThrow();

        assertThat(paella.ingredients())
                .startsWith(
                        "400 g de muslito de pollo",
                        "400 g de magro o costillas de cerdo cortadas en trozos pequeños");

        assertThat(paella.steps())
                .startsWith(
                        "Salpimentar el pollo y el cerdo.",
                        "Colocar aceite de oliva en una sartén y dorar el pollo y el cerdo a fuego medio hasta que estén bien cocinados.");
    }

    @Test
    void abortsWhenImageIsMissing() throws IOException {
        Files.delete(assetsRoot.resolve("paella.jpg"));

        assertThatThrownBy(() -> service.prepare(jsonPath, assetsRoot))
                .isInstanceOf(LegacyRecipeImportException.class)
                .hasMessageContaining("image does not exist")
                .hasMessageContaining("Paella");
    }

    @Test
    void abortsWhenSourceCountIsNotTwentySix() throws IOException {
        List<LegacyRecipeSource> recipes = new ObjectMapper().readValue(
                jsonPath.toFile(),
                new com.fasterxml.jackson.core.type.TypeReference<List<LegacyRecipeSource>>() {
                });

        Path invalidJson = tempDirectory.resolve("invalid-count.json");
        new ObjectMapper().writeValue(
                invalidJson.toFile(),
                recipes.subList(0, 25));

        assertThatThrownBy(() -> service.prepare(invalidJson, assetsRoot))
                .isInstanceOf(LegacyRecipeImportException.class)
                .hasMessageContaining("Expected exactly 26");
    }

    @Test
    void abortsOnInvalidJsonBeforePreparingAnything() throws IOException {
        Path invalidJson = tempDirectory.resolve("invalid.json");
        Files.writeString(invalidJson, "{not-json");

        assertThatThrownBy(() -> service.prepare(invalidJson, assetsRoot))
                .isInstanceOf(LegacyRecipeImportException.class)
                .hasMessageContaining("JSON is invalid");
    }

    private void assertRecipe(
            PreparedLegacyRecipe recipe,
            RecipeType type,
            RecipeDifficulty difficulty) {
        assertThat(recipe.type()).isEqualTo(type);
        assertThat(recipe.difficulty()).isEqualTo(difficulty);
    }
}
