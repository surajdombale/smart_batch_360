package com.smartbatch360.api.common;

/**
 * Thrown when an operation cannot proceed because of a business/referential rule
 * (e.g. deleting a customer that still has sites). Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
