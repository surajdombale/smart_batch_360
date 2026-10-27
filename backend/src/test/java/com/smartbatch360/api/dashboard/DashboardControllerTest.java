package com.smartbatch360.api.dashboard;

import com.smartbatch360.api.dashboard.DashboardProductionResponse.Totals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private DashboardProductionService productionService;

    @Test
    void summaryReturnsOnlyPhase1BackedCounts() throws Exception {
        when(dashboardService.getSummary()).thenReturn(
                new DashboardSummaryResponse(18, 5, 25, 12, 1, 9, 3, "UP", "UP", "UP"));

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClients").value(18))
                .andExpect(jsonPath("$.totalVehicles").value(25))
                .andExpect(jsonPath("$.databaseStatus").value("UP"));
    }

    @Test
    void productionDefaultsToToday() throws Exception {
        when(productionService.getProduction(DashboardPeriod.TODAY)).thenReturn(production(DashboardPeriod.TODAY));

        mockMvc.perform(get("/api/v1/dashboard/production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"))
                .andExpect(jsonPath("$.current.batches").value(4))
                .andExpect(jsonPath("$.current.producedM3").value(12.5));
        verify(productionService).getProduction(DashboardPeriod.TODAY);
    }

    @Test
    void productionTakesThePeriodAsked() throws Exception {
        when(productionService.getProduction(DashboardPeriod.LAST_7_DAYS))
                .thenReturn(production(DashboardPeriod.LAST_7_DAYS));

        mockMvc.perform(get("/api/v1/dashboard/production").param("period", "LAST_7_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("LAST_7_DAYS"));
    }

    @Test
    void anUnknownPeriodIsTheCallersMistake() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/production").param("period", "FORTNIGHT"))
                .andExpect(status().isBadRequest());
    }

    private DashboardProductionResponse production(DashboardPeriod period) {
        LocalDate day = LocalDate.of(2026, 10, 27);
        return new DashboardProductionResponse(period, day, day,
                new Totals(4, new BigDecimal("12.50"), 2),
                new Totals(3, new BigDecimal("10.00"), 2),
                List.of(), List.of());
    }
}
