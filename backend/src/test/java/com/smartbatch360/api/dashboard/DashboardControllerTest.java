package com.smartbatch360.api.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void summaryReturnsOnlyPhase1BackedCounts() throws Exception {
        when(dashboardService.getSummary()).thenReturn(
                new DashboardSummaryResponse(18, 5, 25, 12, 1, "UP", "UP", "UP"));

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(18))
                .andExpect(jsonPath("$.totalVehicles").value(25))
                .andExpect(jsonPath("$.databaseStatus").value("UP"));
    }
}
