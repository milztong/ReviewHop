package dev.reviewhop.integration.domain;

public enum SyncJobStatus {
    REQUESTED,
    RUNNING,
    RETRY_SCHEDULED,
    SUCCEEDED,
    RETRYABLE_FAILED,
    TERMINAL_FAILED,
    DEAD_LETTERED
}
