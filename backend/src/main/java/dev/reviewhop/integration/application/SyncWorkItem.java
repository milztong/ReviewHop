package dev.reviewhop.integration.application;

import dev.reviewhop.identity.api.AppUserId;
import java.util.UUID;

public record SyncWorkItem(
        UUID eventId,
        UUID leaseToken,
        UUID syncJobId,
        AppUserId userId,
        UUID connectionId,
        String externalUserId,
        int attempt,
        UUID correlationId,
        String traceparent) {
}

