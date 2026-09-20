package com.smartbatch360.api.common;

import com.smartbatch360.api.client.ClientController;
import com.smartbatch360.api.client.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

    /**
     * A value too large for its column reaches Spring as the same exception
     * type as a duplicate key, so both were reported as 409 "conflicts with an
     * existing record". For an over-long quantity that is simply untrue and
     * sends the operator looking for a clash that does not exist. SQLState
     * class 22 is a data exception, which is the caller's input being wrong.
     */
    @Test
    void valueTooLargeForItsColumnIsBadRequestNotConflict() throws Exception {
        given(clientService.create(any())).willThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("Out of range value for column 'quantity'", "22003")));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content("{\"name\":\"X\",\"contactPerson\":\"Y\",\"phone\":\"9000000000\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("too large")));
    }

    /** A genuine clash is still a conflict - the split must not swallow those. */
    @Test
    void aDuplicateKeyIsStillAConflict() throws Exception {
        given(clientService.create(any())).willThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("Duplicate entry 'X' for key 'client.name'", "23000")));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content("{\"name\":\"X\",\"contactPerson\":\"Y\",\"phone\":\"9000000000\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());
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
