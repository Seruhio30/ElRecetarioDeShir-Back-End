package com.elrecetariodeshir.backend.admin.recipe;

import java.io.IOException;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.elrecetariodeshir.backend.media.storage.MediaStorageService;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeImage;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;

@Service
public class AdminRecipeImageService {

    private final RecipeRepository recipeRepository;
    private final MediaStorageService mediaStorageService;
    private final AdminRecipeImageUploadValidator uploadValidator;

    public AdminRecipeImageService(
            RecipeRepository recipeRepository,
            MediaStorageService mediaStorageService,
            AdminRecipeImageUploadValidator uploadValidator) {
        this.recipeRepository = recipeRepository;
        this.mediaStorageService = mediaStorageService;
        this.uploadValidator = uploadValidator;
    }

    @Transactional
    public AdminRecipeImageResponse upload(Long recipeId, MultipartFile file) {
        Recipe recipe = findRecipe(recipeId);

        AdminRecipeImageUploadValidator.ValidatedImage validated =
                uploadValidator.validate(file);

        String storageKey;

        try {
            storageKey = mediaStorageService.store(
                    file.getInputStream(),
                    validated.extension());
        } catch (IOException exception) {
            throw new AdminRecipeValidationException("Unable to read image file.");
        }

        try {
            int nextPosition = recipe.getImages().stream()
                    .mapToInt(RecipeImage::getPosition)
                    .max()
                    .orElse(-1) + 1;

            boolean firstImage = recipe.getImages().isEmpty();

            RecipeImage image = new RecipeImage(
                    storageKey,
                    validated.originalFilename(),
                    validated.mediaType(),
                    null,
                    nextPosition,
                    firstImage);

            recipe.addImage(image);

            Recipe savedRecipe = recipeRepository.saveAndFlush(recipe);

            RecipeImage savedImage = savedRecipe.getImages().stream()
                    .filter(candidate -> storageKey.equals(candidate.getStorageKey()))
                    .findFirst()
                    .orElseThrow();

            return toResponse(savedRecipe, savedImage);
        } catch (RuntimeException exception) {
            mediaStorageService.delete(storageKey);
            throw exception;
        }
    }

    @Transactional
    public AdminRecipeImageResponse update(
            Long recipeId,
            Long imageId,
            AdminRecipeImageUpdateRequest request) {

        Recipe recipe = findRecipe(recipeId);
        RecipeImage image = findImage(recipe, imageId);

        image.setAltText(normalizeAltText(request.altText()));

        if (Boolean.TRUE.equals(request.primaryImage())) {
            recipe.getImages().forEach(candidate ->
                    candidate.setPrimaryImage(candidate == image));
        } else if (Boolean.FALSE.equals(request.primaryImage())) {
            image.setPrimaryImage(false);
        }

        recipeRepository.flush();

        return toResponse(recipe, image);
    }

    @Transactional
    public void delete(Long recipeId, Long imageId) {
        Recipe recipe = findRecipe(recipeId);
        RecipeImage image = findImage(recipe, imageId);

        String storageKey = image.getStorageKey();
        boolean wasPrimary = image.isPrimaryImage();

        recipe.removeImage(image);

        if (wasPrimary && !recipe.getImages().isEmpty()) {
            recipe.getImages().stream()
                    .min(Comparator.comparingInt(RecipeImage::getPosition))
                    .orElseThrow()
                    .setPrimaryImage(true);
        }

        recipeRepository.flush();

        if (!mediaStorageService.delete(storageKey)) {
            throw new IllegalStateException("Stored image file was not found.");
        }
    }

    @Transactional(readOnly = true)
    public AdminRecipeMediaResponse getContent(Long recipeId, Long imageId) {
        Recipe recipe = findRecipe(recipeId);
        RecipeImage image = findImage(recipe, imageId);

        if (!mediaStorageService.exists(image.getStorageKey())) {
            throw new AdminRecipeNotFoundException();
        }

        return new AdminRecipeMediaResponse(
                mediaStorageService.read(image.getStorageKey()),
                image.getMediaType());
    }

    private Recipe findRecipe(Long recipeId) {
        if (recipeId == null || recipeId <= 0) {
            throw new AdminRecipeNotFoundException();
        }

        return recipeRepository.findById(recipeId)
                .orElseThrow(AdminRecipeNotFoundException::new);
    }

    private RecipeImage findImage(Recipe recipe, Long imageId) {
        if (imageId == null || imageId <= 0) {
            throw new AdminRecipeNotFoundException();
        }

        return recipe.getImages().stream()
                .filter(candidate -> imageId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(AdminRecipeNotFoundException::new);
    }

    private String normalizeAltText(String altText) {
        if (altText == null) {
            return null;
        }

        String normalized = altText.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private AdminRecipeImageResponse toResponse(
            Recipe recipe,
            RecipeImage image) {

        return new AdminRecipeImageResponse(
                image.getId(),
                "/api/admin/recipes/" + recipe.getId()
                        + "/images/" + image.getId() + "/content",
                image.getOriginalFilename(),
                image.getMediaType(),
                image.getAltText(),
                image.getPosition(),
                image.isPrimaryImage(),
                image.getCreatedAt());
    }
}
