package dev.reviewhop.learning.application.port.out;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.application.DueReviewView;
import dev.reviewhop.learning.application.LearningItemView;
import dev.reviewhop.learning.application.OwnedReviewCard;
import dev.reviewhop.learning.application.ReviewCompletionView;
import dev.reviewhop.learning.application.ReviewHistoryEntry;
import dev.reviewhop.learning.api.CompletedSourceItem;
import dev.reviewhop.learning.api.LearningImportResult;
import dev.reviewhop.learning.domain.DesiredRetention;
import dev.reviewhop.learning.domain.ReviewRating;
import dev.reviewhop.learning.domain.ScheduleResult;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningStore {

    LearningImportResult importCompletedSourceItems(
            AppUserId userId,
            String provider,
            List<CompletedSourceItem> items,
            Instant importedAt);

    LearningItemView createManualLearningItem(
            AppUserId userId,
            UUID idempotencyKey,
            String title,
            URI sourceUrl,
            String difficulty,
            List<String> tags,
            String notes,
            String recallPrompt,
            Instant firstCompletedAt,
            Instant createdAt);

    List<DueReviewView> findDueReviews(AppUserId userId, Instant dueAtOrBefore);

    Optional<OwnedReviewCard> findReviewCard(AppUserId userId, UUID reviewCardId);

    Optional<ReviewCompletionView> findCompletion(
            AppUserId userId,
            UUID idempotencyKey);

    ReviewCompletionView completeReview(
            AppUserId userId,
            OwnedReviewCard card,
            UUID idempotencyKey,
            DesiredRetention desiredRetention,
            ReviewRating rating,
            Instant reviewedAt,
            long durationMs,
            String note,
            ScheduleResult schedule);

    Optional<List<ReviewHistoryEntry>> findReviewHistory(
            AppUserId userId,
            UUID reviewCardId);

    List<ReviewHistoryEntry> findRecentReviewHistory(AppUserId userId, int limit);
}
