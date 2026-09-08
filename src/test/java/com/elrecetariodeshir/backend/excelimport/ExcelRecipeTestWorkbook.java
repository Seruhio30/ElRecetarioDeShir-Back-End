package com.elrecetariodeshir.backend.excelimport;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

final class ExcelRecipeTestWorkbook {

    private ExcelRecipeTestWorkbook() {
    }

    static Path canonicalWorkbook() {
        String configuredPath = System.getenv("RECIPE_EXCEL_PATH");

        assumeTrue(
                configuredPath != null && !configuredPath.isBlank(),
                "RECIPE_EXCEL_PATH is not configured");

        Path workbook = Path.of(configuredPath);

        assumeTrue(
                Files.isRegularFile(workbook),
                "Canonical recipe workbook does not exist: " + workbook);

        return workbook;
    }
}
