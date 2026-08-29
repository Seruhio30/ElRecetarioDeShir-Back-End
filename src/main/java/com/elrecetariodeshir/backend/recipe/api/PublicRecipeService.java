package com.elrecetariodeshir.backend.recipe.api;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeImage;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeSpecifications;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeType;
import com.elrecetariodeshir.backend.media.storage.MediaStorageService;

@Service
@Transactional(readOnly = true)
public class PublicRecipeService {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 12;
    public static final int MAX_SIZE = 24;

    private final RecipeRepository recipeRepository;
    private final MediaStorageService mediaStorageService;

    public PublicRecipeService(
            RecipeRepository recipeRepository,
            MediaStorageService mediaStorageService) {
        this.recipeRepository = recipeRepository;
        this.mediaStorageService = mediaStorageService;
    }

    public PagedRecipeResponse listRecipes(
            int page,
            int size,
            String category,
            String country,
            String type,
            String difficulty,
            String queryText) {

        validatePagination(page, size);

        RecipeCategory parsedCategory = parseEnum(category, RecipeCategory.class);
        RecipeType parsedType = parseEnum(type, RecipeType.class);
        RecipeDifficulty parsedDifficulty = parseEnum(difficulty, RecipeDifficulty.class);
        String normalizedCountry = normalizeCountry(country);
        String normalizedQuery = normalizeQuery(queryText);

        Specification<Recipe> specification = RecipeSpecifications.isPublished()
                .and(RecipeSpecifications.hasCategory(parsedCategory))
                .and(RecipeSpecifications.hasCountryCode(normalizedCountry))
                .and(RecipeSpecifications.hasType(parsedType))
                .and(RecipeSpecifications.hasDifficulty(parsedDifficulty))
                .and(RecipeSpecifications.nameContainsIgnoreCase(normalizedQuery));

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("publishedAt"),
                        Sort.Order.desc("id")));

        Page<Recipe> result = recipeRepository.findAll(specification, pageRequest);

        return new PagedRecipeResponse(
                result.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext());
    }

    public RecipeDetailResponse getRecipeBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);

        Specification<Recipe> specification = RecipeSpecifications.isPublished()
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("slug"), normalizedSlug));

        Recipe recipe = recipeRepository.findOne(specification)
                .orElseThrow(PublicRecipeNotFoundException::new);

        return toDetail(recipe);
    }

    public PublicRecipeMediaResponse getRecipeImage(String slug, Long imageId) {
        if (imageId == null || imageId <= 0) {
            throw new PublicRecipeNotFoundException();
        }

        String normalizedSlug = normalizeSlug(slug);

        Specification<Recipe> specification = RecipeSpecifications.isPublished()
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("slug"), normalizedSlug));

        Recipe recipe = recipeRepository.findOne(specification)
                .orElseThrow(PublicRecipeNotFoundException::new);

        RecipeImage image = recipe.getImages().stream()
                .filter(candidate -> imageId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(PublicRecipeNotFoundException::new);

        if (!mediaStorageService.exists(image.getStorageKey())) {
            throw new PublicRecipeNotFoundException();
        }

        return new PublicRecipeMediaResponse(
                mediaStorageService.read(image.getStorageKey()),
                image.getMediaType());
    }

    private RecipeSummaryResponse toSummary(Recipe recipe) {
        RecipeImage primaryImage = recipe.getImages().stream()
                .filter(RecipeImage::isPrimaryImage)
                .findFirst()
                .orElseGet(() -> recipe.getImages().stream().findFirst().orElse(null));

        return new RecipeSummaryResponse(
                recipe.getSlug(),
                recipe.getName(),
                recipe.getCuisine(),
                recipe.getCategory(),
                recipe.getCountryCode(),
                recipe.getType(),
                recipe.getDifficulty(),
                recipe.getTime(),
                primaryImage == null ? null : toImage(recipe.getSlug(), primaryImage),
                recipe.getPublishedAt());
    }

    private RecipeDetailResponse toDetail(Recipe recipe) {
        List<RecipeIngredientResponse> ingredients = recipe.getIngredients().stream()
                .sorted(java.util.Comparator.comparingInt(
                        ingredient -> ingredient.getPosition()))
                .map(ingredient -> new RecipeIngredientResponse(
                        ingredient.getPosition(),
                        ingredient.getText()))
                .toList();

        List<RecipeStepResponse> steps = recipe.getSteps().stream()
                .sorted(java.util.Comparator.comparingInt(
                        step -> step.getPosition()))
                .map(step -> new RecipeStepResponse(
                        step.getPosition(),
                        step.getInstruction()))
                .toList();

        List<RecipeImageResponse> images = recipe.getImages().stream()
                .sorted(java.util.Comparator.comparingInt(
                        image -> image.getPosition()))
                .map(image -> toImage(recipe.getSlug(), image))
                .toList();

        return new RecipeDetailResponse(
                recipe.getSlug(),
                recipe.getName(),
                recipe.getCuisine(),
                recipe.getCategory(),
                recipe.getCountryCode(),
                recipe.getType(),
                recipe.getDifficulty(),
                recipe.getTime(),
                ingredients,
                steps,
                images,
                recipe.getPublishedAt());
    }

    private RecipeImageResponse toImage(String slug, RecipeImage image) {
        return new RecipeImageResponse(
                image.getId(),
                "/api/recipes/" + slug + "/images/" + image.getId(),
                image.getAltText(),
                image.isPrimaryImage());
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Invalid page");
        }

        if (size <= 0 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Invalid size");
        }
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }

        String normalized = country.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Invalid country");
        }

        return normalized;
    }

    private String normalizeQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return null;
        }

        return queryText.trim();
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new PublicRecipeNotFoundException();
        }

        return slug.trim();
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(
                    enumType,
                    value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid filter");
        }
    }
}
