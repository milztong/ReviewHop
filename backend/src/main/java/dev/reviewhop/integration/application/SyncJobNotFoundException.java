package dev.reviewhop.integration.application;

import java.util.UUID;

public final class SyncJobNotFoundException extends RuntimeException {

    public SyncJobNotFoundException(UUID syncJobId) {
        super("SyncJob not found: " + syncJobId);
    }
}

