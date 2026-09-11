package com.elrecetariodeshir.backend.excelimport.review;

import java.util.List;

public record RecipeImportPlan(
        int detected,
        int approved,
        int pending,
        int newRecipes,
        int alreadyImported,
        int conflicts,
        int partialStates,
        int withImage,
        int withoutImage,
        int ingredients,
        int steps,
        List<Entry> entries) {

    public RecipeImportPlan {
        entries = List.copyOf(entries);
    }

    public enum Status {
        PENDING_REVIEW,
        NEW,
        ALREADY_IMPORTED,
        CONFLICT,
        PARTIAL_STATE
    }

    public record Entry(
            ReviewedRecipeImportCandidate candidate,
            Status status,
            Long existingRecipeId,
            String reason) {
    }
}
