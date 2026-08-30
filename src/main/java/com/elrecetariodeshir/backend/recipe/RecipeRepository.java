package com.elrecetariodeshir.backend.recipe;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository
        extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    List<Recipe> findAllBySlugInOrNameIn(
            Collection<String> slugs,
            Collection<String> names);


    boolean existsBySlug(String slug);
}
