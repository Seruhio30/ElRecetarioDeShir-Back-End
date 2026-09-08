package com.elrecetariodeshir.backend.recipe;

import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@DatabaseIntegrationTest
@Transactional
class RecipeRepositoryTests {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsRecipeWithEnumsAndTimestamps() {
        Recipe recipe = createRecipe("gallo-pinto");

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();

        entityManager.clear();

        Recipe persisted = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getSlug()).isEqualTo("gallo-pinto");
        assertThat(persisted.getName()).isEqualTo("Gallo Pinto");
        assertThat(persisted.getCuisine()).isEqualTo("Costa Rican");
        assertThat(persisted.getCategory()).isEqualTo(RecipeCategory.NATIONAL);
        assertThat(persisted.getCountryCode()).isEqualTo("CR");
        assertThat(persisted.getType()).isEqualTo(RecipeType.MAIN_COURSE);
        assertThat(persisted.getDifficulty()).isEqualTo(RecipeDifficulty.EASY);
        assertThat(persisted.getTime()).isEqualTo(30);
        assertThat(persisted.getStatus()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isAfterOrEqualTo(persisted.getCreatedAt());

        String category = jdbcTemplate.queryForObject(
                "SELECT category FROM recipe WHERE id = ?",
                String.class,
                recipeId);

        String type = jdbcTemplate.queryForObject(
                "SELECT type FROM recipe WHERE id = ?",
                String.class,
                recipeId);

        String difficulty = jdbcTemplate.queryForObject(
                "SELECT difficulty FROM recipe WHERE id = ?",
                String.class,
                recipeId);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM recipe WHERE id = ?",
                String.class,
                recipeId);

