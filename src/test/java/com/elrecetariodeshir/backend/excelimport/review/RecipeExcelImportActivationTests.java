package com.elrecetariodeshir.backend.excelimport.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.elrecetariodeshir.backend.testsupport.DatabaseIntegrationTest;

@DatabaseIntegrationTest
class RecipeExcelImportActivationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void dryRunIsSafeByDefault() {
        var properties = new RecipeExcelImportProperties();

        assertThat(properties.isDryRun()).isTrue();
    }

    @Test
    void runnerIsDisabledDuringNormalApplicationStartup() {
        assertThat(
                applicationContext.getBeansOfType(
                        RecipeExcelImportRunner.class))
                .isEmpty();
    }
}
