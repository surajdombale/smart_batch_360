package com.smartbatch360.api.header;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.header.dto.HeaderRequest;
import com.smartbatch360.api.header.dto.HeaderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HeaderController.class)
class HeaderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HeaderService headerService;

    private HeaderResponse sample() {
        return new HeaderResponse(1L, "SmartBatch Solutions", "Kharadi Plant", "Kharadi, Pune",
                "9876543210", "info@smartbatch.example", "27ABCDE1234F1Z5", HeaderStatus.ACTIVE,
                Instant.now(), Instant.now());
    }

    @Test
    void listReturnsHeaders() throws Exception {
        when(headerService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/headers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("SmartBatch Solutions"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(headerService.create(any())).thenReturn(sample());

        HeaderRequest request = new HeaderRequest("SmartBatch Solutions", "Kharadi Plant",
                "Kharadi, Pune", "9876543210", "info@smartbatch.example", "27ABCDE1234F1Z5", HeaderStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/headers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/headers/1"));
    }

    @Test
    void createRejectsBlankCompanyName() throws Exception {
        HeaderRequest request = new HeaderRequest("", "Kharadi Plant",
                null, null, null, null, HeaderStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/headers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("companyName"));
    }

    @Test
    void createRejectsInvalidEmail() throws Exception {
        HeaderRequest request = new HeaderRequest("SmartBatch Solutions", "Kharadi Plant",
                null, null, "not-an-email", null, HeaderStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/headers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAllowsOptionalFieldsToBeOmitted() throws Exception {
        when(headerService.create(any())).thenReturn(sample());

        HeaderRequest request = new HeaderRequest("SmartBatch Solutions", "Kharadi Plant",
                null, null, null, null, HeaderStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/headers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/headers/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
