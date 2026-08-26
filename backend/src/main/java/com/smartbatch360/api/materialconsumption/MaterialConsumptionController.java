package com.smartbatch360.api.materialconsumption;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/material-consumption")
public class MaterialConsumptionController {

    private final MaterialConsumptionService service;

    public MaterialConsumptionController(MaterialConsumptionService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public List<MaterialConsumptionResponse> search(
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "DAY") MaterialConsumptionGroupBy groupBy) {
        return service.search(new MaterialConsumptionSearchCriteria(materialName, dateFrom, dateTo, groupBy));
    }
}
