package dev.reviewhop.integration.application;

public record SyncStart(SyncJobView job, boolean created) {
}
