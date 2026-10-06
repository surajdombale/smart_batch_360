package com.smartbatch360.api.header.dto;

import com.smartbatch360.api.header.HeaderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Fields as clarified directly by the user (no source-document mockup exists
 * for Header): Company Name, Plant/Branch Name, Address, City, Pin code,
 * Phone, Email, GSTIN/Tax ID, Supervisor, Mix time, Discharge time, Status.
 * Company Name and Plant Name are required so each header has an identifying
 * label in the list; the rest are optional since a letterhead is often filled
 * in incrementally.
 *
 * The last four arrived on 04-Oct-2026 with the batch report templates, which
 * print them on every report. Mix and discharge times are DURATIONS in seconds,
 * not clock times. The logo is not here - it is uploaded separately, since a
 * letterhead image has no business in a JSON form submission.
 */
public record HeaderRequest(

        @NotBlank(message = "Company name is required.")
        @Size(max = 150, message = "Company name must be at most 150 characters.")
        String companyName,

        @NotBlank(message = "Plant/branch name is required.")
        @Size(max = 150, message = "Plant/branch name must be at most 150 characters.")
        String plantName,

        @Size(max = 255, message = "Address must be at most 255 characters.")
        String address,

        @Size(max = 100, message = "City must be at most 100 characters.")
        String city,

        @Pattern(regexp = "^$|^[0-9]{6}$", message = "Pin code must be 6 digits.")
        String pinCode,

        @Size(max = 20, message = "Phone must be at most 20 characters.")
        String phone,

        @Email(message = "Email must be a valid email address.")
        @Size(max = 150, message = "Email must be at most 150 characters.")
        String email,

        @Size(max = 20, message = "GSTIN/Tax ID must be at most 20 characters.")
        String gstin,

        @Size(max = 150, message = "Supervisor name must be at most 150 characters.")
        String supervisorName,

        @Min(value = 0, message = "Mix time cannot be negative.")
        @Max(value = 3600, message = "Mix time must be at most 3600 seconds.")
        Integer mixTimeSeconds,

        @Min(value = 0, message = "Discharge time cannot be negative.")
        @Max(value = 3600, message = "Discharge time must be at most 3600 seconds.")
        Integer dischargeTimeSeconds,

        @DecimalMin(value = "0.1", message = "Mixer capacity must be at least 0.1 m3.")
        @DecimalMax(value = "10.0", message = "Mixer capacity must be at most 10 m3.")
        @Digits(integer = 2, fraction = 2, message = "Mixer capacity may have at most 2 decimal places.")
        BigDecimal mixerCapacityM3,

        @NotNull(message = "Status is required.")
        HeaderStatus status
) {
}
