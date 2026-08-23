package com.smartbatch360.api.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.recipe.dto.RecipeMaterialRequest;
import com.smartbatch360.api.recipe.dto.RecipeMaterialResponse;
import com.smartbatch360.api.recipe.dto.RecipeRequest;
import com.smartbatch360.api.recipe.dto.RecipeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecipeService recipeService;

    private RecipeResponse sample() {
        return new RecipeResponse(1L, "M25", new BigDecimal("3.00"), "Standard M25 Grade Concrete",
                RecipeStatus.ACTIVE,
                List.of(new RecipeMaterialResponse(1L, "OPC S3 Cement", new BigDecimal("960.00"), "kg")),
                Instant.now(), Instant.now());
    }

    private RecipeRequest sampleRequest() {
        return new RecipeRequest("M25", new BigDecimal("3.00"), "Standard M25 Grade Concrete", RecipeStatus.ACTIVE,
                List.of(new RecipeMaterialRequest("OPC S3 Cement", new BigDecimal("960.00"), "kg")));
    }

    @Test
    void listReturnsRecipesWithMaterials() throws Exception {
        when(recipeService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("M25"))
                .andExpect(jsonPath("$[0].materials[0].materialName").value("OPC S3 Cement"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(recipeService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/v1/recipes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/recipes/1"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        RecipeRequest request = new RecipeRequest("", new BigDecimal("3.00"), null, RecipeStatus.ACTIVE,
                List.of(new RecipeMaterialRequest("Cement", BigDecimal.TEN, "kg")));

        mockMvc.perform(post("/api/v1/recipes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void createRejectsEmptyMaterialList() throws Exception {
        RecipeRequest request = new RecipeRequest("M25", new BigDecimal("3.00"), null, RecipeStatus.ACTIVE, List.of());

        mockMvc.perform(post("/api/v1/recipes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsInvalidMaterialQuantity() throws Exception {
        RecipeRequest request = new RecipeRequest("M25", new BigDecimal("3.00"), null, RecipeStatus.ACTIVE,
                List.of(new RecipeMaterialRequest("Cement", BigDecimal.ZERO, "kg")));

        mockMvc.perform(post("/api/v1/recipes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/recipes/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
