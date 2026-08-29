package com.elrecetariodeshir.backend.legacyimport;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.legacy-import")
public class LegacyRecipeImportProperties {

    private String json;
    private String assetsRoot;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getAssetsRoot() {
        return assetsRoot;
    }

    public void setAssetsRoot(String assetsRoot) {
        this.assetsRoot = assetsRoot;
    }
}
