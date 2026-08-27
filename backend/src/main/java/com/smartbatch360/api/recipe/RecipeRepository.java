package com.smartbatch360.api.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** Guards Material deletion: a material still referenced by a recipe must not be removed. */
    boolean existsByMaterialsMaterialId(Long materialId);
}
