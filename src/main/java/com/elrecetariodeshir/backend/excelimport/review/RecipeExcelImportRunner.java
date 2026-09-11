package com.elrecetariodeshir.backend.excelimport.review;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("excel-import")
@ConditionalOnProperty(
        name = "app.excel-import.enabled",
        havingValue = "true")
@EnableConfigurationProperties(RecipeExcelImportProperties.class)
public class RecipeExcelImportRunner implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecipeExcelImportRunner.class);

    private final RecipeExcelImportProperties properties;
    private final RecipeExcelDryRunService dryRunService;
    private final RecipeExcelImportService importService;

    public RecipeExcelImportRunner(
            RecipeExcelImportProperties properties,
            RecipeExcelDryRunService dryRunService,
            RecipeExcelImportService importService) {
        this.properties = properties;
        this.dryRunService = dryRunService;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path workbookPath = requiredPath(
                properties.getWorkbook(),
                "RECIPE_EXCEL_PATH");

        Path overridesPath = requiredPath(
                properties.getOverrides(),
                "EXCEL_RECIPE_IMPORT_OVERRIDES");

        Path legacyCatalogPath = requiredPath(
                properties.getLegacyCatalog(),
                "EXCEL_RECIPE_LEGACY_CATALOG");

        RecipeExcelDryRunReport report =
                dryRunService.dryRun(
                        workbookPath,
                        overridesPath,
                        legacyCatalogPath);

        RecipeImportPlan plan = report.plan();

        logPlan(plan);

        LOGGER.info(
                "Excel recipe legacy collisions: {}",
                report.legacyCollisionCount());

        if (properties.isDryRun()) {
            Path reportPath = requiredPathValue(
                    properties.getReport(),
                    "EXCEL_RECIPE_IMPORT_REPORT");

            new RecipeImportPlanJsonWriter()
                    .write(report, reportPath);

            LOGGER.info(
                    "Excel recipe import dry-run completed. Report: {}",
                    reportPath);

            return;
        }

        if (plan.conflicts() > 0
                || plan.partialStates() > 0
                || report.legacyCollisionCount() > 0) {
            throw new RecipeImportReviewException(
                    "Excel recipe import cannot continue with conflicts, partial states, or legacy collisions");
        }

        var candidates = plan.entries().stream()
                .map(RecipeImportPlan.Entry::candidate)
                .toList();

        RecipeExcelImportResult result =
                importService.importApproved(
                        workbookPath,
                        candidates);

        LOGGER.info(
                "Excel recipe import finished: imported={}, alreadyImported={}, pending={}, conflicts={}, partialStates={}",
                result.imported(),
                result.skippedAlreadyImported(),
                result.pending(),
                result.conflicts(),
                result.partialStates());
    }

    private void logPlan(RecipeImportPlan plan) {
        LOGGER.info(
                "Excel recipe import plan: detected={}, approved={}, pending={}, new={}, alreadyImported={}, conflicts={}, partialStates={}, withImage={}, withoutImage={}, ingredients={}, steps={}",
                plan.detected(),
                plan.approved(),
                plan.pending(),
                plan.newRecipes(),
                plan.alreadyImported(),
                plan.conflicts(),
                plan.partialStates(),
                plan.withImage(),
                plan.withoutImage(),
                plan.ingredients(),
                plan.steps());
    }

    private Path requiredPath(
            String value,
            String configurationName) {

        Path path = requiredPathValue(
                value,
                configurationName);

        if (!java.nio.file.Files.isRegularFile(path)) {
            throw new RecipeImportReviewException(
                    configurationName
                            + " must point to an existing file");
        }

        return path;
    }

    private Path requiredPathValue(
            String value,
            String configurationName) {

        if (value == null || value.isBlank()) {
            throw new RecipeImportReviewException(
                    configurationName
                            + " must be configured");
        }

        try {
            return Path.of(value)
                    .toAbsolutePath()
                    .normalize();
        } catch (InvalidPathException exception) {
            throw new RecipeImportReviewException(
                    configurationName
                            + " is invalid",
                    exception);
        }
    }
}
