package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class RecipeDuplicateSlugPreflightTests {

    @Test
    void duplicateSlugPreventsReady() throws Exception {
        Path workbookPath = Path.of(
                "target/duplicate-slug-preflight.xlsx");

        try (var workbook = new XSSFWorkbook()) {
            createRecipeSheet(workbook, "1. Café", 1, "Café");
            createRecipeSheet(workbook, "2. Cafe", 2, "Cafe");

            try (var output = Files.newOutputStream(workbookPath)) {
                workbook.write(output);
            }
        }

        var result = new RecipeExcelPreflightService()
                .preflight(workbookPath);

        assertThat(result.candidates()).hasSize(2);
        assertThat(result.uniqueSlugCount()).isEqualTo(1);

        assertThat(result.candidates())
                .allSatisfy(candidate -> {
                    assertThat(candidate.status())
                            .isEqualTo(
                                    RecipePreflightStatus.REVIEW_REQUIRED);
                    assertThat(candidate.warnings())
                            .contains("Duplicate slug candidate: cafe");
                });
    }

    private void createRecipeSheet(
            XSSFWorkbook workbook,
            String sheetName,
            int number,
            String recipeName) {

        var sheet = workbook.createSheet(sheetName);

        sheet.createRow(0)
                .createCell(2)
                .setCellValue("Numero de receta: " + number);

        sheet.createRow(1)
                .createCell(1)
                .setCellValue("Nombre de la Receta: " + recipeName);

        sheet.createRow(2)
                .createCell(2)
                .setCellValue("Numero de Porciones: 1");

        var ingredientHeader = sheet.createRow(3);
        ingredientHeader.createCell(1).setCellValue("INGREDIENTES");
        ingredientHeader.createCell(2).setCellValue("CANTIDAD");
        ingredientHeader.createCell(3).setCellValue("Unidad Medida");

        var ingredient = sheet.createRow(4);
        ingredient.createCell(1).setCellValue("Harina");
        ingredient.createCell(2).setCellValue(100);
        ingredient.createCell(3).setCellValue("gr");

        sheet.createRow(5)
                .createCell(1)
                .setCellValue("PREPARACION:");

        sheet.createRow(6)
                .createCell(1)
                .setCellValue("1. Mezclar.");
    }
}
