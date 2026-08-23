package com.smartbatch360.api.recipe;

import com.smartbatch360.api.recipe.dto.RecipeRequest;
import com.smartbatch360.api.recipe.dto.RecipeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeResponse> list() {
        return recipeService.findAll();
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable Long id) {
        return recipeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeRequest request) {
        RecipeResponse created = recipeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/recipes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return recipeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recipeService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
