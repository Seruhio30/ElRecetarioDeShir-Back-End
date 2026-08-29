package com.elrecetariodeshir.backend.recipe.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

@SpringBootTest(properties = "app.media.storage.root=${java.io.tmpdir}/elrecetariodeshir-public-api-test-media")
@AutoConfigureMockMvc
@Transactional
class PublicRecipeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Test
    void listsOnlyPublishedRecipes() throws Exception {
        persistRecipe(
                "test-api-published-list",
                "Published Test Recipe",
                RecipeStatus.PUBLISHED,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        persistRecipe(
                "test-api-draft-list",
                "Draft Test Recipe",
                RecipeStatus.DRAFT,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        mockMvc.perform(get("/api/recipes")
                        .param("q", "Test Recipe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug")
                        .value(org.hamcrest.Matchers.hasItem("test-api-published-list")))
                .andExpect(jsonPath("$.content[*].slug")
                        .value(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.hasItem("test-api-draft-list"))));
    }

    @Test
    void supportsPagination() throws Exception {
        for (int index = 0; index < 3; index++) {
            persistRecipe(
                    "test-api-page-" + index,
                    "Paging Fixture " + index,
                    RecipeStatus.PUBLISHED,
                    RecipeCategory.NATIONAL,
                    "CR",
                    RecipeType.MAIN_COURSE,
                    RecipeDifficulty.EASY);
        }

        mockMvc.perform(get("/api/recipes")
                        .param("q", "Paging Fixture")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void filtersByCategoryCountryTypeDifficultyAndName() throws Exception {
        persistRecipe(
                "test-api-filter-match",
                "Filter Match Recipe",
                RecipeStatus.PUBLISHED,
                RecipeCategory.INTERNATIONAL,
                "PE",
                RecipeType.SIDE_DISH,
                RecipeDifficulty.MEDIUM);

        persistRecipe(
                "test-api-filter-other",
                "Filter Match Other",
                RecipeStatus.PUBLISHED,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        mockMvc.perform(get("/api/recipes")
                        .param("category", "international")
                        .param("country", "pe")
                        .param("type", "side_dish")
                        .param("difficulty", "medium")
                        .param("q", "fIlTeR mAtCh ReCiPe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug")
                        .value("test-api-filter-match"));
    }

    @Test
    void returnsPublishedRecipeDetailWithOrderedChildren() throws Exception {
        Recipe recipe = newRecipe(
                "test-api-detail",
                "Detail Fixture",
                RecipeStatus.PUBLISHED,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        recipe.addIngredient(new RecipeIngredient(1, "Segundo ingrediente"));
        recipe.addIngredient(new RecipeIngredient(0, "Primer ingrediente"));

        recipe.addStep(new RecipeStep(1, "Segundo paso"));
        recipe.addStep(new RecipeStep(0, "Primer paso"));

        recipe.addImage(new RecipeImage(
                "test-api-detail-second.webp",
                "second.webp",
                "image/webp",
                "Segunda imagen",
                1,
                false));

        recipe.addImage(new RecipeImage(
                "test-api-detail-primary.webp",
                "primary.webp",
                "image/webp",
                "Imagen principal",
                0,
                true));

        recipeRepository.saveAndFlush(recipe);

        mockMvc.perform(get("/api/recipes/test-api-detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-api-detail"))
                .andExpect(jsonPath("$.ingredients[0].position").value(0))
                .andExpect(jsonPath("$.ingredients[0].text").value("Primer ingrediente"))
                .andExpect(jsonPath("$.ingredients[1].position").value(1))
                .andExpect(jsonPath("$.steps[0].position").value(0))
                .andExpect(jsonPath("$.steps[0].instruction").value("Primer paso"))
                .andExpect(jsonPath("$.steps[1].position").value(1))
                .andExpect(jsonPath("$.images[0].primary").value(true))
                .andExpect(jsonPath("$.images[1].primary").value(false));
    }

    @Test
    void returnsNotFoundForMissingDraftAndArchivedRecipes() throws Exception {
        persistRecipe(
                "test-api-hidden-draft",
                "Hidden Draft",
                RecipeStatus.DRAFT,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        persistRecipe(
                "test-api-hidden-archived",
                "Hidden Archived",
                RecipeStatus.ARCHIVED,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        mockMvc.perform(get("/api/recipes/test-api-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        mockMvc.perform(get("/api/recipes/test-api-hidden-draft"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        mockMvc.perform(get("/api/recipes/test-api-hidden-archived"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void rejectsInvalidFiltersAndPagination() throws Exception {
        mockMvc.perform(get("/api/recipes")
                        .param("category", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/recipes")
                        .param("country", "Costa Rica"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/recipes")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/recipes")
                        .param("size", "25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/recipes")
                        .param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid recipe request."));
    }

    @Test
    void servesImageOnlyThroughPublishedOwningRecipe() throws Exception {
        String storageKey = mediaStorageService.store(
                new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
                "webp");

        Recipe recipe = newRecipe(
                "test-api-media",
                "Media Fixture",
                RecipeStatus.PUBLISHED,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        recipe.addImage(new RecipeImage(
                storageKey,
                "media.webp",
                "image/webp",
                "Media fixture",
                0,
                true));

        recipeRepository.saveAndFlush(recipe);

        Long imageId = recipe.getImages().getFirst().getId();

        mockMvc.perform(get(
                        "/api/recipes/test-api-media/images/{imageId}",
                        imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(new byte[] {1, 2, 3, 4}));

        mockMvc.perform(get(
                        "/api/recipes/wrong-recipe/images/{imageId}",
                        imageId))
                .andExpect(status().isNotFound());
    }

    @Test
    void supportsIndividualFilters() throws Exception {
        persistRecipe(
                "test-api-individual-match",
                "Individual Filter Match",
                RecipeStatus.PUBLISHED,
                RecipeCategory.INTERNATIONAL,
                "MX",
                RecipeType.DESSERT,
                RecipeDifficulty.HARD);

        mockMvc.perform(get("/api/recipes")
                        .param("category", "INTERNATIONAL")
                        .param("q", "Individual Filter Match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/recipes")
                        .param("country", "MX")
                        .param("q", "Individual Filter Match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/recipes")
                        .param("type", "DESSERT")
                        .param("q", "Individual Filter Match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/recipes")
                        .param("difficulty", "HARD")
                        .param("q", "Individual Filter Match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void rejectsMediaForUnpublishedRecipeAndForeignImage() throws Exception {
        String publishedStorageKey = mediaStorageService.store(
                new ByteArrayInputStream(new byte[] {5, 6, 7}),
                "webp");

        Recipe published = newRecipe(
                "test-api-media-owner",
                "Media Owner",
                RecipeStatus.PUBLISHED,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        published.addImage(new RecipeImage(
                publishedStorageKey,
                "owner.webp",
                "image/webp",
                "Owner image",
                0,
                true));

        recipeRepository.saveAndFlush(published);
        Long publishedImageId = published.getImages().getFirst().getId();

        String draftStorageKey = mediaStorageService.store(
                new ByteArrayInputStream(new byte[] {8, 9, 10}),
                "webp");

        Recipe draft = newRecipe(
                "test-api-media-draft",
                "Media Draft",
                RecipeStatus.DRAFT,
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY);

        draft.addImage(new RecipeImage(
                draftStorageKey,
                "draft.webp",
                "image/webp",
                "Draft image",
                0,
                true));

        recipeRepository.saveAndFlush(draft);
        Long draftImageId = draft.getImages().getFirst().getId();

        mockMvc.perform(get(
                        "/api/recipes/test-api-media-draft/images/{imageId}",
                        draftImageId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/recipes/test-api-media-draft/images/{imageId}",
                        publishedImageId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/recipes/test-api-media-owner/images/{imageId}",
                        draftImageId))
                .andExpect(status().isNotFound());
    }

    private Recipe persistRecipe(
            String slug,
            String name,
            RecipeStatus status,
            RecipeCategory category,
            String countryCode,
            RecipeType type,
            RecipeDifficulty difficulty) {

        return recipeRepository.saveAndFlush(
                newRecipe(
                        slug,
                        name,
                        status,
                        category,
                        countryCode,
                        type,
                        difficulty));
    }

    private Recipe newRecipe(
            String slug,
            String name,
            RecipeStatus status,
            RecipeCategory category,
            String countryCode,
            RecipeType type,
            RecipeDifficulty difficulty) {

        Recipe recipe = new Recipe(
                slug,
                name,
                "Test Cuisine",
                category,
                countryCode,
                type,
                difficulty,
                30,
                status);

        if (status == RecipeStatus.PUBLISHED) {
            recipe.setPublishedAt(Instant.parse("2026-08-20T12:00:00Z"));
        }

        if (status == RecipeStatus.ARCHIVED) {
            recipe.setArchivedAt(Instant.parse("2026-08-21T12:00:00Z"));
        }

        return recipe;
    }
}
