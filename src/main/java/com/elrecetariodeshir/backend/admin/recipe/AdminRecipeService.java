package com.elrecetariodeshir.backend.admin.recipe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeIngredient;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;
import com.elrecetariodeshir.backend.recipe.RecipeSpecifications;
import com.elrecetariodeshir.backend.recipe.RecipeSlugifier;
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeStep;

@Service
public class AdminRecipeService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final RecipeRepository recipeRepository;

    public AdminRecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public AdminPagedRecipeResponse listRecipes(
            int page,
            int size,
            RecipeStatus status,
            String queryText) {

        validatePagination(page, size);

        String normalizedQuery = normalizeQuery(queryText);

        Specification<Recipe> specification =
                RecipeSpecifications.hasStatus(status)
                        .and(RecipeSpecifications.nameContainsIgnoreCase(normalizedQuery));

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("id")));

        Page<Recipe> result =
                recipeRepository.findAll(specification, pageRequest);

        return new AdminPagedRecipeResponse(
                result.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext());
    }

    @Transactional(readOnly = true)
    public AdminRecipeDetailResponse getRecipe(Long id) {
        return toDetail(findRecipe(id));
    }

    @Transactional
    public AdminRecipeDetailResponse createRecipe(
            AdminRecipeWriteRequest request) {

        validateCollectionPositions(request);
        validateYield(request);
        validateIngredients(request);

        String slug = generateUniqueSlug(request.name());

        Recipe recipe = new Recipe(
                slug,
                normalizeRequired(request.name()),
                normalizeOptional(request.cuisine()),
                request.category(),
                normalizeCountry(request.countryCode()),
                request.type(),
                request.difficulty(),
                request.time(),
                RecipeStatus.DRAFT);

        recipe.setYieldQuantity(request.yieldQuantity());
        recipe.setYieldMin(request.yieldMin());
        recipe.setYieldMax(request.yieldMax());
        recipe.setYieldUnit(request.yieldUnit());
        recipe.setYieldDisplay(normalizeOptional(request.yieldDisplay()));

        recipe.replaceIngredients(toIngredients(request));
        recipe.replaceSteps(toSteps(request));

        Recipe saved = recipeRepository.save(recipe);

        return toDetail(saved);
    }

    @Transactional
    public AdminRecipeDetailResponse updateRecipe(
            Long id,
            AdminRecipeWriteRequest request) {

        validateCollectionPositions(request);
        validateYield(request);
        validateIngredients(request);

        Recipe recipe = findRecipe(id);

        recipe.setName(normalizeRequired(request.name()));
        recipe.setCuisine(normalizeOptional(request.cuisine()));
        recipe.setCategory(request.category());
        recipe.setCountryCode(normalizeCountry(request.countryCode()));
        recipe.setType(request.type());
        recipe.setDifficulty(request.difficulty());
        recipe.setTime(request.time());
        recipe.setYieldQuantity(request.yieldQuantity());
        recipe.setYieldMin(request.yieldMin());
        recipe.setYieldMax(request.yieldMax());
        recipe.setYieldUnit(request.yieldUnit());
        recipe.setYieldDisplay(normalizeOptional(request.yieldDisplay()));

        recipe.clearIngredients();
        recipe.clearSteps();

        recipeRepository.flush();

        recipe.clearIngredients();
        recipe.clearSteps();

        recipeRepository.flush();

        recipe.replaceIngredients(toIngredients(request));
        recipe.replaceSteps(toSteps(request));

        return toDetail(recipe);
    }

    @Transactional
    public AdminRecipeDetailResponse publish(Long id) {
        Recipe recipe = findRecipe(id);

        if (recipe.getStatus() != RecipeStatus.DRAFT) {
            throw new AdminRecipeConflictException(
                    "Recipe cannot be published from current status.");
        }

        validatePublishable(recipe);

        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setPublishedAt(Instant.now());
        recipe.setArchivedAt(null);

        return toDetail(recipe);
    }

    @Transactional
    public AdminRecipeDetailResponse archive(Long id) {
        Recipe recipe = findRecipe(id);

        if (recipe.getStatus() != RecipeStatus.PUBLISHED) {
            throw new AdminRecipeConflictException(
                    "Recipe cannot be archived from current status.");
        }

        recipe.setStatus(RecipeStatus.ARCHIVED);
        recipe.setArchivedAt(Instant.now());

        return toDetail(recipe);
    }

    @Transactional
    public AdminRecipeDetailResponse restore(Long id) {
        Recipe recipe = findRecipe(id);

        if (recipe.getStatus() != RecipeStatus.ARCHIVED) {
            throw new AdminRecipeConflictException(
                    "Recipe cannot be restored from current status.");
        }

        recipe.setStatus(RecipeStatus.DRAFT);
        recipe.setArchivedAt(null);

        return toDetail(recipe);
    }

    private Recipe findRecipe(Long id) {
        if (id == null || id <= 0) {
            throw new AdminRecipeNotFoundException();
        }

        return recipeRepository.findById(id)
                .orElseThrow(AdminRecipeNotFoundException::new);
    }

    private void validatePublishable(Recipe recipe) {
        if (recipe.getName() == null || recipe.getName().isBlank()
                || recipe.getSlug() == null || recipe.getSlug().isBlank()
                || recipe.getCategory() == null
                || recipe.getCountryCode() == null
                || !recipe.getCountryCode().matches("[A-Z]{2}")
                || recipe.getType() == null
                || recipe.getDifficulty() == null
                || recipe.getIngredients().isEmpty()
                || recipe.getSteps().isEmpty()) {

            throw new AdminRecipeValidationException(
                    "Recipe is incomplete and cannot be published.");
        }
    }

    private void validateYield(AdminRecipeWriteRequest request) {
        if (isNegative(request.yieldQuantity())
                || isNegative(request.yieldMin())
                || isNegative(request.yieldMax())) {
            throw new AdminRecipeValidationException(
                    "Recipe yield values cannot be negative.");
        }

        if (request.yieldMin() != null
                && request.yieldMax() != null
                && request.yieldMin().compareTo(request.yieldMax()) > 0) {
            throw new AdminRecipeValidationException(
                    "Recipe yield minimum cannot exceed maximum.");
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private void validateIngredients(AdminRecipeWriteRequest request) {
        for (AdminRecipeIngredientRequest ingredient : request.ingredients()) {
            if (resolveDisplayText(ingredient) == null) {
                throw new AdminRecipeValidationException(
                        "Ingredient display text is required.");
            }

            if (isNegative(ingredient.quantity())
                    || isNegative(ingredient.quantityMax())) {
                throw new AdminRecipeValidationException(
                        "Ingredient quantities cannot be negative.");
            }

            if (ingredient.quantity() != null
                    && ingredient.quantityMax() != null
                    && ingredient.quantityMax().compareTo(ingredient.quantity()) < 0) {
                throw new AdminRecipeValidationException(
                        "Ingredient maximum quantity cannot be less than quantity.");
            }
        }
    }

    private String resolveDisplayText(AdminRecipeIngredientRequest ingredient) {
        String displayText = normalizeOptional(ingredient.displayText());

        if (displayText != null) {
            return displayText;
        }

        return normalizeOptional(ingredient.text());
    }

    private void validateCollectionPositions(AdminRecipeWriteRequest request) {
        validatePositions(
                request.ingredients().stream()
                        .map(AdminRecipeIngredientRequest::position)
                        .toList(),
                "ingredients");

        validatePositions(
                request.steps().stream()
                        .map(AdminRecipeStepRequest::position)
                        .toList(),
                "steps");
    }

    private void validatePositions(List<Integer> positions, String field) {
        long unique = positions.stream().distinct().count();

        if (unique != positions.size()) {
            throw new AdminRecipeValidationException(
                    "Duplicate " + field + " positions.");
        }
    }

    private List<RecipeIngredient> toIngredients(
            AdminRecipeWriteRequest request) {

        return request.ingredients().stream()
                .sorted(Comparator.comparingInt(
                        AdminRecipeIngredientRequest::position))
                .map(item -> new RecipeIngredient(
                        item.position(),
                        normalizeOptional(item.ingredientName()),
                        item.quantity(),
                        item.quantityMax(),
                        item.unit(),
                        normalizeOptional(item.notes()),
                        resolveDisplayText(item)))
                .toList();
    }

    private List<RecipeStep> toSteps(
            AdminRecipeWriteRequest request) {

        return request.steps().stream()
                .sorted(Comparator.comparingInt(
                        AdminRecipeStepRequest::position))
                .map(item -> new RecipeStep(
                        item.position(),
                        item.instruction().trim()))
                .toList();
    }

    private String generateUniqueSlug(String name) {
        String base = RecipeSlugifier.slugify(normalizeRequired(name));

        if (base.isBlank()) {
            throw new AdminRecipeValidationException(
                    "Recipe name cannot produce a valid slug.");
        }

        String candidate = base;
        int suffix = 2;

        while (recipeRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new AdminRecipeValidationException(
                    "Required recipe value is missing.");
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeCountry(String value) {
        String country = normalizeRequired(value)
                .toUpperCase(Locale.ROOT);

        if (!country.matches("[A-Z]{2}")) {
            throw new AdminRecipeValidationException(
                    "Invalid country code.");
        }

        return country;
    }

    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new AdminRecipeValidationException("Invalid page.");
        }

        if (size <= 0 || size > MAX_SIZE) {
            throw new AdminRecipeValidationException("Invalid size.");
        }
    }

    private AdminRecipeSummaryResponse toSummary(Recipe recipe) {
        return new AdminRecipeSummaryResponse(
                recipe.getId(),
                recipe.getSlug(),
                recipe.getName(),
                recipe.getCuisine(),
                recipe.getCategory(),
                recipe.getCountryCode(),
                recipe.getType(),
                recipe.getDifficulty(),
                recipe.getTime(),
                recipe.getStatus(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                recipe.getPublishedAt(),
                recipe.getArchivedAt());
    }

    private AdminRecipeDetailResponse toDetail(Recipe recipe) {
        return new AdminRecipeDetailResponse(
                recipe.getId(),
                recipe.getSlug(),
                recipe.getName(),
                recipe.getCuisine(),
                recipe.getCategory(),
                recipe.getCountryCode(),
                recipe.getType(),
                recipe.getDifficulty(),
                recipe.getTime(),
                recipe.getYieldQuantity(),
                recipe.getYieldMin(),
                recipe.getYieldMax(),
                recipe.getYieldUnit(),
                recipe.getYieldDisplay(),
                recipe.getStatus(),
                recipe.getIngredients().stream()
                        .sorted(Comparator.comparingInt(
                                RecipeIngredient::getPosition))
                        .map(item -> new AdminRecipeIngredientResponse(
                                item.getPosition(),
                                item.getText(),
                                item.getIngredientName(),
                                item.getQuantity(),
                                item.getQuantityMax(),
                                item.getUnit(),
                                item.getNotes(),
                                item.getDisplayText()))
                        .toList(),
                recipe.getSteps().stream()
                        .sorted(Comparator.comparingInt(
                                RecipeStep::getPosition))
                        .map(item -> new AdminRecipeStepResponse(
                                item.getPosition(),
                                item.getInstruction()))
                        .toList(),
                recipe.getImages().stream()
                        .sorted(Comparator.comparingInt(
                                image -> image.getPosition()))
                        .map(image -> new AdminRecipeImageResponse(
                                image.getId(),
                                "/api/admin/recipes/" + recipe.getId()
                                        + "/images/" + image.getId() + "/content",
                                image.getOriginalFilename(),
                                image.getMediaType(),
                                image.getAltText(),
                                image.getPosition(),
                                image.isPrimaryImage(),
                                image.getCreatedAt()))
                        .toList(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                recipe.getPublishedAt(),
                recipe.getArchivedAt());
    }
}
