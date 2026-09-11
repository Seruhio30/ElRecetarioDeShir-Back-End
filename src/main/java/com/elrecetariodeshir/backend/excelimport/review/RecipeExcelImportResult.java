package com.elrecetariodeshir.backend.excelimport.review;

public record RecipeExcelImportResult(
        int imported,
        int skippedAlreadyImported,
        int pending,
        int conflicts,
        int partialStates) {
}
