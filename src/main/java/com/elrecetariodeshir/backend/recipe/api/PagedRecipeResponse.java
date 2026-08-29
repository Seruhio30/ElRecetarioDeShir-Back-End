package com.elrecetariodeshir.backend.recipe.api;

import java.util.List;

public record PagedRecipeResponse(
        List<RecipeSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {
}
