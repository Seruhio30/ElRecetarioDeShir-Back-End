package com.elrecetariodeshir.backend.legacyimport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;

@Service
public class LegacyRecipePreflightService {

    private static final Map<String, String> COUNTRY_CODES = Map.of(
            "Costa Rica", "CR",
            "España", "ES",
            "Francia", "FR",
            "Perú", "PE",
            "México", "MX",
            "Japón", "JP");

    private static final Map<String, RecipeDifficulty> DIFFICULTIES = Map.of(
            "Fácil", RecipeDifficulty.EASY,
            "Media", RecipeDifficulty.MEDIUM,
            "Alta", RecipeDifficulty.HARD);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<PreparedLegacyRecipe> prepare(Path jsonPath, Path assetsRoot) {
        validateSourcePaths(jsonPath, assetsRoot);

        List<LegacyRecipeSource> sourceRecipes = readSource(jsonPath);

        if (sourceRecipes.size() != LegacyRecipeCatalog.EXPECTED_RECIPE_COUNT) {
            throw new LegacyRecipeImportException(
                    "Expected exactly " + LegacyRecipeCatalog.EXPECTED_RECIPE_COUNT
                            + " legacy recipes but found " + sourceRecipes.size());
        }

        Set<String> names = new HashSet<>();
        Set<String> slugs = new HashSet<>();

        List<PreparedLegacyRecipe> prepared = sourceRecipes.stream()
                .map(source -> prepareRecipe(source, assetsRoot, names, slugs))
                .toList();

        if (names.size() != LegacyRecipeCatalog.EXPECTED_RECIPE_COUNT) {
            throw new LegacyRecipeImportException("Legacy recipe names must be unique");
        }

        if (slugs.size() != LegacyRecipeCatalog.EXPECTED_RECIPE_COUNT) {
            throw new LegacyRecipeImportException("Legacy recipe slugs must be unique");
        }

        Set<String> expectedNames = LegacyRecipeCatalog.definitions().stream()
                .map(LegacyRecipeDefinition::name)
                .collect(java.util.stream.Collectors.toSet());

        if (!names.equals(expectedNames)) {
            throw new LegacyRecipeImportException(
                    "Legacy source recipes do not match the approved migration catalog");
        }

        return prepared;
    }

    private void validateSourcePaths(Path jsonPath, Path assetsRoot) {
        if (jsonPath == null || !Files.isRegularFile(jsonPath)) {
            throw new LegacyRecipeImportException("Legacy recipe JSON file does not exist");
        }

        if (assetsRoot == null || !Files.isDirectory(assetsRoot)) {
            throw new LegacyRecipeImportException("Legacy assets root does not exist");
        }
    }

    private List<LegacyRecipeSource> readSource(Path jsonPath) {
        try {
            return objectMapper.readValue(
                    jsonPath.toFile(),
                    new TypeReference<List<LegacyRecipeSource>>() {
                    });
        } catch (IOException exception) {
            throw new LegacyRecipeImportException("Legacy recipe JSON is invalid", exception);
        }
    }

    private PreparedLegacyRecipe prepareRecipe(
            LegacyRecipeSource source,
            Path assetsRoot,
            Set<String> names,
            Set<String> slugs) {

        requireText(source.name(), "name");

        if (!names.add(source.name())) {
            throw new LegacyRecipeImportException(
                    "Duplicate legacy recipe name: " + source.name());
        }

        LegacyRecipeDefinition definition = LegacyRecipeCatalog.definitionFor(source.name());

        if (definition == null) {
            throw new LegacyRecipeImportException(
                    "No approved migration definition for recipe: " + source.name());
        }

        if (definition.slug() == null || definition.slug().isBlank()) {
            throw new LegacyRecipeImportException(
                    "Approved slug is missing for recipe: " + source.name());
        }

        if (!slugs.add(definition.slug())) {
            throw new LegacyRecipeImportException(
                    "Duplicate approved slug: " + definition.slug());
        }

        requireText(source.cuisine(), "cuisine");
        requireText(source.category(), "category");
        requireText(source.country(), "country");
        requireText(source.difficulty(), "difficulty");
        requireText(source.image(), "image");

        if (source.ingredients() == null || source.ingredients().isEmpty()) {
            throw new LegacyRecipeImportException(
                    "Ingredients must not be empty for recipe: " + source.name());
        }

        if (source.steps() == null || source.steps().isEmpty()) {
            throw new LegacyRecipeImportException(
                    "Steps must not be empty for recipe: " + source.name());
        }

        validateListText(source.ingredients(), "ingredient", source.name());
        validateListText(source.steps(), "step", source.name());

        RecipeCategory category = mapCategory(source.category(), source.name());
        String countryCode = mapCountry(source.country(), source.name());
        RecipeDifficulty difficulty = mapDifficulty(source, definition);
        Integer time = mapTime(source.time(), source.name());
        ImageMetadata image = resolveImage(source.image(), assetsRoot, source.name());

        return new PreparedLegacyRecipe(
                definition.slug(),
                source.name(),
                source.cuisine(),
                category,
                countryCode,
                definition.type(),
                difficulty,
                time,
                List.copyOf(source.ingredients()),
                List.copyOf(source.steps()),
                image.path(),
                image.extension(),
                image.originalFilename(),
                image.mediaType());
    }

