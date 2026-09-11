package com.elrecetariodeshir.backend.excelimport.review;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.elrecetariodeshir.backend.excelimport.RecipeImportCandidate;

public final class RecipeImportFingerprint {

    private RecipeImportFingerprint() {
    }

    public static String from(RecipeImportCandidate candidate) {
        StringBuilder source = new StringBuilder();

        append(source, candidate.sourceWorkbook());
        append(source, candidate.sourceSheet());
        append(source, candidate.sourceRecipeNumber());
        append(source, candidate.originalName());
        append(source, candidate.normalizedName());
        append(source, candidate.slugCandidate());

        if (candidate.yield() != null) {
            append(source, candidate.yield().quantity());
            append(source, candidate.yield().min());
            append(source, candidate.yield().max());
            append(source, candidate.yield().unit());
            append(source, candidate.yield().displayText());
        }

        candidate.ingredients().forEach(ingredient -> {
            append(source, ingredient.position());
            append(source, ingredient.ingredientName());
            append(source, ingredient.quantity());
            append(source, ingredient.quantityMax());
            append(source, ingredient.unit());
            append(source, ingredient.notes());
            append(source, ingredient.displayText());
        });

        candidate.steps().forEach(step -> {
            append(source, step.position());
            append(source, step.instruction());
        });

        append(source, candidate.imageReference());
        append(source, candidate.imageExists());

        return sha256(source.toString());
    }

    private static void append(StringBuilder target, Object value) {
        String text = value == null ? "<null>" : value.toString();

        target.append(text.length())
                .append(':')
                .append(text)
                .append('|');
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception);
        }
    }
}
