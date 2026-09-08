package com.elrecetariodeshir.backend.excelimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.elrecetariodeshir.backend.recipe.RecipeRepository;

class RecipeExcelPreflightAcceptanceTests {

    private static final Path WORKBOOK =
            ExcelRecipeTestWorkbook.canonicalWorkbook();

    @Test
    void detectsCanonicalWorkbookCounts() {
        var result = new RecipeExcelPreflightService()
                .preflight(WORKBOOK);

        assertThat(result.totalSheets()).isEqualTo(63);
        assertThat(result.candidates()).hasSize(62);
        assertThat(result.ingredientRowsDetected()).isEqualTo(451);
        assertThat(result.ingredientCount()).isEqualTo(450);
        assertThat(result.stepCount()).isEqualTo(518);
        assertThat(result.uniqueSlugCount()).isEqualTo(62);
    }

    @Test
    void preservesSourceMetadata() {
        var candidate = candidate("2. Pasta choux");

        assertThat(candidate.sourceWorkbook())
                .isEqualTo("Recetas.xlsx");
        assertThat(candidate.sourceSheet())
                .isEqualTo("2. Pasta choux");
        assertThat(candidate.sourceRecipeNumber()).isEqualTo(2);
        assertThat(candidate.originalName())
                .isEqualTo("Pasta choux");
        assertThat(candidate.normalizedName())
                .isEqualTo("Pasta choux");
        assertThat(candidate.slugCandidate())
                .isEqualTo("pasta-choux");
    }

    @Test
    void preservesHuevosBenedictinosSheetIdentityAndFlagsConflict() {
        var candidate = candidate("35. Huevos Benedictinos");

        assertThat(candidate.normalizedName())
                .isEqualTo("Huevos Benedictinos");
        assertThat(candidate.originalName())
                .isEqualTo("Salsa Bechamel");
        assertThat(candidate.slugCandidate())
                .isEqualTo("huevos-benedictinos");
        assertThat(candidate.status())
                .isEqualTo(RecipePreflightStatus.REVIEW_REQUIRED);
        assertThat(candidate.warnings())
                .anyMatch(warning -> warning.contains(
                        "Huevos Benedictinos != Salsa Bechamel"));
    }

    @Test
    void reportsKnownMissingRecipeImages() {
        var tresLeches = candidate("23. Tres Leches de Coco");
        var polloHongos = candidate("36. Pollo en Salsa de Hongos");
        var pastaChoux = candidate("2. Pasta choux");

        assertThat(tresLeches.imageExists()).isFalse();
        assertThat(tresLeches.imageReference()).isNull();

        assertThat(polloHongos.imageExists()).isFalse();
        assertThat(polloHongos.imageReference()).isNull();

        assertThat(pastaChoux.imageExists()).isTrue();
        assertThat(pastaChoux.imageReference())
                .startsWith("embedded-image@");
    }

    @Test
    void preflightIsDeterministic() throws Exception {
        var service = new RecipeExcelPreflightService();

        var first = RecipePreflightReportFactory.from(
                service.preflight(WORKBOOK));

        var second = RecipePreflightReportFactory.from(
                service.preflight(WORKBOOK));

        Path firstOutput = Path.of("target/preflight-first.json");
        Path secondOutput = Path.of("target/preflight-second.json");

        var writer = new RecipePreflightJsonWriter();
        writer.write(first, firstOutput);
        writer.write(second, secondOutput);

        assertThat(Files.readString(firstOutput))
                .isEqualTo(Files.readString(secondOutput));
    }

    @Test
    void preflightHasNoRepositoryDependency() {
        assertThat(Arrays.stream(
                        RecipeExcelPreflightService.class
                                .getDeclaredFields())
                .map(Field::getType))
                .doesNotContain(RecipeRepository.class);

        assertThat(Arrays.stream(
                        ExcelRecipeWorkbookReader.class
                                .getDeclaredFields())
                .map(Field::getType))
                .doesNotContain(RecipeRepository.class);

        assertThat(Arrays.stream(
                        ExcelRecipeSheetParser.class
                                .getDeclaredFields())
                .map(Field::getType))
                .doesNotContain(RecipeRepository.class);
    }

    private RecipeImportCandidate candidate(String sheetName) {
        return new RecipeExcelPreflightService()
                .preflight(WORKBOOK)
                .candidates()
                .stream()
                .filter(candidate ->
                        sheetName.equals(candidate.sourceSheet()))
                .findFirst()
                .orElseThrow();
    }
}
