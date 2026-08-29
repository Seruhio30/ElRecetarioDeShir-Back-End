package com.elrecetariodeshir.backend.media.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMediaStorageServiceTests {

    @TempDir
    Path tempDirectory;

    @Test
    void storesAndReadsMedia() throws IOException {
        LocalMediaStorageService storage = storageAt(tempDirectory);
        byte[] content = "recipe-image".getBytes(StandardCharsets.UTF_8);

        String storageKey = storage.store(
                new ByteArrayInputStream(content),
                "jpg");

        assertThat(storageKey)
                .matches("[0-9a-f\\-]{36}\\.jpg");

        assertThat(storage.read(storageKey).readAllBytes())
                .isEqualTo(content);
    }

    @Test
    void reportsWhetherStoredMediaExists() {
        LocalMediaStorageService storage = storageAt(tempDirectory);

        String storageKey = storage.store(
                new ByteArrayInputStream(new byte[] { 1, 2, 3 }),
                "png");

        assertThat(storage.exists(storageKey)).isTrue();
        assertThat(storage.exists("00000000-0000-0000-0000-000000000000.png"))
                .isFalse();
    }

    @Test
    void deletesMediaAndSecondDeleteIsNoOp() {
        LocalMediaStorageService storage = storageAt(tempDirectory);

        String storageKey = storage.store(
                new ByteArrayInputStream(new byte[] { 1 }),
                "webp");

        assertThat(storage.delete(storageKey)).isTrue();
        assertThat(storage.exists(storageKey)).isFalse();
        assertThat(storage.delete(storageKey)).isFalse();
    }

    @Test
    void generatesUniqueStorageKeysWithoutOverwritingExistingMedia() throws IOException {
        LocalMediaStorageService storage = storageAt(tempDirectory);

        byte[] firstContent = "first".getBytes(StandardCharsets.UTF_8);
        byte[] secondContent = "second".getBytes(StandardCharsets.UTF_8);

        String firstKey = storage.store(
                new ByteArrayInputStream(firstContent),
                ".jpeg");

        String secondKey = storage.store(
                new ByteArrayInputStream(secondContent),
                "jpeg");

        assertThat(firstKey).isNotEqualTo(secondKey);

        assertThat(storage.read(firstKey).readAllBytes())
                .isEqualTo(firstContent);

        assertThat(storage.read(secondKey).readAllBytes())
                .isEqualTo(secondContent);
    }

    @Test
    void rejectsPathTraversalStorageKeys() {
        LocalMediaStorageService storage = storageAt(tempDirectory);

        assertThatThrownBy(() -> storage.read("../outside.jpg"))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("storage root");

        assertThatThrownBy(() -> storage.exists("nested/../../outside.jpg"))
                .isInstanceOf(MediaStorageException.class);

        assertThatThrownBy(() -> storage.delete("../outside.jpg"))
                .isInstanceOf(MediaStorageException.class);
    }

    @Test
    void rejectsAbsoluteStorageKeys() {
        LocalMediaStorageService storage = storageAt(tempDirectory);
        String absolutePath = tempDirectory.resolve("outside.jpg")
                .toAbsolutePath()
                .toString();

        assertThatThrownBy(() -> storage.read(absolutePath))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("Absolute storage keys");

        assertThatThrownBy(() -> storage.delete(absolutePath))
                .isInstanceOf(MediaStorageException.class);
    }

    @Test
    void rejectsNestedStorageKeysEvenWhenTheyRemainInsideRoot() {
        LocalMediaStorageService storage = storageAt(tempDirectory);

        assertThatThrownBy(() -> storage.exists("recipes/image.jpg"))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("Nested storage keys");
    }

    @Test
    void rejectsSymbolicLinksThatCouldEscapeStorageRoot() throws IOException {
        Path root = tempDirectory.resolve("storage");
        Files.createDirectories(root);

        Path outsideFile = tempDirectory.resolve("outside.jpg");
        Files.writeString(outsideFile, "outside");

        Path symbolicLink = root.resolve("linked.jpg");
        Files.createSymbolicLink(symbolicLink, outsideFile);

        LocalMediaStorageService storage = storageAt(root);

        assertThat(storage.exists("linked.jpg")).isFalse();

        assertThatThrownBy(() -> storage.read("linked.jpg"))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("Symbolic links");

        assertThatThrownBy(() -> storage.delete("linked.jpg"))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("Symbolic links");

        assertThat(outsideFile).exists();
    }

    @Test
    void rejectsUnsupportedOrMaliciousExtensions() {
        LocalMediaStorageService storage = storageAt(tempDirectory);

        assertThatThrownBy(() -> storage.store(
                new ByteArrayInputStream(new byte[] { 1 }),
                "../jpg"))
                .isInstanceOf(MediaStorageException.class);

        assertThatThrownBy(() -> storage.store(
                new ByteArrayInputStream(new byte[] { 1 }),
                "exe"))
                .isInstanceOf(MediaStorageException.class);
    }

    @Test
    void createsConfiguredStorageRootWhenItDoesNotExist() {
        Path root = tempDirectory.resolve("media").resolve("private");

        assertThat(root).doesNotExist();

        LocalMediaStorageService storage = storageAt(root);

        assertThat(root).isDirectory();

        String storageKey = storage.store(
                new ByteArrayInputStream(new byte[] { 1 }),
                "gif");

        assertThat(storage.exists(storageKey)).isTrue();
    }

    @Test
    void rejectsStorageRootThatIsNotADirectory() throws IOException {
        Path invalidRoot = tempDirectory.resolve("media-file");
        Files.writeString(invalidRoot, "not-a-directory");

        MediaStorageProperties properties = propertiesWithRoot(invalidRoot);

        assertThatThrownBy(() -> new LocalMediaStorageService(properties))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("not a directory");
    }

    @Test
    void rejectsMissingStorageRootConfiguration() {
        MediaStorageProperties properties = new MediaStorageProperties();

        assertThatThrownBy(() -> new LocalMediaStorageService(properties))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("must be configured");
    }

    private LocalMediaStorageService storageAt(Path root) {
        return new LocalMediaStorageService(propertiesWithRoot(root));
    }

    private MediaStorageProperties propertiesWithRoot(Path root) {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setRoot(root.toString());
        return properties;
    }
}
