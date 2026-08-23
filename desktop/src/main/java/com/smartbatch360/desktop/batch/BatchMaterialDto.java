package com.smartbatch360.desktop.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchMaterialDto(
        Long id,
        String materialName,
        BigDecimal target,
        BigDecimal setpoint,
        BigDecimal achieved,
        String unit
) {
}
