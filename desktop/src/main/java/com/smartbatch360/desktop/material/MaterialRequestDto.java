package com.smartbatch360.desktop.material;

import java.math.BigDecimal;

public record MaterialRequestDto(
        String name,
        MaterialUnit unit,
        BigDecimal densityKgPerM3
) {
}
