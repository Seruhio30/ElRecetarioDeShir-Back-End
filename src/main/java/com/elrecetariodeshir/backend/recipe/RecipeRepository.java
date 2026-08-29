package com.elrecetariodeshir.backend.recipe;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findAllBySlugInOrNameIn(
            Collection<String> slugs,
            Collection<String> names);
}
