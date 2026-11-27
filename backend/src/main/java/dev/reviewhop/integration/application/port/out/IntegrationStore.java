package dev.reviewhop.integration.application.port.out;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.integration.application.ProviderConnectionView;
import dev.reviewhop.integration.application.SyncJobView;
import dev.reviewhop.integration.application.SyncStart;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationStore {

    ProviderConnectionView findOrCreateCodewarsConnection(
            AppUserId userId, String externalUserId, Instant now);

    List<ProviderConnectionView> findConnections(AppUserId userId);

    Optional<ProviderConnectionView> findConnection(AppUserId userId, UUID connectionId);

    SyncStart requestSync(
            AppUserId userId,
            UUID connectionId,
            UUID idempotencyKey,
            UUID correlationId,
            String traceparent,
            Instant requestedAt);

    Optional<SyncJobView> findSyncJob(
            AppUserId userId, UUID connectionId, UUID syncJobId);
}
