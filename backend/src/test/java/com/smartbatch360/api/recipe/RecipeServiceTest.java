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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private MaterialRepository materialRepository;

    private RecipeService service() {
        return new RecipeService(recipeRepository, batchRepository, materialRepository);
    }

    private Material material(Long id, String name) {
        Material m = new Material();
        m.setName(name);
        // id has no setter (generated); tests only need findById to return this.
        when(materialRepository.findById(id)).thenReturn(Optional.of(m));
        return m;
    }

    private RecipeRequest sampleRequest() {
        material(1L, "OPC S3 Cement");
        material(2L, "Fly Ash");
        material(3L, "Water");
        return new RecipeRequest("M25", "Standard M25 Grade Concrete", RecipeStatus.ACTIVE,
                List.of(
                        new RecipeMaterialRequest(1L, new BigDecimal("960.00")),
                        new RecipeMaterialRequest(2L, new BigDecimal("240.00")),
                        new RecipeMaterialRequest(3L, new BigDecimal("540.00"))
                ));
    }

    @Test
    void createsRecipeWithMaterialsInOrder() {
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeResponse response = service().create(sampleRequest());

        assertThat(response.name()).isEqualTo("M25");
        assertThat(response.materials()).hasSize(3);
        assertThat(response.materials().get(0).materialName()).isEqualTo("OPC S3 Cement");
        assertThat(response.materials().get(2).materialName()).isEqualTo("Water");
        verify(recipeRepository).save(any(Recipe.class));
    }

    /**
     * The core of the 2026-08-27 change: the batch quantity is derived, not
     * supplied. 960kg / 1440 + 240kg / 2200 + 540L / 1000 = 1.3158 m3.
     */
    /**
     * Since 2026-09-07 the batch quantity is simply what goes into the batch:
     * every material is kilograms, so the total is their sum and nothing has to
     * be converted (or known) to work it out.
     */
    @Test
    void derivesTotalBatchQuantityAsTheSumOfItsMaterials() {
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeResponse response = service().create(sampleRequest());

        assertThat(response.totalBatchQuantityKg()).isEqualByComparingTo("1740.00");
    }

    @Test
    void rejectsUnknownMaterial() {
        when(materialRepository.findById(77L)).thenReturn(Optional.empty());

        RecipeRequest request = new RecipeRequest("Ghost", null, RecipeStatus.ACTIVE,
                List.of(new RecipeMaterialRequest(77L, BigDecimal.ONE)));

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateReplacesMaterialListEntirely() {
        Recipe existing = new Recipe();
        existing.setName("M25");
        existing.setTotalBatchQuantityKg(new BigDecimal("3.0000"));
        existing.setStatus(RecipeStatus.ACTIVE);
        Material old = new Material();
        old.setName("Old Material");
        RecipeMaterial oldLine = new RecipeMaterial();
        oldLine.setRecipe(existing);
        oldLine.setMaterial(old);
        oldLine.setQuantity(BigDecimal.ONE);
        existing.getMaterials().add(oldLine);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeResponse response = service().update(1L, sampleRequest());

        assertThat(response.materials()).hasSize(3);
        assertThat(response.materials()).noneMatch(m -> m.materialName().equals("Old Material"));
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectedWhenRecipeHasBatches() {
        Recipe recipe = new Recipe();
        recipe.setName("M25");
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(batchRepository.existsByRecipeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service().delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("batches");

        verify(recipeRepository, never()).delete(any());
    }

    @Test
    void deleteSucceeds() {
        Recipe recipe = new Recipe();
        recipe.setName("M25");
        when(recipeRepository.findById(2L)).thenReturn(Optional.of(recipe));
        when(batchRepository.existsByRecipeId(2L)).thenReturn(false);

        service().delete(2L);

        verify(recipeRepository).delete(recipe);
    }
}
