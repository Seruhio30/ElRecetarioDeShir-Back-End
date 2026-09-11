package com.elrecetariodeshir.backend.excelimport.review;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class RecipeImportPlanJsonWriter {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void write(
            RecipeExcelDryRunReport report,
            Path outputPath) {

        try {
            Path parent = outputPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            objectMapper.writeValue(
                    outputPath.toFile(),
                    report);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not write recipe import dry-run report: "
                            + outputPath,
                    exception);
        }
    }
}
