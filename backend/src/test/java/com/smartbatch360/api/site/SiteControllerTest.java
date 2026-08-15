package com.smartbatch360.api.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.site.dto.SiteRequest;
import com.smartbatch360.api.site.dto.SiteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SiteController.class)
class SiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SiteService siteService;

    private SiteResponse sample() {
        return new SiteResponse(1L, "Kharadi", 5L, "SmartBatch Solutions", "Pune", SiteStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    void listReturnsSites() throws Exception {
        when(siteService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("SmartBatch Solutions"));
    }

    @Test
    void createRejectsMissingCustomerId() throws Exception {
        SiteRequest request = new SiteRequest("Kharadi", null, "Pune", SiteStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/sites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedSite() throws Exception {
        when(siteService.update(eq(1L), any())).thenReturn(sample());
        SiteRequest request = new SiteRequest("Kharadi", 5L, "Pune", SiteStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/sites/{id}", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Pune"));
    }
}
