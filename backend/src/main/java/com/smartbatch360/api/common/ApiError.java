package com.smartbatch360.api.common;

import java.time.Instant;
import java.util.List;

/**
 * Uniform, user-friendly error payload returned by every failed API call.
 * Never carries stack traces or raw exception text (docs/05_CRUD_SPECIFICATION.md).
 */
public class ApiError {

    private final Instant timestamp = Instant.now();
    private int status;
    private String error;
    private String message;
    private List<FieldError> fieldErrors;

    public ApiError() {
    }

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ApiError(int status, String error, String message, List<FieldError> fieldErrors) {
        this(status, error, message);
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public record FieldError(String field, String message) {
    }
}
