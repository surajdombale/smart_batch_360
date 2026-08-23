package com.smartbatch360.desktop.batch;

import java.math.BigDecimal;

public record BatchMaterialRequestDto(
        String materialName,
        BigDecimal target,
        BigDecimal setpoint,
        BigDecimal achieved,
        String unit
) {
}
