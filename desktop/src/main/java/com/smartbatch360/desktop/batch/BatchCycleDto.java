package com.smartbatch360.desktop.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One mixer cycle of a batch, as the batch report prints it. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchCycleDto(
        Long id,
        Integer cycleNumber,
        Instant cycleTime,
        List<Material> materials,
        BigDecimal totalAchieved
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Material(
            String materialName,
            BigDecimal target,
            BigDecimal setpoint,
            BigDecimal achieved
    ) {
    }
}
