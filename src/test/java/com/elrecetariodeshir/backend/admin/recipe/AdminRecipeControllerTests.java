package com.elrecetariodeshir.backend.admin.recipe;

import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeIngredient;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeStep;
import com.elrecetariodeshir.backend.recipe.RecipeType;

@DatabaseIntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminRecipeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void listsAllAdministrativeStatusesAndSupportsFilters() throws Exception {
        persistRecipe("admin-list-draft", "Admin Draft Fixture", RecipeStatus.DRAFT);
        persistRecipe("admin-list-published", "Admin Published Fixture", RecipeStatus.PUBLISHED);
        persistRecipe("admin-list-archived", "Admin Archived Fixture", RecipeStatus.ARCHIVED);

        mockMvc.perform(get("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .param("q", "Admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug")
                        .value(hasItem("admin-list-draft")))
                .andExpect(jsonPath("$.content[*].slug")
                        .value(hasItem("admin-list-published")))
                .andExpect(jsonPath("$.content[*].slug")
                        .value(hasItem("admin-list-archived")));

        mockMvc.perform(get("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .param("status", "DRAFT")
                        .param("q", "Admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug")
                        .value(hasItem("admin-list-draft")))
                .andExpect(jsonPath("$.content[*].slug")
                        .value(not(hasItem("admin-list-published"))));
    }

    @Test
    void createsDraftWithGeneratedSlugAndOrderedChildren() throws Exception {
        mockMvc.perform(post("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(
                                "Arroz Ñandú Especial",
                                "Ingrediente B",
                                "Ingrediente A",
                                "Paso B",
                                "Paso A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug")
                        .value("arroz-nandu-especial"))
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.ingredients[0].position")
                        .value(0))
                .andExpect(jsonPath("$.ingredients[0].text")
                        .value("Ingrediente A"))
                .andExpect(jsonPath("$.ingredients[1].position")
                        .value(1))
                .andExpect(jsonPath("$.steps[0].position")
                        .value(0))
                .andExpect(jsonPath("$.steps[0].instruction")
                        .value("Paso A"));
    }

    @Test
    void createsDraftWithFlexibleRecipeYield() throws Exception {
        mockMvc.perform(post("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Yield Create Fixture",
                                  "cuisine": "Test",
                                  "category": "NATIONAL",
                                  "countryCode": "CR",
                                  "type": "MAIN_COURSE",
                                  "difficulty": "EASY",
                                  "time": 30,
                                  "yieldQuantity": 18,
                                  "yieldUnit": "UNIT",
                                  "yieldDisplay": "18 u",
                                  "ingredients": [
                                    {"position": 0, "text": "Ingrediente"}
                                  ],
                                  "steps": [
                                    {"position": 0, "instruction": "Paso"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yieldQuantity").value(18))
                .andExpect(jsonPath("$.yieldMin").doesNotExist())
                .andExpect(jsonPath("$.yieldMax").doesNotExist())
                .andExpect(jsonPath("$.yieldUnit").value("UNIT"))
                .andExpect(jsonPath("$.yieldDisplay").value("18 u"));
    }

    @Test
    void updatesAndRejectsInvalidRecipeYield() throws Exception {
        Recipe recipe = persistRecipe(
                "yield-update-fixture",
                "Yield Update Fixture",
                RecipeStatus.DRAFT);

        mockMvc.perform(patch("/api/admin/recipes/{id}", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Yield Update Fixture",
                                  "cuisine": "Test",
                                  "category": "NATIONAL",
                                  "countryCode": "CR",
                                  "type": "MAIN_COURSE",
                                  "difficulty": "EASY",
                                  "time": 30,
                                  "yieldMin": 8,
                                  "yieldMax": 12,
                                  "yieldUnit": "SERVING",
                                  "yieldDisplay": "8-12 porciones",
                                  "ingredients": [
                                    {"position": 0, "text": "Ingrediente"}
                                  ],
                                  "steps": [
                                    {"position": 0, "instruction": "Paso"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yieldQuantity").doesNotExist())
                .andExpect(jsonPath("$.yieldMin").value(8))
                .andExpect(jsonPath("$.yieldMax").value(12))
                .andExpect(jsonPath("$.yieldUnit").value("SERVING"))
                .andExpect(jsonPath("$.yieldDisplay").value("8-12 porciones"));

        mockMvc.perform(patch("/api/admin/recipes/{id}", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid Negative Yield",
                                  "cuisine": "Test",
                                  "category": "NATIONAL",
                                  "countryCode": "CR",
                                  "type": "MAIN_COURSE",
                                  "difficulty": "EASY",
                                  "time": 30,
                                  "yieldQuantity": -1,
                                  "ingredients": [
                                    {"position": 0, "text": "Ingrediente"}
                                  ],
                                  "steps": [
                                    {"position": 0, "instruction": "Paso"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/recipes/{id}", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid Yield Range",
                                  "cuisine": "Test",
                                  "category": "NATIONAL",
                                  "countryCode": "CR",
                                  "type": "MAIN_COURSE",
                                  "difficulty": "EASY",
                                  "time": 30,
                                  "yieldMin": 12,
                                  "yieldMax": 8,
                                  "yieldUnit": "SERVING",
                                  "ingredients": [
                                    {"position": 0, "text": "Ingrediente"}
                                  ],
                                  "steps": [
                                    {"position": 0, "instruction": "Paso"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void generatesUniqueSlugWithoutAllowingClientSlugManipulation() throws Exception {
        persistRecipe(
                "receta-especial",
                "Existing Recipe",
                RecipeStatus.DRAFT);

        mockMvc.perform(post("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(
                                "Receta Especial",
                                "B",
                                "A",
                                "B",
                                "A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug")
                        .value("receta-especial-2"));
    }

    @Test
    void editsRecipeButKeepsSlugStableAfterRename() throws Exception {
        Recipe recipe = persistRecipe(
                "stable-slug",
                "Original Name",
                RecipeStatus.DRAFT);

        mockMvc.perform(patch("/api/admin/recipes/{id}", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(
                                "Completely New Name",
                                "Nuevo segundo",
                                "Nuevo primero",
                                "Nuevo paso dos",
                                "Nuevo paso uno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug")
                        .value("stable-slug"))
                .andExpect(jsonPath("$.name")
                        .value("Completely New Name"))
                .andExpect(jsonPath("$.ingredients[0].text")
                        .value("Nuevo primero"))
                .andExpect(jsonPath("$.steps[0].instruction")
                        .value("Nuevo paso uno"));
    }

    @Test
    void replacesExistingIngredientsAndStepsWithoutPositionConflicts() throws Exception {
        Recipe recipe = persistCompleteRecipe(
                "replace-children",
                "Replace Children",
                RecipeStatus.DRAFT);

        mockMvc.perform(patch("/api/admin/recipes/{id}", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(
                                "Replace Children Updated",
                                "Ingrediente nuevo dos",
                                "Ingrediente nuevo uno",
                                "Paso nuevo dos",
                                "Paso nuevo uno")))
                .andExpect(status().isOk());

        recipeRepository.flush();

        mockMvc.perform(get("/api/admin/recipes/{id}", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andExpect(jsonPath("$.ingredients[0].position").value(0))
                .andExpect(jsonPath("$.ingredients[0].text")
                        .value("Ingrediente nuevo uno"))
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[0].position").value(0))
                .andExpect(jsonPath("$.steps[0].instruction")
                        .value("Paso nuevo uno"));
    }

    @Test
    void publishesCompleteDraftAndMakesItPublic() throws Exception {
        Recipe recipe = persistCompleteRecipe(
                "publish-public-fixture",
                "Publish Public Fixture",
                RecipeStatus.DRAFT);

        mockMvc.perform(get("/api/recipes/publish-public-fixture"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/recipes/{id}/publish", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt")
                        .isNotEmpty())
                .andExpect(jsonPath("$.archivedAt")
                        .doesNotExist());

        mockMvc.perform(get("/api/recipes/publish-public-fixture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug")
                        .value("publish-public-fixture"));
    }

    @Test
    void rejectsPublishingIncompleteDraft() throws Exception {
        Recipe recipe = persistRecipe(
                "incomplete-publish",
                "Incomplete Publish",
                RecipeStatus.DRAFT);

        mockMvc.perform(post("/api/admin/recipes/{id}/publish", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("BAD_REQUEST"));
    }

    @Test
    void archivesPublishedRecipeAndRemovesItFromPublicApi() throws Exception {
        Recipe recipe = persistCompleteRecipe(
                "archive-public-fixture",
                "Archive Public Fixture",
                RecipeStatus.PUBLISHED);

        mockMvc.perform(get("/api/recipes/archive-public-fixture"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/recipes/{id}/archive", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedAt")
                        .isNotEmpty());

        mockMvc.perform(get("/api/recipes/archive-public-fixture"))
                .andExpect(status().isNotFound());
    }

    @Test
    void restoresArchivedRecipeToDraftAndKeepsItPrivate() throws Exception {
        Recipe recipe = persistCompleteRecipe(
                "restore-private-fixture",
                "Restore Private Fixture",
                RecipeStatus.ARCHIVED);

        recipe.setArchivedAt(Instant.now());
        recipeRepository.saveAndFlush(recipe);

        mockMvc.perform(post("/api/admin/recipes/{id}/restore", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"));

        mockMvc.perform(get("/api/recipes/restore-private-fixture"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidLifecycleTransition() throws Exception {
        Recipe recipe = persistCompleteRecipe(
                "invalid-transition",
                "Invalid Transition",
                RecipeStatus.DRAFT);

        mockMvc.perform(post("/api/admin/recipes/{id}/archive", recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("CONFLICT"));
    }

    @Test
    void returnsNotFoundForUnknownAdministrativeRecipe() throws Exception {
        mockMvc.perform(get("/api/admin/recipes/999999999")
                        .with(user("sergio").authorities(() -> "ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("NOT_FOUND"));
    }

    @Test
    void rejectsInvalidRequestAndDuplicatePositions() throws Exception {
        mockMvc.perform(post("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "cuisine": "Test",
                                  "category": "NATIONAL",
                                  "countryCode": "Costa Rica",
                                  "type": "MAIN_COURSE",
                                  "difficulty": "EASY",
                                  "time": -1,
                                  "ingredients": [],
                                  "steps": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Duplicate Positions",
                                  "cuisine": "Test",
                                  "category": "NATIONAL",
                                  "countryCode": "CR",
                                  "type": "MAIN_COURSE",
                                  "difficulty": "EASY",
                                  "time": 15,
                                  "ingredients": [
                                    {"position": 0, "text": "Uno"},
                                    {"position": 0, "text": "Dos"}
                                  ],
                                  "steps": [
                                    {"position": 0, "instruction": "Paso"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("BAD_REQUEST"));
    }

    @Test
    void csrfIsRequiredForAdministrativeMutation() throws Exception {
        mockMvc.perform(post("/api/admin/recipes")
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(
                                "No Csrf",
                                "B",
                                "A",
                                "B",
                                "A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_ACCESS_DENIED"));
    }

    private Recipe persistRecipe(
            String slug,
            String name,
            RecipeStatus status) {

        Recipe recipe = new Recipe(
                slug,
                name,
                "Test",
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY,
                30,
                status);

        if (status == RecipeStatus.PUBLISHED) {
            recipe.setPublishedAt(Instant.now());
        }

        if (status == RecipeStatus.ARCHIVED) {
            recipe.setArchivedAt(Instant.now());
        }

        return recipeRepository.saveAndFlush(recipe);
    }

    private Recipe persistCompleteRecipe(
            String slug,
            String name,
            RecipeStatus status) {

        Recipe recipe = new Recipe(
                slug,
                name,
                "Test",
                RecipeCategory.NATIONAL,
                "CR",
                RecipeType.MAIN_COURSE,
                RecipeDifficulty.EASY,
                30,
                status);

        recipe.addIngredient(
                new RecipeIngredient(0, "Ingrediente"));

        recipe.addStep(
                new RecipeStep(0, "Paso"));

        if (status == RecipeStatus.PUBLISHED) {
            recipe.setPublishedAt(Instant.now());
        }

        if (status == RecipeStatus.ARCHIVED) {
            recipe.setArchivedAt(Instant.now());
        }

        return recipeRepository.saveAndFlush(recipe);
    }

    private String validRequest(
            String name,
            String ingredientPositionOne,
            String ingredientPositionZero,
            String stepPositionOne,
            String stepPositionZero) {

        return """
                {
                  "name": "%s",
                  "cuisine": "Costarricense",
                  "category": "NATIONAL",
                  "countryCode": "CR",
                  "type": "MAIN_COURSE",
                  "difficulty": "EASY",
                  "time": 45,
                  "ingredients": [
                    {"position": 1, "text": "%s"},
                    {"position": 0, "text": "%s"}
                  ],
                  "steps": [
                    {"position": 1, "instruction": "%s"},
                    {"position": 0, "instruction": "%s"}
                  ]
                }
                """.formatted(
                name,
                ingredientPositionOne,
                ingredientPositionZero,
                stepPositionOne,
                stepPositionZero);
    }
}
