package com.smartbatch360.api.common;

import com.smartbatch360.api.client.ClientController;
import com.smartbatch360.api.client.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for malformed-request handling.
 *
 * All of these used to fall through to the catch-all handler and come back as
 * 500 Internal Server Error - i.e. the API blamed itself for what were plainly
 * bad requests, and the desktop client showed "Something went wrong" instead of
 * anything actionable. Found while stabilising the build 2026-08-27.
 */
@WebMvcTest(ClientController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @Test
    void unknownEnumConstantIsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content("{\"name\":\"X\",\"contactPerson\":\"Y\",\"phone\":\"9000000000\",\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void malformedJsonIsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content("{\"name\": \"unclosed"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongFieldTypeIsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content("{\"name\":{\"nested\":true},\"contactPerson\":\"Y\",\"phone\":\"9\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedMethodIsMethodNotAllowedNotServerError() throws Exception {
        mockMvc.perform(delete("/api/v1/clients"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void errorResponseNeverLeaksTheRawException() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content("{\"name\":\"X\",\"contactPerson\":\"Y\",\"phone\":\"9\",\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));
    }
}
