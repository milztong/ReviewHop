package dev.reviewhop.learning.adapter.out.persistence;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.application.DueReviewView;
import dev.reviewhop.learning.application.LearningItemView;
import dev.reviewhop.learning.application.OwnedReviewCard;
import dev.reviewhop.learning.application.ReviewCompletionView;
import dev.reviewhop.learning.application.ReviewHistoryEntry;
import dev.reviewhop.learning.application.port.out.LearningStore;
import dev.reviewhop.learning.api.CompletedSourceItem;
import dev.reviewhop.learning.api.LearningImportResult;
import dev.reviewhop.learning.domain.DesiredRetention;
import dev.reviewhop.learning.domain.ReviewMode;
import dev.reviewhop.learning.domain.ReviewRating;
import dev.reviewhop.learning.domain.ScheduleResult;
import dev.reviewhop.learning.domain.SchedulerState;
import dev.reviewhop.learning.domain.SchedulingSnapshot;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaLearningStore implements LearningStore {

    private final SourceItemJpaRepository sourceItems;
    private final LearningItemJpaRepository learningItems;
    private final ReviewCardJpaRepository reviewCards;
    private final ReviewLogJpaRepository reviewLogs;
    private final EntityManager entityManager;

    JpaLearningStore(
            SourceItemJpaRepository sourceItems,
            LearningItemJpaRepository learningItems,
            ReviewCardJpaRepository reviewCards,
            ReviewLogJpaRepository reviewLogs,
            EntityManager entityManager) {
        this.sourceItems = sourceItems;
        this.learningItems = learningItems;
        this.reviewCards = reviewCards;
        this.reviewLogs = reviewLogs;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public LearningImportResult importCompletedSourceItems(
            AppUserId userId,
            String provider,
            List<CompletedSourceItem> items,
            Instant importedAt) {
        lock(userId.value() + ":" + provider + ":provider-import");
        var importedCount = 0;
        var existingCount = 0;
        for (var item : items) {
            var sourceItem = sourceItems.findByProviderAndExternalId(provider, item.externalId())
                    .orElseGet(() -> sourceItems.save(new SourceItemEntity(
                            UUID.randomUUID(),
                            provider,
                            item.externalId(),
                            item.title(),
                            item.sourceUrl().toString(),
                            item.difficulty(),
                            item.tags(),
                            importedAt)));
            if (learningItems.existsByUserIdAndSourceItemId(userId.value(), sourceItem.id())) {
                existingCount++;
                continue;
            }
            var learningItem = learningItems.save(new LearningItemEntity(
                    UUID.randomUUID(),
                    userId.value(),
                    sourceItem,
                    null,
                    null,
                    item.completedAt(),
                    importedAt));
            reviewCards.save(new ReviewCardEntity(
                    UUID.randomUUID(), learningItem, item.completedAt()));
            importedCount++;
        }
        return new LearningImportResult(importedCount, existingCount);
    }

    @Override
    @Transactional
    public LearningItemView createManualLearningItem(
            AppUserId userId,
            UUID idempotencyKey,
            String title,
            URI sourceUrl,
            String difficulty,
            List<String> tags,
            String notes,
            String recallPrompt,
            Instant firstCompletedAt,
            Instant createdAt) {
        var externalId = userId.value() + ":" + idempotencyKey;
        lock(userId.value() + ":" + idempotencyKey);
        var existing = learningItems.findManualByUserAndExternalId(userId.value(), externalId);
        if (existing.isPresent()) {
            var card = reviewCards.findByLearningItemId(existing.get().id()).orElseThrow();
            return toLearningItemView(existing.get(), card);
        }

        var sourceItem = sourceItems.save(new SourceItemEntity(
                UUID.randomUUID(),
                externalId,
                title,
                sourceUrl.toString(),
                difficulty,
                tags,
                createdAt));
        var learningItem = learningItems.save(new LearningItemEntity(
                UUID.randomUUID(),
                userId.value(),
                sourceItem,
                notes,
                recallPrompt,
                firstCompletedAt,
                createdAt));
        var reviewCard = reviewCards.save(new ReviewCardEntity(
                UUID.randomUUID(), learningItem, firstCompletedAt));
        return toLearningItemView(learningItem, reviewCard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DueReviewView> findDueReviews(AppUserId userId, Instant dueAtOrBefore) {
        return reviewCards.findDue(userId.value(), dueAtOrBefore).stream()
                .map(this::toDueReviewView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnedReviewCard> findReviewCard(AppUserId userId, UUID reviewCardId) {
        return reviewCards.findOwned(userId.value(), reviewCardId)
                .map(card -> new OwnedReviewCard(card.id(), toSchedulingSnapshot(card), card.version()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewCompletionView> findCompletion(
            AppUserId userId,
            UUID idempotencyKey) {
        return reviewLogs.findByUserIdAndIdempotencyKey(userId.value(), idempotencyKey)
                .map(this::toCompletionView);
    }

    @Override
    @Transactional
    public ReviewCompletionView completeReview(
            AppUserId userId,
            OwnedReviewCard card,
            UUID idempotencyKey,
            DesiredRetention desiredRetention,
            ReviewRating rating,
            Instant reviewedAt,
            long durationMs,
            String note,
            ScheduleResult schedule) {
        lock(userId.value() + ":" + idempotencyKey);
        var existingCompletion = reviewLogs.findByUserIdAndIdempotencyKey(
                userId.value(), idempotencyKey);
        if (existingCompletion.isPresent()) {
            return toCompletionView(existingCompletion.get());
        }
        var entity = reviewCards.findOwned(userId.value(), card.id())
                .orElseThrow(() -> new OptimisticLockingFailureException("ReviewCard changed"));
        if (entity.version() != card.version()) {
            throw new OptimisticLockingFailureException("ReviewCard changed");
        }

        var previous = toSchedulingSnapshot(entity);
        entity.reschedule(schedule.next());
        var log = reviewLogs.save(new ReviewLogEntity(
                UUID.randomUUID(),
                entity,
                userId.value(),
                idempotencyKey,
                rating.name(),
                reviewedAt,
                durationMs,
                note,
                previous.dueAt(),
                schedule.next().dueAt(),
                schedule.schedulerVersion(),
                desiredRetention.value(),
                previous,
                schedule.next()));
        reviewCards.flush();
        return toCompletionView(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<ReviewHistoryEntry>> findReviewHistory(
            AppUserId userId,
            UUID reviewCardId) {
        if (reviewCards.findOwned(userId.value(), reviewCardId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(reviewLogs.findByReviewCardIdOrderByReviewedAtDesc(reviewCardId).stream()
                .map(this::toHistoryEntry)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHistoryEntry> findRecentReviewHistory(AppUserId userId, int limit) {
        return reviewLogs.findByUserIdOrderByReviewedAtDesc(
                        userId.value(), PageRequest.of(0, limit))
                .stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    private LearningItemView toLearningItemView(
            LearningItemEntity learningItem,
            ReviewCardEntity card) {
        var source = learningItem.sourceItem();
        return new LearningItemView(
                learningItem.id(),
                card.id(),
                source.title(),
                source.sourceUrl(),
                source.difficulty(),
                source.tags(),
                learningItem.notes(),
                learningItem.recallPrompt(),
                ReviewMode.valueOf(card.mode()),
                card.dueAt());
    }

    private DueReviewView toDueReviewView(ReviewCardEntity card) {
        var learningItem = card.learningItem();
        var source = learningItem.sourceItem();
        return new DueReviewView(
                card.id(),
                source.title(),
                source.sourceUrl(),
                source.difficulty(),
                learningItem.recallPrompt(),
                learningItem.notes(),
                ReviewMode.valueOf(card.mode()),
                card.dueAt());
    }

    private ReviewCompletionView toCompletionView(ReviewLogEntity log) {
        return new ReviewCompletionView(
                log.id(),
                log.reviewCard().id(),
                ReviewRating.valueOf(log.rating()),
                log.reviewedAt(),
                log.durationMs(),
                log.nextDueAt(),
                log.schedulerVersion());
    }

    private SchedulingSnapshot toSchedulingSnapshot(ReviewCardEntity card) {
        return new SchedulingSnapshot(
                card.dueAt(),
                SchedulerState.valueOf(card.schedulerState()),
                card.schedulerStep(),
                card.stability(),
                card.schedulerDifficulty(),
                card.lastReviewedAt());
    }

    private ReviewHistoryEntry toHistoryEntry(ReviewLogEntity log) {
        return new ReviewHistoryEntry(
                log.id(),
                log.reviewCard().id(),
                log.reviewCard().learningItem().sourceItem().title(),
                ReviewRating.valueOf(log.rating()),
                log.reviewedAt(),
                log.durationMs(),
                log.note(),
                log.previousDueAt(),
                log.nextDueAt(),
                log.schedulerVersion());
    }

    private void lock(String lockKey) {
        entityManager.createNativeQuery(
                        """
                        SELECT 1
                        FROM (SELECT pg_advisory_xact_lock(
                            hashtextextended(CAST(:lockKey AS text), 0))) AS lock_result
                        """,
                        Integer.class)
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
