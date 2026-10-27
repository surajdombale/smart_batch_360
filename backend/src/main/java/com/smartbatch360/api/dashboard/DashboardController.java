package com.smartbatch360.api.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardProductionService productionService;

    public DashboardController(DashboardService dashboardService, DashboardProductionService productionService) {
        this.dashboardService = dashboardService;
        this.productionService = productionService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/production")
    public DashboardProductionResponse production(@RequestParam(defaultValue = "TODAY") DashboardPeriod period) {
        return productionService.getProduction(period);
    }
}
