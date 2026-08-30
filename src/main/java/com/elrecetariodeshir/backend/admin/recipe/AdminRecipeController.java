package com.elrecetariodeshir.backend.admin.recipe;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elrecetariodeshir.backend.recipe.RecipeStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/recipes")
public class AdminRecipeController {

    private final AdminRecipeService adminRecipeService;

    public AdminRecipeController(AdminRecipeService adminRecipeService) {
        this.adminRecipeService = adminRecipeService;
    }

    @GetMapping
    public AdminPagedRecipeResponse listRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) RecipeStatus status,
            @RequestParam(required = false, name = "q") String queryText) {

        return adminRecipeService.listRecipes(
                page,
                size,
                status,
                queryText);
    }

    @GetMapping("/{id}")
    public AdminRecipeDetailResponse getRecipe(
            @PathVariable Long id) {

        return adminRecipeService.getRecipe(id);
    }

    @PostMapping
    public ResponseEntity<AdminRecipeDetailResponse> createRecipe(
            @Valid @RequestBody AdminRecipeWriteRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminRecipeService.createRecipe(request));
    }

    @PatchMapping("/{id}")
    public AdminRecipeDetailResponse updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody AdminRecipeWriteRequest request) {

        return adminRecipeService.updateRecipe(id, request);
    }

    @PostMapping("/{id}/publish")
    public AdminRecipeDetailResponse publish(
            @PathVariable Long id) {

        return adminRecipeService.publish(id);
    }

    @PostMapping("/{id}/archive")
    public AdminRecipeDetailResponse archive(
            @PathVariable Long id) {

        return adminRecipeService.archive(id);
    }

    @PostMapping("/{id}/restore")
    public AdminRecipeDetailResponse restore(
            @PathVariable Long id) {

        return adminRecipeService.restore(id);
    }
}
