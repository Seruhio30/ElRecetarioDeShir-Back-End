package com.elrecetariodeshir.backend.legacyimport;

public record LegacyRecipeImportResult(
        Status status,
        int recipes,
        int ingredients,
        int steps,
        int images) {

    public enum Status {
        IMPORTED,
        NO_OP
    }
}
