package com.elrecetariodeshir.backend.excelimport.review;

public record RecipeLegacyCollision(
        String sourceSheet,
        String excelName,
        String excelSlug,
        String legacyName,
        MatchType matchType) {

    public enum MatchType {
        NAME_AND_SLUG,
        NAME_ONLY,
        SLUG_ONLY
    }
}
