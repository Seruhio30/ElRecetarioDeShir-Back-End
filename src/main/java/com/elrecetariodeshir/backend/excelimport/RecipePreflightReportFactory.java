package com.elrecetariodeshir.backend.excelimport;

import java.math.BigDecimal;

import com.elrecetariodeshir.backend.recipe.RecipeIngredientUnit;
import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

public final class RecipePreflightReportFactory {

    private RecipePreflightReportFactory() {
    }

    public static RecipePreflightReport from(
            RecipeExcelPreflightService.PreflightResult result) {

        long structureReady = result.candidates().stream()
                .filter(candidate -> candidate.status() != RecipePreflightStatus.INVALID)
                .filter(candidate -> candidate.warnings().stream()
                        .allMatch(RecipePreflightReportFactory::isMetadataWarning))
                .count();

        long structureReviewRequired = result.candidates().stream()
                .filter(candidate -> candidate.status() != RecipePreflightStatus.INVALID)
                .filter(candidate -> candidate.warnings().stream()
                        .anyMatch(warning -> !isMetadataWarning(warning)))
                .count();

        long simpleYields = result.candidates().stream()
                .filter(candidate -> {
                    var yield = candidate.yield();
                    return yield != null
                            && yield.quantity() != null
                            && yield.min() == null
                            && yield.max() == null
                            && yield.unit() == null;
                })
                .count();

        long rangeYields = result.candidates().stream()
                .filter(candidate -> {
                    var yield = candidate.yield();
                    return yield != null
                            && yield.min() != null
                            && yield.max() != null;
                })
                .count();

        long unitYields = result.candidates().stream()
                .filter(candidate -> {
                    var yield = candidate.yield();
                    return yield != null
                            && yield.quantity() != null
                            && yield.unit() != null
                            && yield.unit() != RecipeYieldUnit.OTHER;
                })
                .count();

        long otherYields = result.candidates().stream()
                .filter(candidate -> {
                    var yield = candidate.yield();
                    return yield != null
                            && yield.unit() == RecipeYieldUnit.OTHER;
                })
                .count();

        long unresolvedYields = result.candidates().stream()
                .filter(candidate -> candidate.yield() == null
                        || (candidate.yield().quantity() == null
                        && candidate.yield().min() == null
                        && candidate.yield().max() == null))
                .count();

        long unknownIngredientUnits = result.candidates().stream()
                .flatMap(candidate -> candidate.ingredients().stream())
                .filter(ingredient ->
                        ingredient.unit() == RecipeIngredientUnit.OTHER)
                .count();

        long zeroIngredientQuantities = result.candidates().stream()
                .flatMap(candidate -> candidate.ingredients().stream())
                .filter(ingredient ->
                        ingredient.quantity() != null
                                && ingredient.quantity()
                                        .compareTo(BigDecimal.ZERO) == 0)
                .count();

        long missingIngredientQuantities = result.candidates().stream()
                .flatMap(candidate -> candidate.ingredients().stream())
                .filter(ingredient -> ingredient.quantity() == null)
                .count();

        long missingIngredientUnits = result.candidates().stream()
                .flatMap(candidate -> candidate.ingredients().stream())
                .filter(ingredient -> ingredient.unit() == null)
                .count();

        long missingImages = result.candidates().stream()
                .filter(candidate -> !Boolean.TRUE.equals(candidate.imageExists()))
                .count();

        return new RecipePreflightReport(
                result.workbook(),
                result.totalSheets(),
                result.candidates().size(),
                result.count(RecipePreflightStatus.READY),
                result.count(RecipePreflightStatus.REVIEW_REQUIRED),
                result.count(RecipePreflightStatus.INVALID),
                structureReady,
                structureReviewRequired,
                result.ingredientRowsDetected(),
                result.ingredientCount(),
                result.stepCount(),
                result.uniqueSlugCount(),
                simpleYields,
                rangeYields,
                unitYields,
                otherYields,
                unresolvedYields,
                unknownIngredientUnits,
                zeroIngredientQuantities,
                missingIngredientQuantities,
                missingIngredientUnits,
                missingImages,
                result.candidates());
    }
    private static boolean isMetadataWarning(String warning) {
        return "Recipe category requires review.".equals(warning)
                || "Recipe countryCode requires review.".equals(warning)
                || "Recipe type requires review.".equals(warning)
                || "Recipe difficulty requires review.".equals(warning);
    }

}
