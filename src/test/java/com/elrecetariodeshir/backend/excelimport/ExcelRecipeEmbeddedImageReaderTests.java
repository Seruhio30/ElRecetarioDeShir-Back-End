package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ExcelRecipeEmbeddedImageReaderTests {

    private static final Path WORKBOOK =
            ExcelRecipeTestWorkbook.canonicalWorkbook();

    private final ExcelRecipeEmbeddedImageReader reader =
            new ExcelRecipeEmbeddedImageReader();

    @Test
    void readsKnownEmbeddedRecipeImage() {
        var preflight = new RecipeExcelPreflightService()
                .preflight(WORKBOOK);

        var candidate = preflight.candidates().stream()
                .filter(recipe -> Boolean.TRUE.equals(recipe.imageExists()))
                .findFirst()
                .orElseThrow();

        var image = reader.read(
                WORKBOOK,
                candidate.sourceSheet(),
                candidate.imageReference());

        assertThat(image).isNotNull();
        assertThat(image.bytes()).isNotEmpty();
        assertThat(image.reference())
                .isEqualTo(candidate.imageReference());
        assertThat(image.extension())
                .isIn("jpg", "jpeg", "png");
        assertThat(image.mediaType())
                .isIn("image/jpeg", "image/png");
    }

    @Test
    void returnsNullForRecipeWithoutImage() {
        var image = reader.read(
                WORKBOOK,
                "23. Tres Leches de Coco",
                null);

        assertThat(image).isNull();
    }
}
