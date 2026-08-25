package com.elrecetariodeshir.backend.recipe;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_image")
public class RecipeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "media_type", nullable = false, length = 120)
    private String mediaType;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(nullable = false)
    private int position;

    @Column(name = "primary_image", nullable = false)
    private boolean primaryImage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecipeImage() {
    }

    public RecipeImage(
            String storageKey,
            String originalFilename,
            String mediaType,
            String altText,
            int position,
            boolean primaryImage) {
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.mediaType = mediaType;
        this.altText = altText;
        this.position = position;
        this.primaryImage = primaryImage;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getAltText() {
        return altText;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPrimaryImage() {
        return primaryImage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setPrimaryImage(boolean primaryImage) {
        this.primaryImage = primaryImage;
    }
}
