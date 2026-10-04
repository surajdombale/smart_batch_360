package com.smartbatch360.api.batch.dto;

import com.smartbatch360.api.batch.BatchCycle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One recorded cycle, with its row total - the batch report's cycle row. */
public record BatchCycleResponse(
        Long id,
        Long batchId,
        String batchNumber,
        Integer cycleNumber,
        Instant cycleTime,
        List<MaterialAchievedResponse> materials,
        BigDecimal totalAchieved
) {

    public record MaterialAchievedResponse(String materialName, BigDecimal achieved) {
    }

    public static BatchCycleResponse from(BatchCycle cycle) {
        return new BatchCycleResponse(
                cycle.getId(),
                cycle.getBatch().getId(),
                cycle.getBatch().getBatchNumber(),
                cycle.getCycleNumber(),
                cycle.getCycleTime(),
                cycle.getMaterials().stream()
                        .map(m -> new MaterialAchievedResponse(m.getMaterialName(), m.getAchieved()))
                        .toList(),
                cycle.totalAchieved());
    }
}
