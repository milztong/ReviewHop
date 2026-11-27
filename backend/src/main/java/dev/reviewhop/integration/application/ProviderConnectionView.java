package dev.reviewhop.integration.application;

import dev.reviewhop.integration.domain.ConnectionMode;
import dev.reviewhop.integration.domain.ConnectionStatus;
import dev.reviewhop.integration.domain.Provider;
import dev.reviewhop.integration.domain.VerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record ProviderConnectionView(
        UUID id,
        Provider provider,
        ConnectionMode mode,
        ConnectionStatus status,
        VerificationStatus verificationStatus,
        String externalUserId,
        Instant lastSyncedAt) {
}
