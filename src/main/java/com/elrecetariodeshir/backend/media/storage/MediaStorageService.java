package com.elrecetariodeshir.backend.media.storage;

import java.io.InputStream;

public interface MediaStorageService {

    String store(InputStream content, String extension);

    InputStream read(String storageKey);

    boolean exists(String storageKey);

    boolean delete(String storageKey);
}
