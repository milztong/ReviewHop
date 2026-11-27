package dev.reviewhop.learning.application;

import dev.reviewhop.learning.domain.ReviewRating;
import java.time.Instant;
import java.util.UUID;

public record ReviewHistoryEntry(
        UUID reviewLogId,
        UUID reviewCardId,
        String title,
        ReviewRating rating,
        Instant reviewedAt,
        long durationMs,
        String note,
        Instant previousDueAt,
        Instant nextDueAt,
        String schedulerVersion) {
}
