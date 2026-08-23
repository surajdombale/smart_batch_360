package com.smartbatch360.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.client.dto.ClientRequest;
import com.smartbatch360.api.client.dto.ClientResponse;
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

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    private ClientResponse sample() {
        return new ClientResponse(1L, "SmartBatch Solutions", "Rahul Deshmukh", "9876543210", "Kharadi, Pune",
                ClientStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    void listReturnsClients() throws Exception {
        when(clientService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("SmartBatch Solutions"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(clientService.create(any())).thenReturn(sample());

        ClientRequest request = new ClientRequest("SmartBatch Solutions", "Rahul Deshmukh", "9876543210",
                "Kharadi, Pune", ClientStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/clients/1"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        ClientRequest request = new ClientRequest("", "Rahul Deshmukh", "9876543210", null, ClientStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void createRejectsInvalidPhone() throws Exception {
        ClientRequest request = new ClientRequest("Pune Metro Rail", "Vijay Kulkarni", "abc", null, ClientStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAllowsOmittedAddress() throws Exception {
        when(clientService.create(any())).thenReturn(sample());

        ClientRequest request = new ClientRequest("SmartBatch Solutions", "Rahul Deshmukh", "9876543210",
                null, ClientStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
