package dev.reviewhop.integration.application;

import dev.reviewhop.integration.application.port.out.ProviderConnector;
import dev.reviewhop.integration.application.port.out.ProviderFailure;
import dev.reviewhop.integration.domain.CompletedChallenge;
import dev.reviewhop.integration.domain.Provider;
import dev.reviewhop.learning.api.CompletedSourceItem;
import dev.reviewhop.learning.api.LearningItemImporter;
import java.time.Clock;
import java.util.Comparator;

public final class ProcessCodewarsSynchronization {

    private static final int MAX_PAGES_PER_SYNC = 1_000;

    private final ProviderConnector connector;
    private final LearningItemImporter importer;
    private final Clock clock;

    public ProcessCodewarsSynchronization(
            ProviderConnector connector,
            LearningItemImporter importer,
            Clock clock) {
        this.connector = connector;
        this.importer = importer;
        this.clock = clock;
    }

    public SyncExecutionResult execute(SyncWorkItem workItem, Runnable renewLease) {
        var importedCount = 0;
        var existingCount = 0;
        var pagesProcessed = 0;
        CompletedChallenge highWaterMark = null;
        var pageNumber = 0;
        var totalPages = 1;
        try {
            while (pageNumber < totalPages) {
                if (pageNumber >= MAX_PAGES_PER_SYNC) {
                    throw new ProviderFailure(
                            "PAGE_LIMIT_EXCEEDED",
                            "Codewars returned more pages than the configured safety limit",
                            false,
                            null);
                }
                var page = connector.fetchCompletedChallenges(
                        workItem.externalUserId(), pageNumber);
                totalPages = Math.max(totalPages, page.totalPages());
                var items = page.challenges().stream()
                        .map(ProcessCodewarsSynchronization::toLearningItem)
                        .toList();
                var result = importer.importCompleted(
                        workItem.userId(), Provider.CODEWARS.name(), items, clock.instant());
                importedCount += result.importedCount();
                existingCount += result.existingCount();
                pagesProcessed++;
                var pageMaximum = page.challenges().stream()
                        .max(Comparator.comparing(CompletedChallenge::completedAt)
                                .thenComparing(CompletedChallenge::externalId))
                        .orElse(null);
                if (isNewer(pageMaximum, highWaterMark)) {
                    highWaterMark = pageMaximum;
                }
                renewLease.run();
                pageNumber++;
            }
        } catch (ProviderFailure failure) {
            throw new SyncProcessingFailure(
                    failure,
                    new SyncExecutionResult(
                            null, importedCount, existingCount, pagesProcessed));
        }
        var cursor = highWaterMark == null
                ? null
                : highWaterMark.completedAt() + "|" + highWaterMark.externalId();
        return new SyncExecutionResult(
                cursor, importedCount, existingCount, pagesProcessed);
    }

    private static boolean isNewer(
            CompletedChallenge candidate, CompletedChallenge current) {
        return candidate != null
                && (current == null
                || candidate.completedAt().isAfter(current.completedAt())
                || candidate.completedAt().equals(current.completedAt())
                && candidate.externalId().compareTo(current.externalId()) > 0);
    }

    private static CompletedSourceItem toLearningItem(CompletedChallenge challenge) {
        return new CompletedSourceItem(
                challenge.externalId(),
                challenge.title(),
                challenge.sourceUrl(),
                null,
                java.util.List.of(),
                challenge.completedAt());
    }
}

