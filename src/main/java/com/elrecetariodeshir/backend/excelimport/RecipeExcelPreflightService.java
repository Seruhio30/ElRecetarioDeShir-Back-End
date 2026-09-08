package com.elrecetariodeshir.backend.excelimport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.elrecetariodeshir.backend.recipe.RecipeCategory;
import com.elrecetariodeshir.backend.recipe.RecipeDifficulty;
import com.elrecetariodeshir.backend.recipe.RecipeSlugifier;
import com.elrecetariodeshir.backend.recipe.RecipeType;

public final class RecipeExcelPreflightService {

    private final ExcelRecipeWorkbookReader workbookReader =
            new ExcelRecipeWorkbookReader();

    public PreflightResult preflight(Path workbookPath) {
        var workbook = workbookReader.read(workbookPath);

        List<RecipeImportCandidate> candidates = new ArrayList<>();

        for (var sheet : workbook.sheets()) {
            if (sheet.empty()) {
                continue;
            }

            candidates.add(toCandidate(sheet));
        }

        applyDuplicateChecks(candidates);

        return new PreflightResult(
                workbook.workbook(),
                workbook.totalSheets(),
                workbook.ingredientRowCount(),
                candidates);
    }

    private RecipeImportCandidate toCandidate(
            ExcelRecipeSheetParser.ParsedSheet sheet) {

        List<String> warnings = new ArrayList<>(sheet.warnings());

        String sheetRecipeName = extractSheetRecipeName(sheet.sheet());

        boolean nameConflict = sheet.originalName() != null
                && sheetRecipeName != null
                && !normalizeName(sheet.originalName())
                        .equals(normalizeName(sheetRecipeName));

        if (nameConflict) {
            warnings.add(
                    "Sheet recipe name does not match internal recipe name: "
                            + sheetRecipeName
                            + " != "
                            + sheet.originalName());
        }

        String candidateName = nameConflict
                ? sheetRecipeName
                : sheet.normalizedName();

        String slugCandidate = RecipeSlugifier.slugify(candidateName);

        var yieldResult = RecipeYieldNormalizer.normalize(sheet.yieldRaw());
        warnings.addAll(yieldResult.warnings());

        var approved = ExcelRecipeApprovedMetadata.find(sheet.originalName());

        String countryCode = approved == null
                ? null
                : approved.countryCode();

        RecipeType type = approved == null
                ? null
                : approved.type();

        RecipeDifficulty difficulty = mapDifficulty(sheet.difficultyRaw());

        if (difficulty == null && approved != null) {
            difficulty = approved.difficulty();
        }

        Integer time = parseTime(sheet.timeRaw());

        if (time == null && approved != null) {
            time = approved.timeMinutes();
        }

        if (!sheet.imageExists()) {
            warnings.add("Recipe image is missing.");
        }

        RecipeCategory category = null;

        if (category == null) {
            warnings.add("Recipe category requires review.");
        }

        if (countryCode == null) {
            warnings.add("Recipe countryCode requires review.");
        }

        if (type == null) {
            warnings.add("Recipe type requires review.");
        }

        if (difficulty == null) {
            warnings.add("Recipe difficulty requires review.");
        }

        RecipePreflightStatus status = classify(
                sheet,
                yieldResult,
                warnings);

        return new RecipeImportCandidate(
                sheet.workbook(),
                sheet.sheet(),
                sheet.recipeNumber(),
                sheet.originalName(),
                candidateName,
                slugCandidate,
                null,
                category,
                countryCode,
                type,
                difficulty,
                time,
                yieldResult.yield(),
                sheet.ingredients(),
                sheet.steps(),
                sheet.imageReference(),
                sheet.imageExists(),
                List.copyOf(warnings),
                status);
    }

    private RecipePreflightStatus classify(
            ExcelRecipeSheetParser.ParsedSheet sheet,
            RecipeYieldNormalizer.Result yieldResult,
            List<String> warnings) {

        if (sheet.originalName() == null
                || sheet.originalName().isBlank()
                || sheet.slugCandidate() == null
                || sheet.slugCandidate().isBlank()
                || sheet.ingredients().isEmpty()
                || sheet.steps().isEmpty()) {
            return RecipePreflightStatus.INVALID;
        }

        if (!yieldResult.deterministic() || !warnings.isEmpty()) {
            return RecipePreflightStatus.REVIEW_REQUIRED;
        }

        return RecipePreflightStatus.READY;
    }

