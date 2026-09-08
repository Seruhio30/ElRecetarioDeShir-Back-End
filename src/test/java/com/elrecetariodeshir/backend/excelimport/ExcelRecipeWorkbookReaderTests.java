package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ExcelRecipeWorkbookReaderTests {

    private static final Path WORKBOOK =
            ExcelRecipeTestWorkbook.canonicalWorkbook();

    @Test
    void readsCanonicalWorkbookAndDetectsRecipes() {
        var result = new ExcelRecipeWorkbookReader().read(WORKBOOK);

        assertThat(result.totalSheets()).isEqualTo(63);
        assertThat(result.recipeCount()).isEqualTo(62);
        assertThat(result.emptySheetCount()).isEqualTo(1);

        var empty = result.sheets().stream()
                .filter(ExcelRecipeSheetParser.ParsedSheet::empty)
                .toList();

        assertThat(empty)
                .extracting(ExcelRecipeSheetParser.ParsedSheet::sheet)
                .containsExactly("Hoja11");
    }

    @Test
    void parsesKnownRecipeMetadata() {
        var result = new ExcelRecipeWorkbookReader().read(WORKBOOK);

        var pastaChoux = result.sheets().stream()
                .filter(sheet -> "2. Pasta choux".equals(sheet.sheet()))
                .findFirst()
                .orElseThrow();

        assertThat(pastaChoux.recipeNumber()).isEqualTo(2);
        assertThat(pastaChoux.originalName()).isEqualTo("Pasta choux");
        assertThat(pastaChoux.slugCandidate())
                .isEqualTo("pasta-choux");
        assertThat(pastaChoux.yieldRaw())
                .contains("18 u");
        assertThat(pastaChoux.ingredients())
                .hasSize(5);
        assertThat(pastaChoux.steps())
                .isNotEmpty();
    }

    @Test
    void detectsKnownHuevosBenedictinosNameConflict() {
        var result = new ExcelRecipeWorkbookReader().read(WORKBOOK);

        var sheet = result.sheets().stream()
                .filter(candidate ->
                        "35. Huevos Benedictinos".equals(candidate.sheet()))
                .findFirst()
                .orElseThrow();

        assertThat(sheet.originalName())
                .isEqualTo("Salsa Bechamel");
        assertThat(sheet.sheet())
                .contains("Huevos Benedictinos");
    }
}
