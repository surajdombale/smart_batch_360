package com.smartbatch360.api.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbatch360.api.customer.dto.CustomerRequest;
import com.smartbatch360.api.customer.dto.CustomerResponse;
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

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    private CustomerResponse sample() {
        return new CustomerResponse(1L, "SmartBatch Solutions", "Rahul Deshmukh", "9876543210",
                CustomerStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    void listReturnsCustomers() throws Exception {
        when(customerService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("SmartBatch Solutions"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(customerService.create(any())).thenReturn(sample());

        CustomerRequest request = new CustomerRequest("SmartBatch Solutions", "Rahul Deshmukh", "9876543210", CustomerStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/customers/1"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        CustomerRequest request = new CustomerRequest("", "Rahul Deshmukh", "9876543210", CustomerStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void createRejectsInvalidPhone() throws Exception {
        CustomerRequest request = new CustomerRequest("Pune Metro Rail", "Vijay Kulkarni", "abc", CustomerStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
