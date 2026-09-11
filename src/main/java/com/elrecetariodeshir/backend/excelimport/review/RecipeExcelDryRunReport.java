package com.elrecetariodeshir.backend.excelimport.review;

import java.util.List;

public record RecipeExcelDryRunReport(
        RecipeImportPlan plan,
        int legacyCollisionCount,
        List<RecipeLegacyCollision> legacyCollisions) {

    public RecipeExcelDryRunReport {
        legacyCollisions = List.copyOf(legacyCollisions);
    }
}
