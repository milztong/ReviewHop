package dev.reviewhop.privacy.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountDataExport(
        int formatVersion,
        Instant exportedAt,
        Profile profile,
        List<ProviderConnection> providerConnections,
        List<LearningItem> learningItems,
        List<ReviewCard> reviewCards,
        List<ReviewLog> reviewLogs,
        List<SyncJob> syncJobs) {

    public record Profile(String timezone, double desiredRetention, Instant createdAt) {
    }

    public record ProviderConnection(
            UUID id,
            String provider,
            String mode,
            String status,
            String verificationStatus,
            String externalUserId,
            Instant lastSyncedAt,
            Instant createdAt) {
    }

    public record LearningItem(
            UUID id,
            String provider,
            String externalId,
            String title,
            String sourceUrl,
            String difficulty,
            List<String> tags,
            String notes,
            String recallPrompt,
            Instant firstCompletedAt,
            String status,
            Instant createdAt) {
    }

    public record ReviewCard(
            UUID id,
            UUID learningItemId,
            String mode,
            String state,
            Instant dueAt,
            SchedulingSnapshot scheduling) {
    }

    public record ReviewLog(
            UUID id,
            UUID reviewCardId,
            String rating,
            Instant reviewedAt,
            long durationMs,
            String note,
            String schedulerVersion,
            Double desiredRetention,
            SchedulingSnapshot previousScheduling,
            SchedulingSnapshot nextScheduling) {
    }

    public record SchedulingSnapshot(
            String state,
            Integer step,
            Double stability,
            Double difficulty,
            Instant lastReviewedAt,
            Instant dueAt) {
    }

    public record SyncJob(
            UUID id,
            UUID connectionId,
            String status,
            int importedCount,
            int existingCount,
            int pagesProcessed,
            String failureCode,
            Instant startedAt,
            Instant completedAt) {
    }
}
