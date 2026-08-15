package com.smartbatch360.desktop.api;

import java.util.List;

/** Mirrors the backend's ApiError shape so field-level validation messages can be shown next to inputs. */
public record ApiErrorDto(
        int status,
        String error,
        String message,
        List<FieldErrorDto> fieldErrors
) {
    public record FieldErrorDto(String field, String message) {
    }
}
