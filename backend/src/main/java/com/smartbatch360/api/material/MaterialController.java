package com.smartbatch360.api.material;

import com.smartbatch360.api.material.dto.MaterialRequest;
import com.smartbatch360.api.material.dto.MaterialResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<MaterialResponse> list() {
        return materialService.findAll();
    }

    @GetMapping("/{id}")
    public MaterialResponse get(@PathVariable Long id) {
        return materialService.findById(id);
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> create(@Valid @RequestBody MaterialRequest request) {
        MaterialResponse created = materialService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/materials/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return materialService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
