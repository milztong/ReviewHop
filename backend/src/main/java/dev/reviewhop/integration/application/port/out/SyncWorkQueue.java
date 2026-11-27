package dev.reviewhop.integration.application.port.out;

import dev.reviewhop.integration.application.SyncExecutionResult;
import dev.reviewhop.integration.application.SyncJobView;
import dev.reviewhop.integration.application.SyncWorkItem;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SyncWorkQueue {

    Optional<SyncWorkItem> claimNext(String workerId, Instant now, Duration leaseDuration);

    void renewLease(SyncWorkItem workItem, Instant now, Duration leaseDuration);

    void complete(SyncWorkItem workItem, SyncExecutionResult result, Instant completedAt);

    void failTerminal(
            SyncWorkItem workItem,
            ProviderFailure failure,
            SyncExecutionResult partialResult,
            Instant completedAt);

    void scheduleRetry(
            SyncWorkItem workItem,
            ProviderFailure failure,
            SyncExecutionResult partialResult,
            Instant availableAt);

    void deadLetter(
            SyncWorkItem workItem,
            ProviderFailure failure,
            SyncExecutionResult partialResult,
            Instant completedAt);

    SyncJobView retryDeadLetter(UUID syncJobId, Instant availableAt);
}
