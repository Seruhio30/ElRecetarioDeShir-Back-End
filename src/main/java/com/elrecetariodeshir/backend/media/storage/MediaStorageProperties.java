package com.elrecetariodeshir.backend.media.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media.storage")
public class MediaStorageProperties {

    private String root;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
