package com.elrecetariodeshir.backend.excelimport.review;

public class RecipeImportReviewException extends RuntimeException {

    public RecipeImportReviewException(String message) {
        super(message);
    }

    public RecipeImportReviewException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
