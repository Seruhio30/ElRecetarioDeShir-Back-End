package com.elrecetariodeshir.backend.media.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LocalMediaStorageService implements MediaStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif");

    private final Path storageRoot;

    public LocalMediaStorageService(MediaStorageProperties properties) {
        this.storageRoot = initializeStorageRoot(properties);
    }

    @Override
    public String store(InputStream content, String extension) {
        if (content == null) {
            throw new MediaStorageException("Media content must not be null");
        }

        String normalizedExtension = normalizeExtension(extension);
        String storageKey = UUID.randomUUID() + "." + normalizedExtension;
        Path target = resolveStorageKey(storageKey);

        try {
            Files.copy(content, target);
            return storageKey;
        } catch (IOException exception) {
            throw new MediaStorageException("Failed to store media file", exception);
        }
    }

    @Override
    public InputStream read(String storageKey) {
        Path target = resolveStorageKey(storageKey);
        rejectSymbolicLink(target);

        try {
            return Files.newInputStream(
                    target,
                    StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            throw new MediaStorageException("Failed to read media file", exception);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path target = resolveStorageKey(storageKey);

        return Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(target);
    }

    @Override
    public boolean delete(String storageKey) {
        Path target = resolveStorageKey(storageKey);
        rejectSymbolicLink(target);

        try {
            return Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new MediaStorageException("Failed to delete media file", exception);
        }
    }

    private Path initializeStorageRoot(MediaStorageProperties properties) {
        if (properties == null || properties.getRoot() == null || properties.getRoot().isBlank()) {
            throw new MediaStorageException("Media storage root must be configured");
        }

        final Path configuredRoot;

        try {
            configuredRoot = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new MediaStorageException("Media storage root is invalid", exception);
        }

        if (Files.exists(configuredRoot) && !Files.isDirectory(configuredRoot)) {
            throw new MediaStorageException("Media storage root is not a directory");
        }

        try {
            Files.createDirectories(configuredRoot);
        } catch (IOException exception) {
            throw new MediaStorageException("Media storage root could not be created", exception);
        }

        if (!Files.isDirectory(configuredRoot)) {
            throw new MediaStorageException("Media storage root is not a directory");
        }

        if (!Files.isWritable(configuredRoot)) {
            throw new MediaStorageException("Media storage root is not writable");
        }

        try {
            return configuredRoot.toRealPath();
        } catch (IOException exception) {
            throw new MediaStorageException("Media storage root could not be resolved", exception);
        }
    }

    private void rejectSymbolicLink(Path target) {
        if (Files.isSymbolicLink(target)) {
            throw new MediaStorageException("Symbolic links are not allowed in media storage");
        }
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new MediaStorageException("Media file extension must be provided");
        }

        String normalized = extension
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }

        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new MediaStorageException("Media file extension is not supported");
        }

        return normalized;
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new MediaStorageException("Storage key must be provided");
        }

        final Path keyPath;

        try {
            keyPath = Path.of(storageKey);
        } catch (RuntimeException exception) {
            throw new MediaStorageException("Storage key is invalid", exception);
        }

        if (keyPath.isAbsolute()) {
            throw new MediaStorageException("Absolute storage keys are not allowed");
        }

        Path resolved = storageRoot.resolve(keyPath).normalize();

        if (!resolved.startsWith(storageRoot)) {
            throw new MediaStorageException("Storage key escapes the configured storage root");
        }

        if (keyPath.getNameCount() != 1) {
            throw new MediaStorageException("Nested storage keys are not allowed");
        }

        return resolved;
    }
}
