package dev.reviewhop.integration.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.integration.application.port.out.IntegrationStore;
import java.util.UUID;

public final class GetSyncJob {

    private final IntegrationStore store;

    public GetSyncJob(IntegrationStore store) {
        this.store = store;
    }

    public SyncJobView execute(
            AppUserId userId, UUID connectionId, UUID syncJobId) {
        return store.findSyncJob(userId, connectionId, syncJobId)
                .orElseThrow(() -> new SyncJobNotFoundException(syncJobId));
    }
}

