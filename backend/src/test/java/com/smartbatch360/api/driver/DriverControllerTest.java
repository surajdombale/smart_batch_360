package com.smartbatch360.api.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.driver.dto.DriverRequest;
import com.smartbatch360.api.driver.dto.DriverResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DriverService driverService;

    private DriverResponse sample() {
        return new DriverResponse(1L, "Ganesh More", "9822345678", "MH12 2019 123456",
                DriverStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    void listReturnsDrivers() throws Exception {
        when(driverService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licenseNo").value("MH12 2019 123456"));
    }

    @Test
    void createRejectsBlankLicenseNo() throws Exception {
        DriverRequest request = new DriverRequest("Ganesh More", "9822345678", "", DriverStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/drivers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("licenseNo"));
    }
}
