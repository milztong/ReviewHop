package dev.reviewhop.privacy.application;

public record DeletedAccountData(
        int appUsers,
        int providerConnections,
        int syncJobs,
        int outboxEvents,
        int learningItems,
        int reviewCards,
        int reviewLogs,
        int orphanedSourceItems) {
}
