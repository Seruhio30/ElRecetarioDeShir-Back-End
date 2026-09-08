package com.elrecetariodeshir.backend.excelimport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class RecipePreflightJsonWriter {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void write(
            RecipePreflightReport report,
            Path outputPath) {

        try {
            Path parent = outputPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            objectMapper.writeValue(outputPath.toFile(), report);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not write recipe preflight report: "
                            + outputPath,
                    exception);
        }
    }
}
