package com.smartbatch360.api.common;

/** Thrown when a requested resource does not exist. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException forId(String resource, Object id) {
        return new NotFoundException(resource + " with id " + id + " was not found.");
    }
}
