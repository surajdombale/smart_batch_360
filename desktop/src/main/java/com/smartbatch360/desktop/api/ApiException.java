package com.smartbatch360.desktop.api;

import java.util.List;

/**
 * User-friendly API failure. Never wraps a raw stack trace message for display -
 * callers show {@link #getMessage()} directly in the UI (docs/05_CRUD_SPECIFICATION.md).
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final List<ApiErrorDto.FieldErrorDto> fieldErrors;

    public ApiException(int statusCode, String message, List<ApiErrorDto.FieldErrorDto> fieldErrors) {
        super(message);
        this.statusCode = statusCode;
        this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
    }

    public static ApiException network(Throwable cause) {
        ApiException ex = new ApiException(0, "Could not reach the SmartBatch360 server. Check your connection and try again.", List.of());
        ex.initCause(cause);
        return ex;
    }

    public int statusCode() {
        return statusCode;
    }

    public List<ApiErrorDto.FieldErrorDto> fieldErrors() {
        return fieldErrors;
    }

    public boolean isNotFound() {
        return statusCode == 404;
    }

    public boolean isValidation() {
        return statusCode == 400;
    }

    public boolean isConflict() {
        return statusCode == 409;
    }
}
