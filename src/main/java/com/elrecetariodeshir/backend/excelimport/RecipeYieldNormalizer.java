package com.elrecetariodeshir.backend.excelimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.elrecetariodeshir.backend.recipe.RecipeYieldUnit;

public final class RecipeYieldNormalizer {

    private static final Pattern LABEL = Pattern.compile(
            "^\\s*Numero de Porciones\\s*:\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SIMPLE = Pattern.compile(
            "^([0-9]+(?:[.,][0-9]+)?)$");

    private static final Pattern RANGE = Pattern.compile(
            "^([0-9]+(?:[.,][0-9]+)?)\\s*-\\s*([0-9]+(?:[.,][0-9]+)?)$");

    private static final Pattern RANGE_WITH_UNIT = Pattern.compile(
            "^([0-9]+(?:[.,][0-9]+)?)\\s*-\\s*([0-9]+(?:[.,][0-9]+)?)\\s+(.+)$");

    private static final Pattern VALUE_WITH_UNIT = Pattern.compile(
            "^([0-9]+(?:[.,][0-9]+)?)\\s+(.+)$");

    private static final Pattern VALUE_WITH_DESCRIPTION = Pattern.compile(
            "^([0-9]+(?:[.,][0-9]+)?)\\s*(\\(.+\\))$");

    private RecipeYieldNormalizer() {
    }

    public static Result normalize(String rawValue) {
        List<String> warnings = new ArrayList<>();

        if (rawValue == null || rawValue.isBlank()) {
            warnings.add("Yield value is missing.");
            return new Result(
                    new RecipeImportYield(null, null, null, null, rawValue),
                    warnings,
                    false);
        }

        String display = rawValue.trim();
        String value = extractValue(display);

        if (value.isBlank()) {
            warnings.add("Yield value is missing.");
            return new Result(
                    new RecipeImportYield(null, null, null, null, display),
                    warnings,
                    false);
        }

        Matcher range = RANGE.matcher(value);
        if (range.matches()) {
            BigDecimal min = decimal(range.group(1));
            BigDecimal max = decimal(range.group(2));

            if (max.compareTo(min) < 0) {
                warnings.add("Yield range maximum is lower than minimum.");
                return new Result(
                        new RecipeImportYield(null, min, max, null, display),
                        warnings,
                        false);
            }

            return new Result(
                    new RecipeImportYield(null, min, max, null, display),
                    warnings,
                    true);
        }

        Matcher rangeWithUnit = RANGE_WITH_UNIT.matcher(value);
        if (rangeWithUnit.matches()) {
            BigDecimal min = decimal(rangeWithUnit.group(1));
            BigDecimal max = decimal(rangeWithUnit.group(2));
            String rawUnit = rangeWithUnit.group(3).trim();
            RecipeYieldUnit unit = RecipeImportUnitNormalizer
                    .normalizeYieldUnit(rawUnit);

            if (max.compareTo(min) < 0) {
                warnings.add("Yield range maximum is lower than minimum.");
                return new Result(
                        new RecipeImportYield(null, min, max, unit, display),
                        warnings,
                        false);
            }

            if (unit == RecipeYieldUnit.OTHER) {
                warnings.add("Unknown yield unit: " + rawUnit);
                return new Result(
                        new RecipeImportYield(
                                null,
                                min,
                                max,
                                RecipeYieldUnit.OTHER,
                                display),
                        warnings,
                        false);
            }

            return new Result(
                    new RecipeImportYield(null, min, max, unit, display),
                    warnings,
                    true);
        }

        Matcher described = VALUE_WITH_DESCRIPTION.matcher(value);
        if (described.matches()) {
            BigDecimal quantity = decimal(described.group(1));

            return new Result(
                    new RecipeImportYield(
                            quantity,
                            null,
                            null,
                            RecipeYieldUnit.OTHER,
                            display),
                    warnings,
                    true);
        }

        Matcher withUnit = VALUE_WITH_UNIT.matcher(value);
        if (withUnit.matches()) {
            BigDecimal quantity = decimal(withUnit.group(1));
            String rawUnit = withUnit.group(2).trim();
            RecipeYieldUnit unit = RecipeImportUnitNormalizer
                    .normalizeYieldUnit(rawUnit);

            if (unit == RecipeYieldUnit.OTHER) {
                warnings.add("Unknown yield unit: " + rawUnit);
                return new Result(
                        new RecipeImportYield(
                                quantity,
                                null,
                                null,
                                RecipeYieldUnit.OTHER,
                                display),
                        warnings,
                        false);
            }

            return new Result(
                    new RecipeImportYield(
                            quantity,
                            null,
                            null,
                            unit,
                            display),
                    warnings,
                    true);
        }

        Matcher simple = SIMPLE.matcher(value);
        if (simple.matches()) {
            return new Result(
                    new RecipeImportYield(
                            decimal(simple.group(1)),
                            null,
                            null,
                            null,
                            display),
                    warnings,
                    true);
        }

        warnings.add("Yield value could not be parsed: " + value);

        return new Result(
                new RecipeImportYield(null, null, null, null, display),
                warnings,
                false);
    }

    private static String extractValue(String rawValue) {
        Matcher matcher = LABEL.matcher(rawValue);
        return matcher.matches()
                ? matcher.group(1).trim()
                : rawValue.trim();
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    public record Result(
            RecipeImportYield yield,
            List<String> warnings,
            boolean deterministic) {

        public Result {
            warnings = List.copyOf(warnings);
        }
    }
}
