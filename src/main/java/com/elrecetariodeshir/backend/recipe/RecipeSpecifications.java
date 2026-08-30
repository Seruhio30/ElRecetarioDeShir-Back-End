package com.elrecetariodeshir.backend.recipe;

import org.springframework.data.jpa.domain.Specification;

public final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    public static Specification<Recipe> isPublished() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), RecipeStatus.PUBLISHED);
    }

    public static Specification<Recipe> hasStatus(RecipeStatus status) {
        return optionalEquals("status", status);
    }

    public static Specification<Recipe> hasCategory(RecipeCategory category) {
        return optionalEquals("category", category);
    }

    public static Specification<Recipe> hasCountryCode(String countryCode) {
        return optionalEquals("countryCode", countryCode);
    }

    public static Specification<Recipe> hasType(RecipeType type) {
        return optionalEquals("type", type);
    }

    public static Specification<Recipe> hasDifficulty(RecipeDifficulty difficulty) {
        return optionalEquals("difficulty", difficulty);
    }

    public static Specification<Recipe> nameContainsIgnoreCase(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%" + queryText.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        pattern);
    }

    private static <T> Specification<Recipe> optionalEquals(String attribute, T value) {
        if (value == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(attribute), value);
    }
}
