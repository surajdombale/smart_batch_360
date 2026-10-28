package com.smartbatch360.api.batch;

import java.util.List;

/**
 * Manual/simulated batch state - no PLC integration (still intentionally
 * postponed). Driven entirely by the control endpoints
 * (start/pause/resume/stop/emergencyStop) and Add/Edit form - and, since
 * 28-Oct-2026, by the last planned cycle arriving, which completes the batch.
 */
public enum BatchStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    STOPPED,
    COMPLETED;

    /** Still being made. The plant runs one batch like this at a time. */
    public static final List<BatchStatus> RUNNING = List.of(PENDING, IN_PROGRESS, PAUSED);
}
