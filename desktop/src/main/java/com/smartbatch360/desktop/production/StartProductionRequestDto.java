package com.smartbatch360.desktop.production;

import java.math.BigDecimal;

public record StartProductionRequestDto(
        Long orderId,
        Long vehicleId,
        BigDecimal batchSizeM3,
        String batchNumber,
        String shift
) {
}
