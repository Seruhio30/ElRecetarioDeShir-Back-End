package com.elrecetariodeshir.backend.excelimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelRecipeWorkbookReader {

    private final ExcelRecipeSheetParser sheetParser =
            new ExcelRecipeSheetParser();

    public WorkbookResult read(Path workbookPath) {
        if (workbookPath == null || !Files.isRegularFile(workbookPath)) {
            throw new IllegalArgumentException(
                    "Recipe workbook does not exist: " + workbookPath);
        }

        try (InputStream input = Files.newInputStream(workbookPath);
                Workbook workbook = new XSSFWorkbook(input)) {

            List<ExcelRecipeSheetParser.ParsedSheet> sheets =
                    new ArrayList<>();

            for (int index = 0;
                    index < workbook.getNumberOfSheets();
                    index++) {

                sheets.add(sheetParser.parse(
                        workbookPath.getFileName().toString(),
                        workbook.getSheetAt(index)));
            }

            return new WorkbookResult(
                    workbookPath.getFileName().toString(),
                    workbook.getNumberOfSheets(),
                    List.copyOf(sheets));

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read recipe workbook: " + workbookPath,
                    exception);
        }
    }

    public record WorkbookResult(
            String workbook,
            int totalSheets,
            List<ExcelRecipeSheetParser.ParsedSheet> sheets) {

        public long recipeCount() {
            return sheets.stream()
                    .filter(sheet -> !sheet.empty())
                    .count();
        }

        public long emptySheetCount() {
            return sheets.stream()
                    .filter(ExcelRecipeSheetParser.ParsedSheet::empty)
                    .count();
        }

        public int ingredientCount() {
            return sheets.stream()
                    .mapToInt(sheet -> sheet.ingredients().size())
                    .sum();
        }

        public int ingredientRowCount() {
            return sheets.stream()
                    .mapToInt(ExcelRecipeSheetParser.ParsedSheet::ingredientRowsDetected)
                    .sum();
        }

        public int stepCount() {
            return sheets.stream()
                    .mapToInt(sheet -> sheet.steps().size())
                    .sum();
        }
    }
}
