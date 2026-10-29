package com.smartbatch360.desktop.order;

/** Mirrors the backend's OrderStatus. */
public enum OrderStatus {
    UNFULFILLED,
    IN_PROGRESS,
    FULFILLED,
    CANCELLED;

    /** Can still be produced against. The backend refuses a fulfilled or cancelled order. */
    public boolean isOpen() {
        return this == UNFULFILLED || this == IN_PROGRESS;
    }
}
