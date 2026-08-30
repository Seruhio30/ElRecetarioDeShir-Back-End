package com.elrecetariodeshir.backend.admin.recipe;

import java.util.List;

public record AdminPagedRecipeResponse(
        List<AdminRecipeSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {
}
