package com.smartbatch360.api.site;

import com.smartbatch360.api.site.dto.SiteRequest;
import com.smartbatch360.api.site.dto.SiteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public List<SiteResponse> list() {
        return siteService.findAll();
    }

    @GetMapping("/{id}")
    public SiteResponse get(@PathVariable Long id) {
        return siteService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SiteResponse> create(@Valid @RequestBody SiteRequest request) {
        SiteResponse created = siteService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sites/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public SiteResponse update(@PathVariable Long id, @Valid @RequestBody SiteRequest request) {
        return siteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        siteService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
