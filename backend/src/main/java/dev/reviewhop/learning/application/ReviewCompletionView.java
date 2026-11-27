package dev.reviewhop.learning.application;

import dev.reviewhop.learning.domain.ReviewRating;
import java.time.Instant;
import java.util.UUID;

public record ReviewCompletionView(
        UUID reviewLogId,
        UUID reviewCardId,
        ReviewRating rating,
        Instant reviewedAt,
        long durationMs,
        Instant nextDueAt,
        String schedulerVersion) {
}
