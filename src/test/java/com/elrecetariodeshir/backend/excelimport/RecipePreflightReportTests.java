package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RecipePreflightReportTests {

    private static final Path WORKBOOK =
            ExcelRecipeTestWorkbook.canonicalWorkbook();

    private static final Path OUTPUT = Path.of(
            "target/recipe-preflight.json");

    @Test
    void generatesInspectableDeterministicReport() throws Exception {
        var preflight = new RecipeExcelPreflightService()
                .preflight(WORKBOOK);

        var report = RecipePreflightReportFactory.from(preflight);

        new RecipePreflightJsonWriter().write(report, OUTPUT);

        assertThat(Files.isRegularFile(OUTPUT)).isTrue();

        System.out.println("recipesDetected=" + report.recipesDetected());
        System.out.println("ready=" + report.ready());
        System.out.println("reviewRequired=" + report.reviewRequired());
        System.out.println("invalid=" + report.invalid());
        System.out.println("structureReady=" + report.structureReady());
        System.out.println("structureReviewRequired="
                + report.structureReviewRequired());
        System.out.println("ingredientRowsDetected="
                + report.ingredientRowsDetected());
        System.out.println("normalizedIngredients="
                + report.normalizedIngredients());
        System.out.println("totalSteps=" + report.totalSteps());
        System.out.println("uniqueSlugs=" + report.uniqueSlugs());
        System.out.println("simpleYields=" + report.simpleYields());
        System.out.println("rangeYields=" + report.rangeYields());
        System.out.println("unitYields=" + report.unitYields());
        System.out.println("otherYields=" + report.otherYields());
        System.out.println("unresolvedYields=" + report.unresolvedYields());
        System.out.println("unknownIngredientUnits="
                + report.unknownIngredientUnits());
        System.out.println("zeroIngredientQuantities="
                + report.zeroIngredientQuantities());
        System.out.println("missingIngredientQuantities="
                + report.missingIngredientQuantities());
        System.out.println("missingIngredientUnits="
                + report.missingIngredientUnits());
        System.out.println("missingImages=" + report.missingImages());
        System.out.println("output=" + OUTPUT.toAbsolutePath());
    }
}
