package com.elrecetariodeshir.backend.excelimport;

import java.util.List;

public record RecipePreflightReport(
        String workbook,
        int totalSheets,
        int recipesDetected,
        long ready,
        long reviewRequired,
        long invalid,
        long structureReady,
        long structureReviewRequired,
        int ingredientRowsDetected,
        int normalizedIngredients,
        int totalSteps,
        long uniqueSlugs,
        long simpleYields,
        long rangeYields,
        long unitYields,
        long otherYields,
        long unresolvedYields,
        long unknownIngredientUnits,
        long zeroIngredientQuantities,
        long missingIngredientQuantities,
        long missingIngredientUnits,
        long missingImages,
        List<RecipeImportCandidate> candidates) {

    public RecipePreflightReport {
        candidates = List.copyOf(candidates);
    }
}
