package com.elrecetariodeshir.backend.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "app.media.storage.root=${java.io.tmpdir}/elrecetariodeshir-test-media")
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
        Recipe recipe = createRecipe("arroz-con-pollo");

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
                .isEqualTo(createdAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
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
