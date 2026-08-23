package com.smartbatch360.api.batch.dto;

import com.smartbatch360.api.batch.BatchMaterial;

import java.math.BigDecimal;

public record BatchMaterialResponse(
        Long id,
        String materialName,
        BigDecimal target,
        BigDecimal setpoint,
        BigDecimal achieved,
        String unit
) {
    public static BatchMaterialResponse from(BatchMaterial m) {
        return new BatchMaterialResponse(m.getId(), m.getMaterialName(), m.getTarget(), m.getSetpoint(),
                m.getAchieved(), m.getUnit());
    }
}
