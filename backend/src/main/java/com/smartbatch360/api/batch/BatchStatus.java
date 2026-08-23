package com.smartbatch360.api.batch;

/**
 * Manual/simulated batch state - no PLC integration (still intentionally
 * postponed). Driven entirely by the control endpoints
 * (start/pause/resume/stop/emergencyStop) and Add/Edit form.
 */
public enum BatchStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    STOPPED,
    COMPLETED
}
