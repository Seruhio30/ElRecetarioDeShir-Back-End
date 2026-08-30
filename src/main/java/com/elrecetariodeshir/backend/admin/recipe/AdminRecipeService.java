package com.elrecetariodeshir.backend.admin.recipe;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
import com.elrecetariodeshir.backend.recipe.RecipeStatus;
import com.elrecetariodeshir.backend.recipe.RecipeStep;

@Service
public class AdminRecipeService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

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

        Recipe recipe = findRecipe(id);

        recipe.setName(normalizeRequired(request.name()));
        recipe.setCuisine(normalizeOptional(request.cuisine()));
        recipe.setCategory(request.category());
        recipe.setCountryCode(normalizeCountry(request.countryCode()));
        recipe.setType(request.type());
        recipe.setDifficulty(request.difficulty());
        recipe.setTime(request.time());

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
                        item.text().trim()))
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
        String base = slugify(name);

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

    private String slugify(String value) {
        String normalized = Normalizer.normalize(
                normalizeRequired(value),
                Normalizer.Form.NFD);

        String withoutMarks = normalized.replaceAll("\\p{M}+", "");
        String lowercase = withoutMarks.toLowerCase(Locale.ROOT);

        return NON_SLUG.matcher(lowercase)
                .replaceAll("-")
                .replaceAll("^-+|-+$", "");
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
                recipe.getStatus(),
                recipe.getIngredients().stream()
                        .sorted(Comparator.comparingInt(
                                RecipeIngredient::getPosition))
                        .map(item -> new AdminRecipeIngredientResponse(
                                item.getPosition(),
                                item.getText()))
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
