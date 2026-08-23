package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchPageResponse;
import com.smartbatch360.api.batch.dto.BatchRequest;
import com.smartbatch360.api.batch.dto.BatchResponse;
import com.smartbatch360.api.batch.dto.BatchSearchCriteria;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public List<BatchResponse> list() {
        return batchService.findAll();
    }

    @GetMapping("/{id}")
    public BatchResponse get(@PathVariable Long id) {
        return batchService.findById(id);
    }

    /** Batch Reports: docs/02_UI_REFERENCE.md's filters (batch number/date range, client/site/vehicle/driver/recipe), paginated and sortable. */
    @GetMapping("/search")
    public BatchPageResponse search(
            @RequestParam(required = false) String batchNumberFrom,
            @RequestParam(required = false) String batchNumberTo,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long recipeId,
            @PageableDefault(size = 20) @SortDefault(sort = "cycleDateTime", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        BatchSearchCriteria criteria = new BatchSearchCriteria(batchNumberFrom, batchNumberTo, dateFrom, dateTo,
                clientId, siteId, vehicleId, driverId, recipeId);
        return batchService.search(criteria, pageable);
    }

    @PostMapping
    public ResponseEntity<BatchResponse> create(@Valid @RequestBody BatchRequest request) {
        BatchResponse created = batchService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/batches/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public BatchResponse update(@PathVariable Long id, @Valid @RequestBody BatchRequest request) {
        return batchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        batchService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/start")
    public BatchResponse start(@PathVariable Long id) {
        return batchService.start(id);
    }

    @PostMapping("/{id}/pause")
    public BatchResponse pause(@PathVariable Long id) {
        return batchService.pause(id);
    }

    @PostMapping("/{id}/resume")
    public BatchResponse resume(@PathVariable Long id) {
        return batchService.resume(id);
    }

    @PostMapping("/{id}/stop")
    public BatchResponse stop(@PathVariable Long id) {
        return batchService.stop(id);
    }

    @PostMapping("/{id}/emergency-stop")
    public BatchResponse emergencyStop(@PathVariable Long id) {
        return batchService.emergencyStop(id);
    }

    @PostMapping("/{id}/complete")
    public BatchResponse complete(@PathVariable Long id) {
        return batchService.complete(id);
    }
}
