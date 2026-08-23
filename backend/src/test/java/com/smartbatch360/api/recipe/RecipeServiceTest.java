package com.smartbatch360.api.recipe;

import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.recipe.dto.RecipeMaterialRequest;
import com.smartbatch360.api.recipe.dto.RecipeRequest;
import com.smartbatch360.api.recipe.dto.RecipeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    private RecipeService service() {
        return new RecipeService(recipeRepository);
    }

    private RecipeRequest sampleRequest() {
        return new RecipeRequest("M25", new BigDecimal("3.00"), "Standard M25 Grade Concrete", RecipeStatus.ACTIVE,
                List.of(
                        new RecipeMaterialRequest("OPC S3 Cement", new BigDecimal("960.00"), "kg"),
                        new RecipeMaterialRequest("Fly Ash", new BigDecimal("240.00"), "kg"),
                        new RecipeMaterialRequest("Water", new BigDecimal("540.00"), "L")
                ));
    }

    @Test
    void createsRecipeWithMaterialsInOrder() {
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeResponse response = service().create(sampleRequest());

        assertThat(response.name()).isEqualTo("M25");
        assertThat(response.batchSize()).isEqualByComparingTo("3.00");
        assertThat(response.materials()).hasSize(3);
        assertThat(response.materials().get(0).materialName()).isEqualTo("OPC S3 Cement");
        assertThat(response.materials().get(2).materialName()).isEqualTo("Water");
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void updateReplacesMaterialListEntirely() {
        Recipe existing = new Recipe();
        existing.setName("M25");
        existing.setBatchSize(new BigDecimal("3.00"));
        existing.setStatus(RecipeStatus.ACTIVE);
        RecipeMaterial oldMaterial = new RecipeMaterial();
        oldMaterial.setRecipe(existing);
        oldMaterial.setMaterialName("Old Material");
        oldMaterial.setQuantity(BigDecimal.ONE);
        oldMaterial.setUnit("kg");
        existing.getMaterials().add(oldMaterial);

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
    void deleteSucceeds() {
        Recipe recipe = new Recipe();
        recipe.setName("M25");
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));

        service().delete(1L);

        verify(recipeRepository).delete(recipe);
    }
}
