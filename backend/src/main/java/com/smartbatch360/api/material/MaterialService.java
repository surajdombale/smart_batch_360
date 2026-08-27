package com.smartbatch360.api.material;

import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.DuplicateResourceException;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.material.dto.MaterialRequest;
import com.smartbatch360.api.material.dto.MaterialResponse;
import com.smartbatch360.api.recipe.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final RecipeRepository recipeRepository;

    public MaterialService(MaterialRepository materialRepository, RecipeRepository recipeRepository) {
        this.materialRepository = materialRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        return materialRepository.findAll().stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse findById(Long id) {
        return MaterialResponse.from(getOrThrow(id));
    }

    public MaterialResponse create(MaterialRequest request) {
        String name = request.name().trim();
        if (materialRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A material named '" + name + "' already exists.");
        }
        Material material = new Material();
        applyRequest(material, request, name);
        return MaterialResponse.from(materialRepository.save(material));
    }

    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = getOrThrow(id);
        String name = request.name().trim();
        if (materialRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A material named '" + name + "' already exists.");
        }
        applyRequest(material, request, name);
        return MaterialResponse.from(materialRepository.save(material));
    }

    public void delete(Long id) {
        Material material = getOrThrow(id);
        if (recipeRepository.existsByMaterialsMaterialId(id)) {
            throw new ConflictException("Material '" + material.getName()
                    + "' is used by one or more recipes and cannot be deleted.");
        }
        materialRepository.delete(material);
    }

    Material getOrThrow(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Material", id));
    }

    private void applyRequest(Material material, MaterialRequest request, String name) {
        // Density is only meaningful for weight-based units, but it is what
        // makes the recipe's m3 total possible at all - so require it there
        // rather than silently accepting a material that can never be totalled.
        if (request.unit().requiresDensity() && request.densityKgPerM3() == null) {
            throw new InvalidRequestException("Density (kg/m3) is required for materials measured in "
                    + request.unit() + ", so recipe quantities can be converted to m3.");
        }
        material.setName(name);
        material.setUnit(request.unit());
        material.setDensityKgPerM3(request.unit().requiresDensity() ? request.densityKgPerM3() : null);
    }
}
