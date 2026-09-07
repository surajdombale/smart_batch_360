package com.smartbatch360.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors the backend's ApiError shape so field-level validation messages can be shown next to inputs.
 *
 * Unknown properties are ignored, and that is not incidental. ApiError also
 * carries a timestamp, which this record does not declare, and Jackson fails
 * on unknown properties by default - so EVERY error body failed to parse and
 * ApiClient fell back to its generic per-status text. The backend's actual
 * message ("Material 'X' is measured in KG but has no density set...") was
 * built, sent, and then thrown away in favour of "The submitted data is
 * invalid." on every screen in the app.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiErrorDto(
        int status,
        String error,
        String message,
        List<FieldErrorDto> fieldErrors
) {
    public record FieldErrorDto(String field, String message) {
    }
}
