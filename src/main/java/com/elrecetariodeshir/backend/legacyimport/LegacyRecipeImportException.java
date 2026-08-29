package com.elrecetariodeshir.backend.legacyimport;

public class LegacyRecipeImportException extends RuntimeException {

    public LegacyRecipeImportException(String message) {
        super(message);
    }

    public LegacyRecipeImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