    private void applyDuplicateChecks(
            List<RecipeImportCandidate> candidates) {

        Map<String, List<Integer>> slugIndexes = new HashMap<>();
        Map<String, List<Integer>> nameIndexes = new HashMap<>();
        Map<String, List<Integer>> sheetIndexes = new HashMap<>();

        for (int index = 0; index < candidates.size(); index++) {
            RecipeImportCandidate candidate = candidates.get(index);

            slugIndexes.computeIfAbsent(
                    candidate.slugCandidate(),
                    ignored -> new ArrayList<>()).add(index);

            nameIndexes.computeIfAbsent(
                    normalizeName(candidate.normalizedName()),
                    ignored -> new ArrayList<>()).add(index);

            sheetIndexes.computeIfAbsent(
                    normalizeName(candidate.sourceSheet()),
                    ignored -> new ArrayList<>()).add(index);
        }

        Set<Integer> conflicts = new HashSet<>();

        collectDuplicateIndexes(slugIndexes, conflicts);
        collectDuplicateIndexes(nameIndexes, conflicts);
        collectDuplicateIndexes(sheetIndexes, conflicts);

        for (Integer index : conflicts) {
            RecipeImportCandidate candidate = candidates.get(index);
            List<String> warnings = new ArrayList<>(candidate.warnings());

            if (slugIndexes.get(candidate.slugCandidate()).size() > 1) {
                warnings.add(
                        "Duplicate slug candidate: "
                                + candidate.slugCandidate());
            }

            if (nameIndexes.get(
                    normalizeName(candidate.normalizedName())).size() > 1) {
                warnings.add(
                        "Duplicate normalized recipe name: "
                                + candidate.normalizedName());
            }

            if (sheetIndexes.get(
                    normalizeName(candidate.sourceSheet())).size() > 1) {
                warnings.add(
                        "Duplicate sheet name: "
                                + candidate.sourceSheet());
            }

            candidates.set(
                    index,
                    copyWithWarningsAndStatus(
                            candidate,
                            warnings,
                            RecipePreflightStatus.REVIEW_REQUIRED));
        }
    }

    private void collectDuplicateIndexes(
            Map<String, List<Integer>> indexes,
            Set<Integer> conflicts) {

        indexes.values().stream()
                .filter(values -> values.size() > 1)
                .forEach(conflicts::addAll);
    }

    private RecipeImportCandidate copyWithWarningsAndStatus(
            RecipeImportCandidate candidate,
            List<String> warnings,
            RecipePreflightStatus status) {

        return new RecipeImportCandidate(
                candidate.sourceWorkbook(),
                candidate.sourceSheet(),
                candidate.sourceRecipeNumber(),
                candidate.originalName(),
                candidate.normalizedName(),
                candidate.slugCandidate(),
                candidate.cuisine(),
                candidate.category(),
                candidate.countryCode(),
                candidate.type(),
                candidate.difficulty(),
                candidate.timeMinutes(),
                candidate.yield(),
                candidate.ingredients(),
                candidate.steps(),
                candidate.imageReference(),
                candidate.imageExists(),
                List.copyOf(warnings),
                status);
    }

    private RecipeDifficulty mapDifficulty(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "fácil", "facil" -> RecipeDifficulty.EASY;
            case "media" -> RecipeDifficulty.MEDIUM;
            case "alta" -> RecipeDifficulty.HARD;
            default -> null;
        };
    }

    private Integer parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);

        if (normalized.matches("\\d+\\s+minutos?")) {
            return Integer.valueOf(normalized.split("\\s+")[0]);
        }

        if (normalized.matches("\\d+\\s+horas?")) {
            return Integer.valueOf(normalized.split("\\s+")[0]) * 60;
        }

        return null;
    }

    private String extractSheetRecipeName(String sheetName) {
        if (sheetName == null) {
            return null;
        }

        return sheetName.replaceFirst("^\\s*\\d+\\s*\\.\\s*", "").trim();
    }

    private String normalizeName(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    public record PreflightResult(
            String workbook,
            int totalSheets,
            int ingredientRowsDetected,
            List<RecipeImportCandidate> candidates) {

        public PreflightResult {
            candidates = List.copyOf(candidates);
        }

        public long count(RecipePreflightStatus status) {
            return candidates.stream()
                    .filter(candidate -> candidate.status() == status)
                    .count();
        }

        public int ingredientCount() {
            return candidates.stream()
                    .mapToInt(candidate -> candidate.ingredients().size())
                    .sum();
        }

        public int stepCount() {
            return candidates.stream()
                    .mapToInt(candidate -> candidate.steps().size())
                    .sum();
        }

        public long uniqueSlugCount() {
            return candidates.stream()
                    .map(RecipeImportCandidate::slugCandidate)
                    .distinct()
                    .count();
        }
    }
}
