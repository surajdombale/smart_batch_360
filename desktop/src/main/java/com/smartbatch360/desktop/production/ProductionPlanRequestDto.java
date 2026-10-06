package com.smartbatch360.desktop.production;

import java.math.BigDecimal;

public record ProductionPlanRequestDto(Long orderId, BigDecimal batchSizeM3) {
}
