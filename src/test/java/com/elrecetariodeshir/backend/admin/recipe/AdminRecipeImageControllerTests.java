package com.elrecetariodeshir.backend.admin.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

@SpringBootTest(properties = {
        "app.media.storage.root=${java.io.tmpdir}/elrecetariodeshir-admin-media-test",
        "spring.servlet.multipart.max-file-size=5MB",
        "spring.servlet.multipart.max-request-size=6MB"
})
@AutoConfigureMockMvc
@Transactional
class AdminRecipeImageControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Test
    void authenticatedAdminUploadsFirstImageAsPrimaryAndFileExists() throws Exception {
        Recipe recipe = persistRecipe(
                "admin-media-first-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        mockMvc.perform(multipart("/api/admin/recipes/{recipeId}/images", recipe.getId())
                        .file(jpeg("first.jpg"))
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("first.jpg"))
                .andExpect(jsonPath("$.mediaType").value("image/jpeg"))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.primaryImage").value(true))
                .andExpect(jsonPath("$.storageKey").doesNotExist());

        Recipe reloaded = recipeRepository.findById(recipe.getId()).orElseThrow();
        assertThat(reloaded.getImages()).hasSize(1);

        RecipeImage image = reloaded.getImages().getFirst();
        assertThat(image.isPrimaryImage()).isTrue();
        assertThat(mediaStorageService.exists(image.getStorageKey())).isTrue();
    }

    @Test
    void uploadRequiresAuthenticationAndCsrf() throws Exception {
        Recipe recipe = persistRecipe(
                "admin-media-security-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        mockMvc.perform(multipart("/api/admin/recipes/{recipeId}/images", recipe.getId())
                        .file(jpeg("unauthenticated.jpg"))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/admin/recipes/{recipeId}/images", recipe.getId())
                        .file(jpeg("no-csrf.jpg"))
                        .with(user("sergio").authorities(() -> "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsEmptyInvalidTypeSignatureAndOversizedUploads() throws Exception {
        Recipe recipe = persistRecipe(
                "admin-media-validation-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        MockMultipartFile empty = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        MockMultipartFile invalidType = new MockMultipartFile(
                "file",
                "image.gif",
                "image/gif",
                new byte[] {'G', 'I', 'F', '8', '9', 'a'});

        MockMultipartFile mismatchedSignature = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                new byte[] {'N', 'O', 'T', 'J', 'P', 'E', 'G'});

        MockMultipartFile oversized = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                oversizedJpegBytes());

        for (MockMultipartFile file : new MockMultipartFile[] {
                empty, invalidType, mismatchedSignature, oversized
        }) {
            mockMvc.perform(multipart("/api/admin/recipes/{recipeId}/images", recipe.getId())
                            .file(file)
                            .with(user("sergio").authorities(() -> "ADMIN"))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void secondImageAppendsAndChangingPrimaryKeepsExactlyOnePrimary() throws Exception {
        Recipe recipe = persistRecipe(
                "admin-media-primary-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        upload(recipe, jpeg("first.jpg"));
        upload(recipe, png("second.png"));

        Recipe reloaded = recipeRepository.findById(recipe.getId()).orElseThrow();

        assertThat(reloaded.getImages()).hasSize(2);
        assertThat(reloaded.getImages().get(0).getPosition()).isZero();
        assertThat(reloaded.getImages().get(1).getPosition()).isEqualTo(1);
        assertThat(reloaded.getImages().stream()
                .filter(RecipeImage::isPrimaryImage))
                .hasSize(1);

        Long secondId = reloaded.getImages().get(1).getId();

        mockMvc.perform(patch(
                        "/api/admin/recipes/{recipeId}/images/{imageId}",
                        recipe.getId(),
                        secondId)
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "primaryImage": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryImage").value(true));

        recipeRepository.flush();
        Recipe updated = recipeRepository.findById(recipe.getId()).orElseThrow();

        assertThat(updated.getImages().stream()
                .filter(RecipeImage::isPrimaryImage))
                .singleElement()
                .extracting(RecipeImage::getId)
                .isEqualTo(secondId);
    }

    @Test
    void updatesAltTextWithoutAllowingStorageMetadataMutation() throws Exception {
        Recipe recipe = persistRecipe(
                "admin-media-alt-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        upload(recipe, webp("dish.webp"));

        RecipeImage image = recipeRepository.findById(recipe.getId())
                .orElseThrow()
                .getImages()
                .getFirst();

        String originalStorageKey = image.getStorageKey();

        mockMvc.perform(patch(
                        "/api/admin/recipes/{recipeId}/images/{imageId}",
                        recipe.getId(),
                        image.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "altText": "  Plato terminado listo para servir  ",
                                  "storageKey": "../../evil.jpg",
                                  "mediaType": "application/octet-stream"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.altText")
                        .value("Plato terminado listo para servir"))
                .andExpect(jsonPath("$.storageKey").doesNotExist());

        RecipeImage updated = recipeRepository.findById(recipe.getId())
                .orElseThrow()
                .getImages()
                .getFirst();

        assertThat(updated.getStorageKey()).isEqualTo(originalStorageKey);
        assertThat(updated.getMediaType()).isEqualTo("image/webp");
    }

    @Test
    void deleteRemovesFileAndReassignsPrimaryToLowestRemainingPosition() throws Exception {
        Recipe recipe = persistRecipe(
                "admin-media-delete-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        upload(recipe, jpeg("first.jpg"));
        upload(recipe, png("second.png"));

        Recipe reloaded = recipeRepository.findById(recipe.getId()).orElseThrow();
        RecipeImage first = reloaded.getImages().get(0);
        RecipeImage second = reloaded.getImages().get(1);
        String deletedStorageKey = first.getStorageKey();

        mockMvc.perform(delete(
                        "/api/admin/recipes/{recipeId}/images/{imageId}",
                        recipe.getId(),
                        first.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(mediaStorageService.exists(deletedStorageKey)).isFalse();

        Recipe updated = recipeRepository.findById(recipe.getId()).orElseThrow();

        assertThat(updated.getImages()).hasSize(1);
        assertThat(updated.getImages().getFirst().getId()).isEqualTo(second.getId());
        assertThat(updated.getImages().getFirst().isPrimaryImage()).isTrue();
    }

    @Test
    void rejectsMissingRecipeAndImageOwnedByAnotherRecipe() throws Exception {
        Recipe first = persistRecipe(
                "admin-media-owner-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        Recipe second = persistRecipe(
                "admin-media-other-" + UUID.randomUUID(),
                RecipeStatus.DRAFT);

        upload(first, jpeg("owner.jpg"));

        Long imageId = recipeRepository.findById(first.getId())
                .orElseThrow()
                .getImages()
                .getFirst()
                .getId();

        mockMvc.perform(multipart("/api/admin/recipes/{recipeId}/images", 999999999L)
                        .file(jpeg("missing.jpg"))
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch(
                        "/api/admin/recipes/{recipeId}/images/{imageId}",
                        second.getId(),
                        imageId)
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"altText":"Foreign"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(
                        "/api/admin/recipes/{recipeId}/images/{imageId}",
                        second.getId(),
                        imageId)
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminContentServesDraftImageAndPublicApiServesItAfterPublish() throws Exception {
        Recipe recipe = persistCompleteRecipe(
                "admin-media-public-" + UUID.randomUUID());

        upload(recipe, jpeg("publish.jpg"));

        Recipe reloaded = recipeRepository.findById(recipe.getId()).orElseThrow();
        RecipeImage image = reloaded.getImages().getFirst();

        mockMvc.perform(get(
                        "/api/admin/recipes/{recipeId}/images/{imageId}/content",
                        recipe.getId(),
                        image.getId())
                        .with(user("sergio").authorities(() -> "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpegBytes()));

        mockMvc.perform(get(
                        "/api/recipes/{slug}/images/{imageId}",
                        reloaded.getSlug(),
                        image.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/admin/recipes/{id}/publish",
                        recipe.getId())
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                        "/api/recipes/{slug}/images/{imageId}",
                        reloaded.getSlug(),
                        image.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpegBytes()));
    }

    private void upload(Recipe recipe, MockMultipartFile file) throws Exception {
        mockMvc.perform(multipart("/api/admin/recipes/{recipeId}/images", recipe.getId())
                        .file(file)
                        .with(user("sergio").authorities(() -> "ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    private Recipe persistRecipe(String slug, RecipeStatus status) {
        Recipe recipe = new Recipe(
                slug,
                "Admin Media Fixture",
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

        return recipeRepository.saveAndFlush(recipe);
    }

    private Recipe persistCompleteRecipe(String slug) {
        Recipe recipe = persistRecipe(slug, RecipeStatus.DRAFT);
        recipe.addIngredient(new RecipeIngredient(0, "Ingrediente"));
        recipe.addStep(new RecipeStep(0, "Paso"));
        return recipeRepository.saveAndFlush(recipe);
    }

    private MockMultipartFile jpeg(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "image/jpeg",
                jpegBytes());
    }

    private MockMultipartFile png(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A,
                        0x01, 0x02, 0x03
                });
    }

    private MockMultipartFile webp(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "image/webp",
                new byte[] {
                        'R', 'I', 'F', 'F',
                        0x04, 0x00, 0x00, 0x00,
                        'W', 'E', 'B', 'P',
                        0x01
                });
    }

    private byte[] jpegBytes() {
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF,
                (byte) 0xE0, 0x00, 0x10, 0x01, 0x02
        };
    }

    private byte[] oversizedJpegBytes() {
        byte[] bytes = new byte[(5 * 1024 * 1024) + 1];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }
}
