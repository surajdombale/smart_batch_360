package com.smartbatch360.api.common;

/** Thrown when a uniqueness rule (e.g. vehicle number, driver license) is violated. Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
