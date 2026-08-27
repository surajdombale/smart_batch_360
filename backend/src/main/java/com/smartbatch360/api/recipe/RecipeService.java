package com.smartbatch360.api.recipe;

import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.material.Material;
import com.smartbatch360.api.material.MaterialRepository;
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
    private final MaterialRepository materialRepository;

    public RecipeService(RecipeRepository recipeRepository, BatchRepository batchRepository,
                          MaterialRepository materialRepository) {
        this.recipeRepository = recipeRepository;
        this.batchRepository = batchRepository;
        this.materialRepository = materialRepository;
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
        recipe.setDescription(blankToNull(request.description()));
        recipe.setStatus(request.status());

        recipe.getMaterials().clear();
        int order = 0;
        for (RecipeMaterialRequest materialRequest : request.materials()) {
            Material material = materialRepository.findById(materialRequest.materialId())
                    .orElseThrow(() -> NotFoundException.forId("Material", materialRequest.materialId()));
            // Caught here rather than letting the total blow up further down, so
            // the message names the offending material and what to do about it.
            if (!material.isConvertibleToVolume()) {
                throw new InvalidRequestException("Material '" + material.getName() + "' is measured in "
                        + material.getUnit() + " but has no density set, so the total batch quantity in m3 "
                        + "cannot be calculated. Set its density under Materials first.");
            }
            RecipeMaterial line = new RecipeMaterial();
            line.setRecipe(recipe);
            line.setMaterial(material);
            line.setQuantity(materialRequest.quantity());
            line.setDisplayOrder(order++);
            recipe.getMaterials().add(line);
        }

        // Derived, never client-supplied - the whole point of the 2026-08-27 change.
        recipe.recalculateTotalBatchQuantity();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
