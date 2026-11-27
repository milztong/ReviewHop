package dev.reviewhop.integration.application;

import dev.reviewhop.integration.application.port.out.ProviderFailure;
import dev.reviewhop.integration.application.port.out.SyncWorkQueue;
import dev.reviewhop.integration.application.port.out.SyncTraceContext;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.DoubleSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProcessNextSyncWork {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessNextSyncWork.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(10);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(15);

    private final SyncWorkQueue queue;
    private final ProcessCodewarsSynchronization synchronization;
    private final Clock clock;
    private final DoubleSupplier jitter;
    private final MeterRegistry meterRegistry;
    private final SyncTraceContext traceContext;

    public ProcessNextSyncWork(
            SyncWorkQueue queue,
            ProcessCodewarsSynchronization synchronization,
            Clock clock,
            DoubleSupplier jitter,
            MeterRegistry meterRegistry,
            SyncTraceContext traceContext) {
        this.queue = queue;
        this.synchronization = synchronization;
        this.clock = clock;
        this.jitter = jitter;
        this.meterRegistry = meterRegistry;
        this.traceContext = traceContext;
    }

    public boolean execute(String workerId) {
        var work = queue.claimNext(workerId, clock.instant(), LEASE_DURATION);
        if (work.isEmpty()) {
            return false;
        }
        var workItem = work.orElseThrow();
        traceContext.continueTrace(
                workItem.traceparent(),
                "provider.sync.process",
                () -> {
                    process(workItem);
                    return null;
                });
        return true;
    }

    private void process(SyncWorkItem workItem) {
        var processingStartedAt = clock.instant();
        try {
            var result = synchronization.execute(
                    workItem,
                    () -> queue.renewLease(
                            workItem, clock.instant(), LEASE_DURATION));
            queue.complete(workItem, result, clock.instant());
            LOGGER.info(
                    "provider_sync_succeeded provider=CODEWARS syncJobId={} correlationId={} attempt={} importedCount={} existingCount={} pagesProcessed={}",
                    workItem.syncJobId(),
                    workItem.correlationId(),
                    workItem.attempt(),
                    result.importedCount(),
                    result.existingCount(),
                    result.pagesProcessed());
            recordOutcome("succeeded", processingStartedAt);
        } catch (SyncLeaseLostException failure) {
            LOGGER.warn(
                    "provider_sync_lease_lost syncJobId={} correlationId={} attempt={}",
                    workItem.syncJobId(),
                    workItem.correlationId(),
                    workItem.attempt());
            recordOutcome("lease_lost", processingStartedAt);
        } catch (SyncProcessingFailure failure) {
            var outcome = handleFailure(
                    workItem,
                    failure.providerFailure(),
                    failure.partialResult(),
                    clock.instant());
            recordOutcome(outcome, processingStartedAt);
        } catch (RuntimeException failure) {
            LOGGER.error(
                    "provider_sync_worker_error syncJobId={} correlationId={} attempt={} exceptionType={}",
                    workItem.syncJobId(),
                    workItem.correlationId(),
                    workItem.attempt(),
                    failure.getClass().getSimpleName());
            var outcome = handleFailure(
                    workItem,
                    new ProviderFailure(
                            "WORKER_EXECUTION_FAILED",
                            "Unexpected worker execution failure",
                            true,
                            null),
                    emptyResult(),
                    clock.instant());
            recordOutcome(outcome, processingStartedAt);
        }
    }

    private String handleFailure(
            SyncWorkItem workItem,
            ProviderFailure failure,
            SyncExecutionResult partialResult,
            Instant failedAt) {
        if (!failure.retryable()) {
            queue.failTerminal(workItem, failure, partialResult, failedAt);
            logFailure(workItem, failure, false);
            return "terminal_failed";
        } else if (workItem.attempt() >= MAX_ATTEMPTS) {
            queue.deadLetter(workItem, failure, partialResult, failedAt);
            logFailure(workItem, failure, true);
            return "dead_lettered";
        } else {
            queue.scheduleRetry(
                    workItem,
                    failure,
                    partialResult,
                    nextAttemptAt(workItem.attempt(), failure, failedAt));
            logFailure(workItem, failure, false);
            return "retry_scheduled";
        }
    }

    private void logFailure(
            SyncWorkItem workItem, ProviderFailure failure, boolean deadLettered) {
        LOGGER.warn(
                "provider_sync_failed provider=CODEWARS syncJobId={} correlationId={} failureCode={} retryable={} attempt={} deadLettered={}",
                workItem.syncJobId(),
                workItem.correlationId(),
                failure.code(),
                failure.retryable(),
                workItem.attempt(),
                deadLettered);
    }

    private Instant nextAttemptAt(
            int attempt, ProviderFailure failure, Instant failedAt) {
        var exponent = Math.min(attempt - 1, 8);
        var ceilingSeconds = Math.min(5L << exponent, MAX_BACKOFF.toSeconds());
        var jitterSeconds = Math.max(1L, (long) (ceilingSeconds * jitter.getAsDouble()));
        var backoff = Duration.ofSeconds(jitterSeconds);
        if (failure.retryAfter() != null && failure.retryAfter().compareTo(backoff) > 0) {
            backoff = failure.retryAfter();
        }
        return failedAt.plus(backoff);
    }

    private static SyncExecutionResult emptyResult() {
        return new SyncExecutionResult(null, 0, 0, 0);
    }

    private void recordOutcome(String outcome, Instant processingStartedAt) {
        var duration = Duration.between(processingStartedAt, clock.instant());
        if (duration.isNegative()) {
            duration = Duration.ZERO;
        }
        meterRegistry.counter("ankileet.worker.deliveries", "outcome", outcome)
                .increment();
        meterRegistry.timer("ankileet.worker.processing.duration", "outcome", outcome)
                .record(duration);
    }
}

