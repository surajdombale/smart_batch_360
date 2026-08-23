package com.smartbatch360.api.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.batch.dto.BatchMaterialRequest;
import com.smartbatch360.api.batch.dto.BatchMaterialResponse;
import com.smartbatch360.api.batch.dto.BatchPageResponse;
import com.smartbatch360.api.batch.dto.BatchRequest;
import com.smartbatch360.api.batch.dto.BatchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BatchController.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchService batchService;

    private BatchResponse sample() {
        return new BatchResponse(1L, "250201", 1L, "M25", 2L, "SmartBatch Solutions", 3L, "Kharadi",
                4L, "MH12PQ3457", 5L, "Ganesh More",
                new BigDecimal("3.00"), new BigDecimal("2.40"), new BigDecimal("0.60"),
                Instant.now(), 1, "Day", BatchStatus.IN_PROGRESS,
                EquipmentStatus.RUNNING, EquipmentStatus.RUNNING, EquipmentStatus.RUNNING,
                EquipmentStatus.RUNNING, EquipmentStatus.RUNNING,
                List.of(new BatchMaterialResponse(1L, "OPC S3 Cement", new BigDecimal("960.00"),
                        new BigDecimal("960.00"), new BigDecimal("958.00"), "kg")),
                Instant.now(), Instant.now());
    }

    private BatchRequest sampleRequest() {
        return new BatchRequest("250201", 1L, 2L, 3L, 4L, 5L,
                new BigDecimal("3.00"), BigDecimal.ZERO, null, 1, "Day",
                BatchStatus.PENDING, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                EquipmentStatus.STOPPED, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                List.of(new BatchMaterialRequest("OPC S3 Cement", new BigDecimal("960.00"),
                        new BigDecimal("960.00"), BigDecimal.ZERO, "kg")));
    }

    @Test
    void listReturnsBatches() throws Exception {
        when(batchService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchNumber").value("250201"))
                .andExpect(jsonPath("$[0].recipeName").value("M25"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(batchService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/v1/batches")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/batches/1"));
    }

    @Test
    void createRejectsBlankBatchNumber() throws Exception {
        BatchRequest request = new BatchRequest("", 1L, 2L, 3L, 4L, 5L,
                new BigDecimal("3.00"), BigDecimal.ZERO, null, 1, "Day",
                BatchStatus.PENDING, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                EquipmentStatus.STOPPED, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                List.of(new BatchMaterialRequest("Cement", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, "kg")));

        mockMvc.perform(post("/api/v1/batches")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("batchNumber"));
    }

    @Test
    void searchReturnsPagedResults() throws Exception {
        BatchPageResponse page = new BatchPageResponse(List.of(sample()), 0, 20, 1, 1);
        when(batchService.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/batches/search")
                        .param("batchNumberFrom", "250000")
                        .param("batchNumberTo", "250300")
                        .param("clientId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].batchNumber").value("250201"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void startReturnsUpdatedBatch() throws Exception {
        when(batchService.start(eq(1L))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/batches/{id}/start", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void emergencyStopReturnsUpdatedBatch() throws Exception {
        when(batchService.emergencyStop(eq(1L))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/batches/{id}/emergency-stop", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/batches/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
