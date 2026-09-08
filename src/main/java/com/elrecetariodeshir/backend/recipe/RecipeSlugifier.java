package com.elrecetariodeshir.backend.recipe;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RecipeSlugifier {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private RecipeSlugifier() {
    }

    public static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(
                value.trim(),
                Normalizer.Form.NFD);

        String withoutMarks = normalized.replaceAll("\\p{M}+", "");
        String lowercase = withoutMarks.toLowerCase(Locale.ROOT);

        return NON_SLUG.matcher(lowercase)
                .replaceAll("-")
                .replaceAll("^-+|-+$", "");
    }
}
