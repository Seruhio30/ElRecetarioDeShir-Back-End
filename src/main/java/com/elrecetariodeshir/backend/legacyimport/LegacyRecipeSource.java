package com.elrecetariodeshir.backend.legacyimport;

import java.util.List;

public record LegacyRecipeSource(
        String category,
        String country,
        String type,
        String name,
        String cuisine,
        List<String> ingredients,
        String time,
        String difficulty,
        List<String> steps,
        String image) {
}
