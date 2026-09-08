package com.elrecetariodeshir.backend.excelimport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.elrecetariodeshir.backend.recipe.RecipeSlugifier;

public final class ExcelRecipeSheetParser {

    private static final Pattern RECIPE_NUMBER = Pattern.compile(
            "Numero de receta\\s*:\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RECIPE_NAME = Pattern.compile(
            "Nombre de la Receta\\s*:\\s*(.+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DIFFICULTY = Pattern.compile(
            "Dificultad\\s*:\\s*(.*)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TIME = Pattern.compile(
            "Tiempo\\s*:\\s*(.*)",
            Pattern.CASE_INSENSITIVE);

    private final DataFormatter formatter = new DataFormatter(Locale.ROOT);
    private FormulaEvaluator formulaEvaluator;

    public ParsedSheet parse(String workbookName, Sheet sheet) {
        this.formulaEvaluator = sheet.getWorkbook()
                .getCreationHelper()
                .createFormulaEvaluator();
        if (isEmpty(sheet)) {
            return ParsedSheet.empty(workbookName, sheet.getSheetName());
        }

        int ingredientHeaderRow = findIngredientHeaderRow(sheet);
        int preparationRow = findPreparationRow(sheet);

        List<String> warnings = new ArrayList<>();

        if (ingredientHeaderRow < 0) {
            warnings.add("Ingredient header was not found.");
        }

        if (preparationRow < 0) {
            warnings.add("Preparation section was not found.");
        }

        Integer recipeNumber = null;
        String originalName = null;
        String yieldRaw = null;
        String difficultyRaw = null;
        String timeRaw = null;

        for (Row row : sheet) {
            for (Cell cell : row) {
                String text = cellText(cell);

                if (text.isBlank()) {
                    continue;
                }

                if (recipeNumber == null) {
                    Matcher matcher = RECIPE_NUMBER.matcher(text);
                    if (matcher.find()) {
                        recipeNumber = Integer.valueOf(matcher.group(1));
                    }
                }

                if (originalName == null) {
                    Matcher matcher = RECIPE_NAME.matcher(text);
                    if (matcher.find()) {
                        originalName = matcher.group(1).trim();
                    }
                }

                if (yieldRaw == null
                        && text.toLowerCase(Locale.ROOT)
                                .startsWith("numero de porciones")) {
                    yieldRaw = text;
                }

                if (difficultyRaw == null) {
                    Matcher matcher = DIFFICULTY.matcher(text);
                    if (matcher.matches()) {
                        difficultyRaw = matcher.group(1).trim();
                    }
                }

                if (timeRaw == null) {
                    Matcher matcher = TIME.matcher(text);
                    if (matcher.matches()) {
                        timeRaw = matcher.group(1).trim();
                    }
                }
            }
        }

        String normalizedName = originalName == null
                ? null
                : originalName.trim();

        String slugCandidate = normalizedName == null
                ? ""
                : RecipeSlugifier.slugify(normalizedName);

        int ingredientRowsDetected = countIngredientRows(
                sheet,
                ingredientHeaderRow,
                preparationRow);

        List<RecipeImportIngredient> ingredients =
                parseIngredients(sheet, ingredientHeaderRow, preparationRow, warnings);

        List<RecipeImportStep> steps =
                parseSteps(sheet, preparationRow, warnings);

        ImageReference imageReference = findRecipeImage(sheet);

        return new ParsedSheet(
                workbookName,
                sheet.getSheetName(),
                false,
                recipeNumber,
                originalName,
                normalizedName,
                slugCandidate,
                yieldRaw,
                difficultyRaw,
                timeRaw,
                ingredientRowsDetected,
                ingredients,
                steps,
                imageReference.reference(),
                imageReference.exists(),
                List.copyOf(warnings));
    }

    private int countIngredientRows(
            Sheet sheet,
            int ingredientHeaderRow,
            int preparationRow) {

        if (ingredientHeaderRow < 0) {
            return 0;
        }

        int count = 0;
        int endRow = preparationRow > ingredientHeaderRow
                ? preparationRow
                : sheet.getLastRowNum() + 1;

        for (int rowIndex = ingredientHeaderRow + 1;
                rowIndex < endRow;
                rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            String ingredientName = cellText(row.getCell(1));

            if (!ingredientName.isBlank()) {
                count++;
            }
        }

        return count;
    }

    private List<RecipeImportIngredient> parseIngredients(
            Sheet sheet,
            int ingredientHeaderRow,
            int preparationRow,
            List<String> warnings) {

        if (ingredientHeaderRow < 0) {
            return List.of();
        }

        List<RecipeImportIngredient> ingredients = new ArrayList<>();
        int endRow = preparationRow > ingredientHeaderRow
                ? preparationRow
                : sheet.getLastRowNum() + 1;

        for (int rowIndex = ingredientHeaderRow + 1;
                rowIndex < endRow;
                rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            String ingredientName = cellText(row.getCell(1));

            if (ingredientName.isBlank()) {
                continue;
            }

            Object rawQuantity = rawCellValue(row.getCell(2));
            String rawUnit = cellText(row.getCell(3));

            if (isIngredientSectionHeader(
                    ingredientName,
                    rawQuantity,
                    rawUnit)) {
                continue;
            }

            var result = RecipeIngredientNormalizer.normalize(
                    ingredientName,
                    rawQuantity,
                    rawUnit,
                    ingredients.size());

            ingredients.add(result.ingredient());

            for (String warning : result.warnings()) {
                warnings.add(
                        "Ingredient " + ingredientName + ": " + warning);
            }
        }

        return List.copyOf(ingredients);
    }