        assertThat(category).isEqualTo("NATIONAL");
        assertThat(type).isEqualTo("MAIN_COURSE");
        assertThat(difficulty).isEqualTo("EASY");
        assertThat(status).isEqualTo("DRAFT");
    }

    @Test
    void cascadesAndLoadsIngredientsStepsAndImagesInPersistedOrder() {
        Recipe recipe = createRecipe("test-cascade-recipe");

        recipe.addIngredient(new RecipeIngredient(2, "Culantro al gusto"));
        recipe.addIngredient(new RecipeIngredient(0, "2 tazas de arroz"));
        recipe.addIngredient(new RecipeIngredient(1, "500 g de pollo"));

        recipe.addStep(new RecipeStep(1, "Agregar el pollo y cocinar."));
        recipe.addStep(new RecipeStep(0, "Lavar y preparar el arroz."));

        recipe.addImage(new RecipeImage(
                "recipes/arroz-con-pollo/secondary.webp",
                "secondary.webp",
                "image/webp",
                "Arroz con pollo servido",
                1,
                false));

        recipe.addImage(new RecipeImage(
                "recipes/arroz-con-pollo/main.webp",
                "main.webp",
                "image/webp",
                "Plato principal de arroz con pollo",
                0,
                true));

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();

        entityManager.clear();

        Recipe persisted = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(persisted.getIngredients())
                .extracting(RecipeIngredient::getPosition)
                .containsExactly(0, 1, 2);

        assertThat(persisted.getIngredients())
                .extracting(RecipeIngredient::getText)
                .containsExactly(
                        "2 tazas de arroz",
                        "500 g de pollo",
                        "Culantro al gusto");

        assertThat(persisted.getSteps())
                .extracting(RecipeStep::getPosition)
                .containsExactly(0, 1);

        assertThat(persisted.getSteps())
                .extracting(RecipeStep::getInstruction)
                .containsExactly(
                        "Lavar y preparar el arroz.",
                        "Agregar el pollo y cocinar.");

        assertThat(persisted.getImages())
                .extracting(RecipeImage::getPosition)
                .containsExactly(0, 1);

        RecipeImage primaryImage = persisted.getImages().getFirst();

        assertThat(primaryImage.getStorageKey())
                .isEqualTo("recipes/arroz-con-pollo/main.webp");
        assertThat(primaryImage.getOriginalFilename()).isEqualTo("main.webp");
        assertThat(primaryImage.getMediaType()).isEqualTo("image/webp");
        assertThat(primaryImage.getAltText())
                .isEqualTo("Plato principal de arroz con pollo");
        assertThat(primaryImage.isPrimaryImage()).isTrue();
        assertThat(primaryImage.getCreatedAt()).isNotNull();

        assertThat(persisted.getIngredients())
                .allSatisfy(ingredient ->
                        assertThat(ingredient.getRecipe().getId()).isEqualTo(recipeId));

        assertThat(persisted.getSteps())
                .allSatisfy(step ->
                        assertThat(step.getRecipe().getId()).isEqualTo(recipeId));

        assertThat(persisted.getImages())
                .allSatisfy(image ->
                        assertThat(image.getRecipe().getId()).isEqualTo(recipeId));
    }

    @Test
    void lifecycleStatesAndTimestampsAreRepresentable() {
        Recipe recipe = createRecipe("tres-leches");

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();
        Instant createdAt = recipe.getCreatedAt();
        Instant initialUpdatedAt = recipe.getUpdatedAt();

        try {
            Thread.sleep(5);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test interrupted", exception);
        }

        Instant publishedAt = Instant.now();
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setPublishedAt(publishedAt);
        recipeRepository.flush();

        entityManager.clear();

        Recipe published = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(published.getStatus()).isEqualTo(RecipeStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.getCreatedAt())
                .isCloseTo(createdAt, within(1, java.time.temporal.ChronoUnit.MICROS));
        assertThat(published.getUpdatedAt())
                .isAfter(initialUpdatedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS));

        Instant archivedAt = Instant.now();
        published.setStatus(RecipeStatus.ARCHIVED);
        published.setArchivedAt(archivedAt);
        recipeRepository.flush();

        entityManager.clear();

        Recipe archived = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(archived.getStatus()).isEqualTo(RecipeStatus.ARCHIVED);
        assertThat(archived.getPublishedAt()).isNotNull();
        assertThat(archived.getArchivedAt()).isNotNull();

        archived.setStatus(RecipeStatus.DRAFT);
        recipeRepository.flush();

        entityManager.clear();

        Recipe restored = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(restored.getStatus()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(restored.getPublishedAt()).isNotNull();
        assertThat(restored.getArchivedAt()).isNotNull();
    }

    @Test
    void persistsSimpleRecipeYield() {
        Recipe recipe = createRecipe("yield-simple");

        recipe.setYieldQuantity(new BigDecimal("12"));
        recipe.setYieldUnit(RecipeYieldUnit.SERVING);
        recipe.setYieldDisplay("12 porciones");

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();

        entityManager.clear();

        Recipe persisted = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(persisted.getYieldQuantity())
                .isEqualByComparingTo("12.000");
        assertThat(persisted.getYieldMin()).isNull();
        assertThat(persisted.getYieldMax()).isNull();
        assertThat(persisted.getYieldUnit())
                .isEqualTo(RecipeYieldUnit.SERVING);
        assertThat(persisted.getYieldDisplay())
                .isEqualTo("12 porciones");
    }

    @Test
    void persistsRecipeYieldRangeAndUnit() {
        Recipe recipe = createRecipe("yield-range");

        recipe.setYieldMin(new BigDecimal("8"));
        recipe.setYieldMax(new BigDecimal("12"));
        recipe.setYieldUnit(RecipeYieldUnit.SERVING);
        recipe.setYieldDisplay("8-12");

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();

        entityManager.clear();

        Recipe persisted = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(persisted.getYieldQuantity()).isNull();
        assertThat(persisted.getYieldMin())
                .isEqualByComparingTo("8.000");
        assertThat(persisted.getYieldMax())
                .isEqualByComparingTo("12.000");
        assertThat(persisted.getYieldUnit())
                .isEqualTo(RecipeYieldUnit.SERVING);
        assertThat(persisted.getYieldDisplay())
                .isEqualTo("8-12");
    }

    @Test
    void recipeWithoutYieldRemainsValid() {
        Recipe recipe = createRecipe("yield-optional");

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();

        entityManager.clear();

        Recipe persisted = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(persisted.getYieldQuantity()).isNull();
        assertThat(persisted.getYieldMin()).isNull();
        assertThat(persisted.getYieldMax()).isNull();
        assertThat(persisted.getYieldUnit()).isNull();
        assertThat(persisted.getYieldDisplay()).isNull();
    }

    @Test
    void rejectsInvalidRecipeYieldValues() {
        Recipe negative = createRecipe("yield-negative");
        negative.setYieldQuantity(new BigDecimal("-1"));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(negative))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();

        Recipe invalidRange = createRecipe("yield-invalid-range");
        invalidRange.setYieldMin(new BigDecimal("12"));
        invalidRange.setYieldMax(new BigDecimal("8"));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(invalidRange))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    @Test
    void persistsStructuredIngredientWithSimpleQuantityDecimalRangeUnitNotesAndDisplayText() {
        Recipe recipe = createRecipe("structured-ingredient");

        recipe.addIngredient(new RecipeIngredient(
                2,
                "Aceite de oliva",
                new BigDecimal("1.5"),
                null,
                RecipeIngredientUnit.TABLESPOON,
                "para terminar",
                "1.5 cdas de aceite de oliva para terminar"));

        recipe.addIngredient(new RecipeIngredient(
                0,
                "Huevos",
                new BigDecimal("3"),
                new BigDecimal("4"),
                RecipeIngredientUnit.UNIT,
                "grandes",
                "3-4 huevos grandes"));

        recipe.addIngredient(new RecipeIngredient(
                1,
                "Harina",
                new BigDecimal("250"),
                null,
                RecipeIngredientUnit.GRAM,
                null,
                "250 gr de harina"));

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();

        entityManager.clear();

        Recipe persisted = recipeRepository.findById(recipeId).orElseThrow();

        assertThat(persisted.getIngredients())
                .extracting(RecipeIngredient::getPosition)
                .containsExactly(0, 1, 2);

        RecipeIngredient range = persisted.getIngredients().get(0);
        assertThat(range.getIngredientName()).isEqualTo("Huevos");
        assertThat(range.getQuantity()).isEqualByComparingTo("3");
        assertThat(range.getQuantityMax()).isEqualByComparingTo("4");
        assertThat(range.getUnit()).isEqualTo(RecipeIngredientUnit.UNIT);
        assertThat(range.getNotes()).isEqualTo("grandes");
        assertThat(range.getDisplayText()).isEqualTo("3-4 huevos grandes");
        assertThat(range.getText()).isEqualTo("3-4 huevos grandes");

        RecipeIngredient simple = persisted.getIngredients().get(1);
        assertThat(simple.getQuantity()).isEqualByComparingTo("250");
        assertThat(simple.getQuantityMax()).isNull();
        assertThat(simple.getUnit()).isEqualTo(RecipeIngredientUnit.GRAM);

        RecipeIngredient decimal = persisted.getIngredients().get(2);
        assertThat(decimal.getQuantity()).isEqualByComparingTo("1.5");
        assertThat(decimal.getNotes()).isEqualTo("para terminar");
    }

    @Test
    void keepsLegacyIngredientWithoutStructuredFieldsValid() {
        Recipe recipe = createRecipe("legacy-ingredient");

        recipe.addIngredient(new RecipeIngredient(
                0,
                "Culantro al gusto"));

        recipeRepository.saveAndFlush(recipe);
        Long recipeId = recipe.getId();

        entityManager.clear();

        RecipeIngredient persisted = recipeRepository.findById(recipeId)
                .orElseThrow()
                .getIngredients()
                .getFirst();

        assertThat(persisted.getDisplayText()).isEqualTo("Culantro al gusto");
        assertThat(persisted.getText()).isEqualTo("Culantro al gusto");
        assertThat(persisted.getIngredientName()).isNull();
        assertThat(persisted.getQuantity()).isNull();
        assertThat(persisted.getQuantityMax()).isNull();
        assertThat(persisted.getUnit()).isNull();
        assertThat(persisted.getNotes()).isNull();
    }

    @Test
    void rejectsNegativeIngredientQuantitiesAndInvalidRange() {
        Recipe negativeQuantity = createRecipe("negative-ingredient-quantity");
        negativeQuantity.addIngredient(new RecipeIngredient(
                0,
                "Azucar",
                new BigDecimal("-1"),
                null,
                RecipeIngredientUnit.GRAM,
                null,
                "-1 gr de azucar"));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(negativeQuantity))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();

        Recipe negativeQuantityMax = createRecipe("negative-ingredient-quantity-max");
        negativeQuantityMax.addIngredient(new RecipeIngredient(
                0,
                "Azucar",
                new BigDecimal("1"),
                new BigDecimal("-2"),
                RecipeIngredientUnit.GRAM,
                null,
                "1--2 gr de azucar"));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(negativeQuantityMax))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();

        Recipe invalidRange = createRecipe("invalid-ingredient-range");
        invalidRange.addIngredient(new RecipeIngredient(
                0,
                "Huevos",
                new BigDecimal("4"),
                new BigDecimal("3"),
                RecipeIngredientUnit.UNIT,
                null,
                "4-3 huevos"));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(invalidRange))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateRecipeSlug() {
        Recipe first = createRecipe("olla-de-carne");
        Recipe second = createRecipe("olla-de-carne");

        recipeRepository.saveAndFlush(first);

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateChildPositionsWithinSameRecipe() {
        Recipe recipe = createRecipe("casado");

        recipe.addIngredient(new RecipeIngredient(0, "Arroz"));
        recipe.addIngredient(new RecipeIngredient(0, "Frijoles"));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(recipe))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativeChildPosition() {
        Recipe recipe = createRecipe("picadillo");

        recipe.addStep(new RecipeStep(-1, "Preparar los ingredientes."));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(recipe))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Recipe createRecipe(String slug) {
        return new Recipe(
                slug,
                "Gallo Pinto",
                "Costa Rican",
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY,
                30,
                RecipeStatus.DRAFT);
    }
}
