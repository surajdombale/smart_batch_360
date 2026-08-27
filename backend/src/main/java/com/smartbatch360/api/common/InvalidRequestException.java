package com.smartbatch360.api.common;

/**
 * A request that is well-formed but breaks a business rule the bean-validation
 * annotations can't express on their own - e.g. "density is required, but only
 * for weight-based units". Maps to 400, unlike ConflictException's 409.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
