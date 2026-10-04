package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchCycleRequest;
import com.smartbatch360.api.batch.dto.BatchCycleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Where the PLC reports what each mixer cycle weighed, and where the batch
 * report reads it back.
 */
@RestController
@RequestMapping("/api/v1/batch-cycles")
public class BatchCycleController {

    private final BatchCycleService batchCycleService;

    public BatchCycleController(BatchCycleService batchCycleService) {
        this.batchCycleService = batchCycleService;
    }

    /**
     * Records one cycle. The batch is named rather than given by id, because
     * the PLC works from the batch number. Re-sending a cycle corrects it, so
     * this answers 200 rather than 201.
     */
    @PostMapping
    public ResponseEntity<BatchCycleResponse> record(@Valid @RequestBody BatchCycleRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(batchCycleService.record(request));
    }

    @GetMapping("/batch/{batchId}")
    public List<BatchCycleResponse> forBatch(@PathVariable Long batchId) {
        return batchCycleService.findForBatch(batchId);
    }
}
