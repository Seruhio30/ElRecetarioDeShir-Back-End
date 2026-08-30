package com.elrecetariodeshir.backend.admin.recipe;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AdminRecipeImageUploadValidator {

    public static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private static final Map<String, Set<String>> MIME_EXTENSIONS = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp"));

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new AdminRecipeValidationException("Image file must not be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AdminRecipeValidationException("Image file is too large.");
        }

        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        String mediaType = normalizeMediaType(file.getContentType());

        Set<String> allowedExtensions = MIME_EXTENSIONS.get(mediaType);

        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            throw new AdminRecipeValidationException("Image file type is not supported.");
        }

        byte[] header = readHeader(file);

        if (!matchesSignature(mediaType, header)) {
            throw new AdminRecipeValidationException("Image file content does not match its type.");
        }

        return new ValidatedImage(
                originalFilename,
                extension,
                mediaType);
    }

    private String normalizeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new AdminRecipeValidationException("Image filename is required.");
        }

        String normalized = filename.trim();

        if (normalized.contains("/")
                || normalized.contains("\\")
                || normalized.contains("\0")
                || normalized.equals(".")
                || normalized.equals("..")) {
            throw new AdminRecipeValidationException("Image filename is invalid.");
        }

        if (normalized.length() > 255) {
            throw new AdminRecipeValidationException("Image filename is too long.");
        }

        return normalized;
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');

        if (dot <= 0 || dot == filename.length() - 1) {
            throw new AdminRecipeValidationException("Image file extension is required.");
        }

        return filename.substring(dot + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new AdminRecipeValidationException("Image media type is required.");
        }

        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] readHeader(MultipartFile file) {
        try (var input = file.getInputStream()) {
            return input.readNBytes(12);
        } catch (IOException exception) {
            throw new AdminRecipeValidationException("Unable to inspect image file.");
        }
    }

    private boolean matchesSignature(String mediaType, byte[] header) {
        return switch (mediaType) {
            case "image/jpeg" -> header.length >= 3
                    && unsigned(header[0]) == 0xFF
                    && unsigned(header[1]) == 0xD8
                    && unsigned(header[2]) == 0xFF;

            case "image/png" -> header.length >= 8
                    && unsigned(header[0]) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;

            case "image/webp" -> header.length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';

            default -> false;
        };
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    public record ValidatedImage(
            String originalFilename,
            String extension,
            String mediaType) {
    }
}
