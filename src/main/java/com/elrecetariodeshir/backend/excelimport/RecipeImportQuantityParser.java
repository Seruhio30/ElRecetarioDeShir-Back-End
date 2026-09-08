package com.elrecetariodeshir.backend.excelimport;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RecipeImportQuantityParser {

    private static final Pattern SIMPLE_NUMBER = Pattern.compile(
            "^([+-]?\\d+(?:[.,]\\d+)?)$");

    private static final Pattern RANGE = Pattern.compile(
            "^([+-]?\\d+(?:[.,]\\d+)?)\\s*(?:-|a)\\s*([+-]?\\d+(?:[.,]\\d+)?)$",
            Pattern.CASE_INSENSITIVE);

    private RecipeImportQuantityParser() {
    }

    public static ParsedQuantity parse(Object rawValue) {
        if (rawValue == null) {
            return new ParsedQuantity(null, null, false, false);
        }

        if (rawValue instanceof Number number) {
            return new ParsedQuantity(
                    toBigDecimal(number),
                    null,
                    true,
                    false);
        }

        String text = rawValue.toString().trim();

        if (text.isEmpty()) {
            return new ParsedQuantity(null, null, false, false);
        }

        Matcher rangeMatcher = RANGE.matcher(text);

        if (rangeMatcher.matches()) {
            BigDecimal min = decimal(rangeMatcher.group(1));
            BigDecimal max = decimal(rangeMatcher.group(2));

            return new ParsedQuantity(min, max, true, true);
        }

        Matcher numberMatcher = SIMPLE_NUMBER.matcher(text);

        if (numberMatcher.matches()) {
            return new ParsedQuantity(
                    decimal(numberMatcher.group(1)),
                    null,
                    true,
                    false);
        }

        return new ParsedQuantity(null, null, false, false);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    private static BigDecimal toBigDecimal(Number value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        return new BigDecimal(value.toString());
    }

    public record ParsedQuantity(
            BigDecimal quantity,
            BigDecimal quantityMax,
            boolean parsed,
            boolean range) {
    }
}
