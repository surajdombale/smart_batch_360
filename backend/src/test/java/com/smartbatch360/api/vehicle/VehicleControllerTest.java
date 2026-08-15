package com.smartbatch360.api.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.vehicle.dto.VehicleRequest;
import com.smartbatch360.api.vehicle.dto.VehicleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;

    private VehicleResponse sample() {
        return new VehicleResponse(1L, "MH12PQ3457", 3L, "Ganesh More", new BigDecimal("6.00"),
                VehicleStatus.IN_USE, Instant.now(), Instant.now());
    }

    @Test
    void listReturnsVehicles() throws Exception {
        when(vehicleService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleNumber").value("MH12PQ3457"));
    }

    @Test
    void createRejectsNonPositiveCapacity() throws Exception {
        VehicleRequest request = new VehicleRequest("MH12PQ3457", null, new BigDecimal("0.00"), VehicleStatus.AVAILABLE);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSucceedsWithoutDriver() throws Exception {
        when(vehicleService.create(any())).thenReturn(sample());
        VehicleRequest request = new VehicleRequest("MH12PQ3457", null, new BigDecimal("6.00"), VehicleStatus.AVAILABLE);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
