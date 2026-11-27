package dev.reviewhop.integration.application;

import dev.reviewhop.integration.domain.SyncJobStatus;
import java.time.Instant;
import java.util.UUID;

public record SyncJobView(
        UUID id,
        UUID connectionId,
        SyncJobStatus status,
        int importedCount,
        int existingCount,
        int pagesProcessed,
        String failureCode,
        String failureDetail,
        Long retryAfterSeconds,
        Instant startedAt,
        Instant completedAt) {
}
