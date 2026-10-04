package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchCycleRequest;
import com.smartbatch360.api.batch.dto.BatchCycleResponse;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Records what the PLC reports for each mixer cycle.
 *
 * The PLC calls this rather than writing to the tables directly, so the rules
 * live in one place: the batch has to exist, a material cannot be reported
 * twice in the same cycle, and a cycle that arrives twice corrects the first
 * rather than doubling it.
 */
@Service
@Transactional
public class BatchCycleService {

    private final BatchCycleRepository batchCycleRepository;
    private final BatchRepository batchRepository;

    public BatchCycleService(BatchCycleRepository batchCycleRepository, BatchRepository batchRepository) {
        this.batchCycleRepository = batchCycleRepository;
        this.batchRepository = batchRepository;
    }

    /**
     * Records a cycle, or corrects it if the PLC has sent that cycle before.
     * A resend is the normal consequence of a dropped acknowledgement, and
     * counting it twice would overstate what the plant produced.
     */
    public BatchCycleResponse record(BatchCycleRequest request) {
        Batch batch = batchRepository.findByBatchNumberIgnoreCase(request.batchNumber())
                .orElseThrow(() -> new NotFoundException("No batch numbered '" + request.batchNumber()
                        + "' exists, so its cycles cannot be recorded."));

        rejectDuplicateMaterials(request.materials());

        BatchCycle cycle = batchCycleRepository
                .findByBatchIdAndCycleNumber(batch.getId(), request.cycleNumber())
                .orElseGet(() -> {
                    BatchCycle fresh = new BatchCycle();
                    fresh.setBatch(batch);
                    fresh.setCycleNumber(request.cycleNumber());
                    return fresh;
                });

        cycle.setCycleTime(request.cycleTime());
        applyMaterials(cycle, request.materials());

        return BatchCycleResponse.from(batchCycleRepository.save(cycle));
    }

    /**
     * Updates the cycle's materials in place rather than clearing and re-adding
     * them.
     *
     * Clearing first looks simpler and passes against H2, but fails against
     * MySQL: Hibernate orders the new inserts before the orphan deletes, so
     * re-sending a cycle hits the (cycle, material) uniqueness rule and the
     * correction is rejected as a duplicate. Matching by name touches only what
     * changed and cannot collide with a row that is on its way out.
     */
    private void applyMaterials(BatchCycle cycle, List<BatchCycleRequest.MaterialAchieved> reported) {
        Map<String, BatchCycleMaterial> existing = new LinkedHashMap<>();
        for (BatchCycleMaterial material : cycle.getMaterials()) {
            existing.put(material.getMaterialName().toLowerCase(), material);
        }

        int order = 0;
        Set<String> stillReported = new HashSet<>();
        for (BatchCycleRequest.MaterialAchieved line : reported) {
            String name = line.materialName().trim();
            String key = name.toLowerCase();
            stillReported.add(key);

            BatchCycleMaterial material = existing.get(key);
            if (material == null) {
                material = new BatchCycleMaterial();
                material.setCycle(cycle);
                material.setMaterialName(name);
                cycle.getMaterials().add(material);
            } else {
                material.setMaterialName(name);
            }
            material.setAchieved(line.achieved());
            material.setDisplayOrder(order++);
        }

        // A correction may drop a material the first message carried.
        cycle.getMaterials().removeIf(material -> !stillReported.contains(material.getMaterialName().toLowerCase()));
    }

    @Transactional(readOnly = true)
    public List<BatchCycleResponse> findForBatch(Long batchId) {
        if (!batchRepository.existsById(batchId)) {
            throw NotFoundException.forId("Batch", batchId);
        }
        return batchCycleRepository.findForReport(batchId).stream()
                .map(BatchCycleResponse::from)
                .toList();
    }

    /**
     * Two rows for the same material in one cycle would make the report's totals
     * depend on which one was read, and the database's own uniqueness rule would
     * reject the save anyway - this says so in words the caller can act on.
     */
    private void rejectDuplicateMaterials(List<BatchCycleRequest.MaterialAchieved> materials) {
        Set<String> seen = new HashSet<>();
        for (BatchCycleRequest.MaterialAchieved material : materials) {
            String name = material.materialName().trim().toLowerCase();
            if (!seen.add(name)) {
                throw new InvalidRequestException("Material '" + material.materialName().trim()
                        + "' is reported more than once in this cycle.");
            }
        }
    }

    /** Total weighed across every cycle of a batch - the report's "Achieved total". */
    @Transactional(readOnly = true)
    public BigDecimal totalAchieved(Long batchId) {
        return batchCycleRepository.findForReport(batchId).stream()
                .map(BatchCycle::totalAchieved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
