package com.elrecetariodeshir.backend.excelimport.review;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elrecetariodeshir.backend.excelimport.RecipeExcelPreflightService;
import com.elrecetariodeshir.backend.recipe.Recipe;
import com.elrecetariodeshir.backend.recipe.RecipeRepository;

@Service
public class RecipeExcelDryRunService {

    private final RecipeRepository recipeRepository;

    private final RecipeExcelPreflightService preflightService =
            new RecipeExcelPreflightService();

    private final RecipeImportOverrideLoader overrideLoader =
            new RecipeImportOverrideLoader();

    private final RecipeImportReviewService reviewService =
            new RecipeImportReviewService();

    private final RecipeExcelImportPlanner planner =
            new RecipeExcelImportPlanner();

    public RecipeExcelDryRunService(
            RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public RecipeExcelDryRunReport dryRun(
            Path workbookPath,
            Path overridesPath,
            Path legacyCatalogPath) {

        var preflight = preflightService.preflight(workbookPath);
        var overrides = overrideLoader.load(overridesPath);

        var reviewed = reviewService.review(
                preflight.candidates(),
                overrides);

        List<ReviewedRecipeImportCandidate> approved =
                reviewed.stream()
                        .filter(candidate -> candidate.status()
                                == ReviewedRecipeImportCandidate.ReviewStatus.APPROVED_FOR_IMPORT)
                        .toList();

        Set<String> slugs = approved.stream()
                .map(ReviewedRecipeImportCandidate::slug)
                .collect(Collectors.toSet());

        Set<String> names = approved.stream()
                .map(ReviewedRecipeImportCandidate::finalName)
                .collect(Collectors.toSet());

        List<Recipe> existing = approved.isEmpty()
                ? List.of()
                : recipeRepository.findAllBySlugInOrNameIn(
                        slugs,
                        names);

        RecipeImportPlan plan =
                planner.plan(reviewed, existing);

        var legacyCollisions =
                new RecipeLegacyCollisionDetector()
                        .detect(
                                preflight.candidates(),
                                legacyCatalogPath);

        return new RecipeExcelDryRunReport(
                plan,
                legacyCollisions.size(),
                legacyCollisions);
    }
}
