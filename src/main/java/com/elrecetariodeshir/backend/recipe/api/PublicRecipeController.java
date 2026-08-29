package com.elrecetariodeshir.backend.recipe.api;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class PublicRecipeController {

    private final PublicRecipeService publicRecipeService;

    public PublicRecipeController(PublicRecipeService publicRecipeService) {
        this.publicRecipeService = publicRecipeService;
    }

    @GetMapping
    public PagedRecipeResponse listRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false, name = "q") String queryText) {

        return publicRecipeService.listRecipes(
                page,
                size,
                category,
                country,
                type,
                difficulty,
                queryText);
    }

    @GetMapping("/{slug}/images/{imageId}")
    public ResponseEntity<InputStreamResource> getRecipeImage(
            @PathVariable String slug,
            @PathVariable Long imageId) {

        PublicRecipeMediaResponse media =
                publicRecipeService.getRecipeImage(slug, imageId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.mediaType()))
                .body(new InputStreamResource(media.content()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<RecipeDetailResponse> getRecipe(
            @PathVariable String slug) {

        return ResponseEntity.ok(
                publicRecipeService.getRecipeBySlug(slug));
    }
}