    private boolean isIngredientSectionHeader(
            String ingredientName,
            Object rawQuantity,
            String rawUnit) {

        return rawQuantity == null
                && (rawUnit == null || rawUnit.isBlank())
                && ingredientName.trim().endsWith(":");
    }

    private ImageReference findRecipeImage(Sheet sheet) {
        for (var picture : sheet.getDrawingPatriarch() == null
                ? List.<org.apache.poi.ss.usermodel.Shape>of()
                : sheet.getDrawingPatriarch()) {

            if (!(picture instanceof org.apache.poi.xssf.usermodel.XSSFPicture image)) {
                continue;
            }

            var data = image.getPictureData();
            String extension = data.suggestFileExtension();

            var anchor = image.getClientAnchor();

            boolean isLogo = "png".equalsIgnoreCase(extension)
                    && image.getImageDimension().width == 153
                    && image.getImageDimension().height == 148
                    && anchor.getRow1() < 5;

            if (isLogo) {
                continue;
            }

            String reference = "embedded-image@"
                    + (anchor.getRow1() + 1)
                    + ":"
                    + (anchor.getCol1() + 1)
                    + "."
                    + extension;

            return new ImageReference(reference, true);
        }

        return new ImageReference(null, false);
    }

    private List<RecipeImportStep> parseSteps(
            Sheet sheet,
            int preparationRow,
            List<String> warnings) {

        if (preparationRow < 0) {
            return List.of();
        }

        List<RecipeImportStep> steps = new ArrayList<>();

        for (int rowIndex = preparationRow + 1;
                rowIndex <= sheet.getLastRowNum();
                rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            String text = cellText(row.getCell(1));

            if (text.isBlank()) {
                continue;
            }

            if (text.toLowerCase(Locale.ROOT).startsWith("chef tips")) {
                break;
            }

            steps.add(new RecipeImportStep(
                    steps.size(),
                    text.trim()));
        }

        if (steps.isEmpty()) {
            warnings.add("Recipe has no preparation steps.");
        }

        return List.copyOf(steps);
    }

    private int findIngredientHeaderRow(Sheet sheet) {
        for (Row row : sheet) {
            boolean hasIngredient = false;
            boolean hasQuantity = false;

            for (Cell cell : row) {
                String text = cellText(cell)
                        .trim()
                        .toLowerCase(Locale.ROOT);

                if (text.contains("ingrediente")) {
                    hasIngredient = true;
                }

                if ("cantidad".equals(text)) {
                    hasQuantity = true;
                }
            }

            if (hasIngredient && hasQuantity) {
                return row.getRowNum();
            }
        }

        return -1;
    }

    private int findPreparationRow(Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                String text = cellText(cell)
                        .trim()
                        .toLowerCase(Locale.ROOT);

                if (text.contains("preparaci")) {
                    return row.getRowNum();
                }
            }
        }

        return -1;
    }

    private boolean isEmpty(Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (!cellText(cell).isBlank()) {
                    return false;
                }
            }
        }

        return true;
    }

    private String cellText(Cell cell) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private Object rawCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> cell.getStringCellValue();
            case FORMULA -> {
                var evaluated = formulaEvaluator.evaluate(cell);

                if (evaluated == null) {
                    yield formatter.formatCellValue(cell);
                }

                yield switch (evaluated.getCellType()) {
                    case NUMERIC -> evaluated.getNumberValue();
                    case STRING -> evaluated.getStringValue();
                    case BOOLEAN -> evaluated.getBooleanValue();
                    case BLANK, ERROR, FORMULA, _NONE ->
                            formatter.formatCellValue(cell);
                };
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case BLANK, ERROR, _NONE -> null;
        };
    }

    private record ImageReference(
            String reference,
            boolean exists) {
    }

    public record ParsedSheet(
            String workbook,
            String sheet,
            boolean empty,
            Integer recipeNumber,
            String originalName,
            String normalizedName,
            String slugCandidate,
            String yieldRaw,
            String difficultyRaw,
            String timeRaw,
            int ingredientRowsDetected,
            List<RecipeImportIngredient> ingredients,
            List<RecipeImportStep> steps,
            String imageReference,
            boolean imageExists,
            List<String> warnings) {

        public static ParsedSheet empty(
                String workbook,
                String sheet) {

            return new ParsedSheet(
                    workbook,
                    sheet,
                    true,
                    null,
                    null,
                    null,
                    "",
                    null,
                    null,
                    null,
                    0,
                    List.of(),
                    List.of(),
                    null,
                    false,
                    List.of());
        }
    }
}
