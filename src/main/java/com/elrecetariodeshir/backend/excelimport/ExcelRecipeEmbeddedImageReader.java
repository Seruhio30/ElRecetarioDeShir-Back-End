package com.elrecetariodeshir.backend.excelimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelRecipeEmbeddedImageReader {

    public EmbeddedImage read(
            Path workbookPath,
            String sourceSheet,
            String expectedReference) {

        if (workbookPath == null || !Files.isRegularFile(workbookPath)) {
            throw new IllegalArgumentException(
                    "Recipe workbook does not exist: " + workbookPath);
        }

        if (sourceSheet == null || sourceSheet.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipe source sheet is required");
        }

        try (InputStream input = Files.newInputStream(workbookPath);
                XSSFWorkbook workbook = new XSSFWorkbook(input)) {

            var sheet = workbook.getSheet(sourceSheet);

            if (sheet == null) {
                throw new IllegalArgumentException(
                        "Recipe sheet does not exist: " + sourceSheet);
            }

            var drawing = sheet.getDrawingPatriarch();

            if (drawing == null) {
                return null;
            }

            for (var shape : drawing) {
                if (!(shape instanceof XSSFPicture picture)) {
                    continue;
                }

                var data = picture.getPictureData();
                String extension = normalizeExtension(
                        data.suggestFileExtension());

                var anchor = picture.getClientAnchor();

                boolean logo = "png".equals(extension)
                        && picture.getImageDimension().width == 153
                        && picture.getImageDimension().height == 148
                        && anchor.getRow1() < 5;

                if (logo) {
                    continue;
                }

                String reference = "embedded-image@"
                        + (anchor.getRow1() + 1)
                        + ":"
                        + (anchor.getCol1() + 1)
                        + "."
                        + extension;

                if (expectedReference != null
                        && !expectedReference.equals(reference)) {
                    continue;
                }

                return new EmbeddedImage(
                        data.getData(),
                        extension,
                        mediaType(extension),
                        reference);
            }

            return null;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read embedded recipe image from workbook: "
                            + workbookPath,
                    exception);
        }
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalStateException(
                    "Embedded recipe image extension is missing");
        }

        String normalized = extension
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "jpg", "jpeg", "png" -> normalized;
            default -> throw new IllegalStateException(
                    "Unsupported embedded recipe image extension: "
                            + normalized);
        };
    }

    private String mediaType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> throw new IllegalStateException(
                    "Unsupported embedded recipe image extension: "
                            + extension);
        };
    }

    public record EmbeddedImage(
            byte[] bytes,
            String extension,
            String mediaType,
            String reference) {

        public EmbeddedImage {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
