package com.smartbatch360.api.recipe;

import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.recipe.dto.RecipeMaterialRequest;
import com.smartbatch360.api.recipe.dto.RecipeRequest;
import com.smartbatch360.api.recipe.dto.RecipeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final BatchRepository batchRepository;

    public RecipeService(RecipeRepository recipeRepository, BatchRepository batchRepository) {
        this.recipeRepository = recipeRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public List<RecipeResponse> findAll() {
        return recipeRepository.findAll().stream()
                .map(RecipeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecipeResponse findById(Long id) {
        return RecipeResponse.from(getOrThrow(id));
    }

    public RecipeResponse create(RecipeRequest request) {
        Recipe recipe = new Recipe();
        applyRequest(recipe, request);
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    public RecipeResponse update(Long id, RecipeRequest request) {
        Recipe recipe = getOrThrow(id);
        applyRequest(recipe, request);
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    public void delete(Long id) {
        Recipe recipe = getOrThrow(id);
        if (batchRepository.existsByRecipeId(id)) {
            throw new ConflictException("Recipe '" + recipe.getName()
                    + "' has one or more production batches and cannot be deleted.");
        }
        recipeRepository.delete(recipe);
    }

    Recipe getOrThrow(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Recipe", id));
    }

    /** Replaces the whole material list rather than diffing row-by-row - simplest correct approach for a small list. */
    private void applyRequest(Recipe recipe, RecipeRequest request) {
        recipe.setName(request.name().trim());
        recipe.setBatchSize(request.batchSize());
        recipe.setDescription(blankToNull(request.description()));
        recipe.setStatus(request.status());

        recipe.getMaterials().clear();
        int order = 0;
        for (RecipeMaterialRequest materialRequest : request.materials()) {
            RecipeMaterial material = new RecipeMaterial();
            material.setRecipe(recipe);
            material.setMaterialName(materialRequest.materialName().trim());
            material.setQuantity(materialRequest.quantity());
            material.setUnit(materialRequest.unit().trim());
            material.setDisplayOrder(order++);
            recipe.getMaterials().add(material);
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
