package com.elrecetariodeshir.backend.excelimport.review;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.excel-import")
public class RecipeExcelImportProperties {

    private String workbook;
    private String overrides;
    private String report = "target/recipe-import-dry-run.json";
    private String legacyCatalog;
    private boolean dryRun = true;

    public String getWorkbook() {
        return workbook;
    }

    public void setWorkbook(String workbook) {
        this.workbook = workbook;
    }

    public String getOverrides() {
        return overrides;
    }

    public void setOverrides(String overrides) {
        this.overrides = overrides;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getLegacyCatalog() {
        return legacyCatalog;
    }

    public void setLegacyCatalog(String legacyCatalog) {
        this.legacyCatalog = legacyCatalog;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
