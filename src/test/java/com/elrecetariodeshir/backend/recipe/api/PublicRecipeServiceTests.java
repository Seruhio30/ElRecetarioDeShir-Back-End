package com.elrecetariodeshir.backend.recipe.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.elrecetariodeshir.backend.media.storage.MediaStorageService;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeImage;
import com.elrecetariodeshir.backend.recipe.RecipeIngredient;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeStep;
import com.elrecetariodeshir.backend.recipe.RecipeType;
import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

class PublicRecipeServiceTests {

    private RecipeRepository recipeRepository;
    private MediaStorageService mediaStorageService;
    private PublicRecipeService service;

    @BeforeEach
    void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        mediaStorageService = mock(MediaStorageService.class);
        service = new PublicRecipeService(recipeRepository, mediaStorageService);
    }

    @Test
    void mapsPublishedRecipeListWithoutIngredientsOrSteps() {
        Recipe recipe = publishedRecipe("test-api-gallo-pinto", "Gallo Pinto");

        when(recipeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recipe)));

        PagedRecipeResponse response = service.listRecipes(
                0,
                12,
                null,
                null,
                null,
                null,
                null);

        assertThat(response.content()).hasSize(1);

        RecipeSummaryResponse summary = response.content().getFirst();

        assertThat(summary.slug()).isEqualTo("test-api-gallo-pinto");
        assertThat(summary.name()).isEqualTo("Gallo Pinto");
        assertThat(summary.countryCode()).isEqualTo("CR");
        assertThat(summary.primaryImage()).isNull();
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.listRecipes(
                -1, 12, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.listRecipes(
                0, 25, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidFilters() {
        assertThatThrownBy(() -> service.listRecipes(
                0, 12, "UNKNOWN", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.listRecipes(
                0, 12, null, "Costa Rica", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsDetailWithOrderedChildren() {
        Recipe recipe = publishedRecipe("test-api-paella", "Paella");

        recipe.setYieldQuantity(new BigDecimal("1"));
        recipe.setYieldUnit(RecipeYieldUnit.LITER);
        recipe.setYieldDisplay("1 litro");

        recipe.addIngredient(new RecipeIngredient(0, "Arroz"));
        recipe.addIngredient(new RecipeIngredient(1, "Pollo"));
        recipe.addStep(new RecipeStep(0, "Preparar ingredientes."));
        recipe.addStep(new RecipeStep(1, "Cocinar."));
        recipe.addImage(new RecipeImage(
                "test-image.webp",
                "test-image.webp",
                "image/webp",
                "Paella terminada",
                0,
                true));

        when(recipeRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(recipe));

        RecipeDetailResponse response =
                service.getRecipeBySlug("test-api-paella");

        assertThat(response.yieldQuantity())
                .isEqualByComparingTo("1");
        assertThat(response.yieldMin()).isNull();
        assertThat(response.yieldMax()).isNull();
        assertThat(response.yieldUnit())
                .isEqualTo(RecipeYieldUnit.LITER);
        assertThat(response.yieldDisplay())
                .isEqualTo("1 litro");

        assertThat(response.ingredients())
                .extracting(RecipeIngredientResponse::position)
                .containsExactly(0, 1);

        assertThat(response.steps())
                .extracting(RecipeStepResponse::position)
                .containsExactly(0, 1);

        assertThat(response.images())
                .extracting(RecipeImageResponse::primary)
                .containsExactly(true);
    }

    @Test
    void returnsNotFoundWhenPublishedRecipeIsUnavailable() {
        when(recipeRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getRecipeBySlug("test-api-missing"))
                .isInstanceOf(PublicRecipeNotFoundException.class);
    }

    @Test
    void returnsRecipeImageThroughStorageService() {
        Recipe recipe = publishedRecipe("test-api-image", "Image Recipe");
        RecipeImage image = new RecipeImage(
                "test-image.webp",
                "test-image.webp",
                "image/webp",
                "Test image",
                0,
                true);

        recipe.addImage(image);

        when(recipeRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(recipe));

        when(mediaStorageService.exists("test-image.webp"))
                .thenReturn(true);

        when(mediaStorageService.read("test-image.webp"))
                .thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() ->
                service.getRecipeImage("test-api-image", 1L))
                .isInstanceOf(PublicRecipeNotFoundException.class);
    }

    private Recipe publishedRecipe(String slug, String name) {
        Recipe recipe = new Recipe(
                slug,
                name,
                "Test Cuisine",
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY,
                30,
                RecipeStatus.PUBLISHED);

        recipe.setPublishedAt(Instant.parse("2026-08-01T12:00:00Z"));
        return recipe;
    }
}
