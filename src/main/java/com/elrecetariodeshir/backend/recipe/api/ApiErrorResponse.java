package com.elrecetariodeshir.backend.recipe.api;

public record ApiErrorResponse(
        String status,
        String message) {
}
