package dev.reviewhop.learning.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.application.port.out.LearningStore;
import dev.reviewhop.learning.application.port.out.SchedulingPolicy;
import dev.reviewhop.learning.domain.DesiredRetention;
import dev.reviewhop.learning.domain.ReviewRating;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class CompleteReview {

    private static final Duration MAX_REVIEW_DURATION = Duration.ofHours(24);

    private final LearningStore learningStore;
    private final SchedulingPolicy schedulingPolicy;
    private final Clock clock;

    public CompleteReview(
            LearningStore learningStore,
            SchedulingPolicy schedulingPolicy,
            Clock clock) {
        this.learningStore = learningStore;
        this.schedulingPolicy = schedulingPolicy;
        this.clock = clock;
    }

    public ReviewCompletionView execute(
            AppUserId userId,
            UUID reviewCardId,
            UUID idempotencyKey,
            DesiredRetention desiredRetention,
            ReviewRating rating,
            Instant startedAt,
            String note) {
        var previous = learningStore.findCompletion(userId, idempotencyKey);
        if (previous.isPresent()) {
            return previous.get();
        }

        var reviewedAt = clock.instant();
        var duration = Duration.between(startedAt, reviewedAt);
        if (duration.isNegative() || duration.compareTo(MAX_REVIEW_DURATION) > 0) {
            throw new IllegalArgumentException("startedAt must be within the previous 24 hours");
        }

        var card = learningStore.findReviewCard(userId, reviewCardId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCard not found"));
        var schedule = schedulingPolicy.schedule(
                card.scheduling(), desiredRetention, rating, reviewedAt);
        return learningStore.completeReview(
                userId,
                card,
                idempotencyKey,
                desiredRetention,
                rating,
                reviewedAt,
                duration.toMillis(),
                normalize(note),
                schedule);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
