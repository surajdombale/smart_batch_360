package com.smartbatch360.desktop.production;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One cycle typed in on the Production screen, in the shape the PLC sends it. */
public record BatchCycleRequestDto(
        String batchNumber,
        Integer cycleNumber,
        Instant cycleTime,
        List<Material> materials
) {

    public record Material(String materialName, BigDecimal achieved) {
    }
}