    private RecipeCategory mapCategory(String value, String recipeName) {
        return switch (value) {
            case "national" -> RecipeCategory.NATIONAL;
            case "international" -> RecipeCategory.INTERNATIONAL;
            default -> throw new LegacyRecipeImportException(
                    "Unknown category for recipe " + recipeName + ": " + value);
        };
    }

    private String mapCountry(String value, String recipeName) {
        String code = COUNTRY_CODES.get(value);

        if (code == null) {
            throw new LegacyRecipeImportException(
                    "Unknown country for recipe " + recipeName + ": " + value);
        }

        return code;
    }

    private RecipeDifficulty mapDifficulty(
            LegacyRecipeSource source,
            LegacyRecipeDefinition definition) {

        if (definition.difficultyOverride() != null) {
            return definition.difficultyOverride();
        }

        RecipeDifficulty difficulty = DIFFICULTIES.get(source.difficulty());

        if (difficulty == null) {
            throw new LegacyRecipeImportException(
                    "Unknown difficulty for recipe "
                            + source.name() + ": " + source.difficulty());
        }

        return difficulty;
    }

    private Integer mapTime(String value, String recipeName) {
        requireText(value, "time");

        if ("No especificado".equals(value)) {
            return null;
        }

        if (!value.matches("\\d+ minutos")) {
            throw new LegacyRecipeImportException(
                    "Invalid time for recipe " + recipeName + ": " + value);
        }

        try {
            return Integer.valueOf(value.substring(0, value.indexOf(' ')));
        } catch (NumberFormatException exception) {
            throw new LegacyRecipeImportException(
                    "Invalid time for recipe " + recipeName + ": " + value,
                    exception);
        }
    }

    private ImageMetadata resolveImage(
            String legacyImage,
            Path assetsRoot,
            String recipeName) {

        Path legacyPath;

        try {
            legacyPath = Path.of(legacyImage).normalize();
        } catch (RuntimeException exception) {
            throw new LegacyRecipeImportException(
                    "Invalid image path for recipe: " + recipeName,
                    exception);
        }

        if (legacyPath.isAbsolute()) {
            throw new LegacyRecipeImportException(
                    "Legacy image path must be relative for recipe: " + recipeName);
        }

        Path filename = legacyPath.getFileName();

        if (filename == null) {
            throw new LegacyRecipeImportException(
                    "Legacy image filename is missing for recipe: " + recipeName);
        }

        Path imagePath = assetsRoot.resolve(filename.toString()).normalize();

        if (!imagePath.startsWith(assetsRoot.toAbsolutePath().normalize())
                && assetsRoot.isAbsolute()) {
            throw new LegacyRecipeImportException(
                    "Legacy image escapes assets root for recipe: " + recipeName);
        }

        if (!Files.isRegularFile(imagePath)) {
            throw new LegacyRecipeImportException(
                    "Legacy image does not exist for recipe: " + recipeName);
        }

        String extension = extensionOf(filename.toString(), recipeName);
        String mediaType = switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> throw new LegacyRecipeImportException(
                    "Unsupported image extension for recipe: " + recipeName);
        };

        return new ImageMetadata(
                imagePath,
                extension,
                filename.toString(),
                mediaType);
    }

    private String extensionOf(String filename, String recipeName) {
        int separator = filename.lastIndexOf('.');

        if (separator < 0 || separator == filename.length() - 1) {
            throw new LegacyRecipeImportException(
                    "Image extension is missing for recipe: " + recipeName);
        }

        String extension = filename.substring(separator + 1)
                .toLowerCase(Locale.ROOT);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new LegacyRecipeImportException(
                    "Unsupported image extension for recipe: " + recipeName);
        }

        return extension;
    }

    private void validateListText(
            List<String> values,
            String label,
            String recipeName) {

        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);

            if (value == null || value.isBlank()) {
                throw new LegacyRecipeImportException(
                        "Blank " + label + " at position " + index
                                + " for recipe: " + recipeName);
            }
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new LegacyRecipeImportException(
                    "Legacy recipe field must not be blank: " + field);
        }
    }

    private record ImageMetadata(
            Path path,
            String extension,
            String originalFilename,
            String mediaType) {
    }
}
