package com.smartbatch360.api.header;

import com.smartbatch360.api.header.dto.HeaderRequest;
import com.smartbatch360.api.header.dto.HeaderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/headers")
public class HeaderController {

    private final HeaderService headerService;

    public HeaderController(HeaderService headerService) {
        this.headerService = headerService;
    }

    @GetMapping
    public List<HeaderResponse> list() {
        return headerService.findAll();
    }

    @GetMapping("/{id}")
    public HeaderResponse get(@PathVariable Long id) {
        return headerService.findById(id);
    }

    @PostMapping
    public ResponseEntity<HeaderResponse> create(@Valid @RequestBody HeaderRequest request) {
        HeaderResponse created = headerService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/headers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public HeaderResponse update(@PathVariable Long id, @Valid @RequestBody HeaderRequest request) {
        return headerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        headerService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
