package com.elrecetariodeshir.backend.excelimport.review;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.elrecetariodeshir.backend.excelimport.RecipeImportCandidate;
import com.elrecetariodeshir.backend.recipe.RecipeSlugifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RecipeLegacyCollisionDetector {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(
                            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            false);

    public List<RecipeLegacyCollision> detect(
            List<RecipeImportCandidate> candidates,
            Path legacyJsonPath) {

        List<LegacyRecipeIdentity> legacy =
                readLegacyIdentities(legacyJsonPath);

        List<RecipeLegacyCollision> collisions =
                new ArrayList<>();

        for (RecipeImportCandidate candidate : candidates) {
            String candidateName =
                    normalize(candidate.normalizedName());

            String candidateSlug =
                    candidate.slugCandidate();

            for (LegacyRecipeIdentity existing : legacy) {
                boolean nameMatch = candidateName.equals(
                        normalize(existing.name()));

                boolean slugMatch = candidateSlug.equals(
                        RecipeSlugifier.slugify(existing.name()));

                if (!nameMatch && !slugMatch) {
                    continue;
                }

                RecipeLegacyCollision.MatchType matchType;

                if (nameMatch && slugMatch) {
                    matchType =
                            RecipeLegacyCollision.MatchType.NAME_AND_SLUG;
                } else if (nameMatch) {
                    matchType =
                            RecipeLegacyCollision.MatchType.NAME_ONLY;
                } else {
                    matchType =
                            RecipeLegacyCollision.MatchType.SLUG_ONLY;
                }

                collisions.add(new RecipeLegacyCollision(
                        candidate.sourceSheet(),
                        candidate.normalizedName(),
                        candidate.slugCandidate(),
                        existing.name(),
                        matchType));

                break;
            }
        }

        return List.copyOf(collisions);
    }

    private List<LegacyRecipeIdentity> readLegacyIdentities(
            Path legacyJsonPath) {

        try {
            return objectMapper.readValue(
                    legacyJsonPath.toFile(),
                    new TypeReference<
                            List<LegacyRecipeIdentity>>() {
                    });
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read legacy recipe catalog: "
                            + legacyJsonPath,
                    exception);
        }
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private record LegacyRecipeIdentity(String name) {
    }
}
