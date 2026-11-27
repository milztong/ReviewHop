package dev.reviewhop.integration.adapter.out.persistence;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.integration.application.SyncExecutionResult;
import dev.reviewhop.integration.application.SyncJobView;
import dev.reviewhop.integration.application.SyncJobNotFoundException;
import dev.reviewhop.integration.application.SyncLeaseLostException;
import dev.reviewhop.integration.application.SyncRequestConflictException;
import dev.reviewhop.integration.application.SyncWorkItem;
import dev.reviewhop.integration.application.port.out.ProviderFailure;
import dev.reviewhop.integration.application.port.out.SyncWorkQueue;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaSyncWorkQueue implements SyncWorkQueue {

    private static final Set<String> ACTIVE_SYNC_STATUSES = Set.of(
            "REQUESTED", "RUNNING", "RETRY_SCHEDULED");

    private static final String CLAIM_SQL = """
            WITH candidate AS (
                SELECT id
                FROM sync_outbox_event
                WHERE (status = 'PENDING' AND available_at <= ?)
                   OR (status = 'PROCESSING' AND lease_expires_at <= ?)
                ORDER BY available_at, created_at
                FOR UPDATE SKIP LOCKED
                LIMIT 1
            )
            UPDATE sync_outbox_event event
            SET status = 'PROCESSING',
                attempt_count = attempt_count + 1,
                lease_token = ?,
                lease_owner = ?,
                lease_expires_at = ?
            FROM candidate
            WHERE event.id = candidate.id
            RETURNING event.id,
                      event.aggregate_id,
                      event.attempt_count,
                      event.correlation_id,
                      event.traceparent
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SyncJobJpaRepository syncJobs;
    private final SyncOutboxEventJpaRepository outboxEvents;

    JpaSyncWorkQueue(
            JdbcTemplate jdbcTemplate,
            SyncJobJpaRepository syncJobs,
            SyncOutboxEventJpaRepository outboxEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.syncJobs = syncJobs;
        this.outboxEvents = outboxEvents;
    }

    @Override
    @Transactional
    public Optional<SyncWorkItem> claimNext(
            String workerId, Instant now, Duration leaseDuration) {
        var leaseToken = UUID.randomUUID();
        var leaseExpiresAt = now.plus(leaseDuration);
        var claims = jdbcTemplate.query(
                CLAIM_SQL,
                (resultSet, rowNumber) -> new ClaimedEvent(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("aggregate_id", UUID.class),
                        resultSet.getInt("attempt_count"),
                        resultSet.getObject("correlation_id", UUID.class),
                        resultSet.getString("traceparent")),
                Timestamp.from(now),
                Timestamp.from(now),
                leaseToken,
                workerId,
                Timestamp.from(leaseExpiresAt));
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        var claim = claims.getFirst();
        var job = syncJobs.findById(claim.syncJobId()).orElseThrow();
        job.markRunning();
        return Optional.of(new SyncWorkItem(
                claim.eventId(),
                leaseToken,
                job.id(),
                new AppUserId(job.userId()),
                job.connection().id(),
                job.connection().externalUserId(),
                claim.attempt(),
                claim.correlationId(),
                claim.traceparent()));
    }

    @Override
    @Transactional
    public void renewLease(
            SyncWorkItem workItem, Instant now, Duration leaseDuration) {
        var updated = jdbcTemplate.update(
                """
                UPDATE sync_outbox_event
                SET lease_expires_at = ?
                WHERE id = ? AND status = 'PROCESSING' AND lease_token = ?
                """,
                Timestamp.from(now.plus(leaseDuration)),
                workItem.eventId(),
                workItem.leaseToken());
        if (updated != 1) {
            throw new SyncLeaseLostException(workItem.eventId());
        }
    }

    @Override
    @Transactional
    public void complete(
            SyncWorkItem workItem, SyncExecutionResult result, Instant completedAt) {
        var event = leasedEvent(workItem);
        var job = event.syncJob();
        job.succeed(
                result.resultCursor(),
                result.importedCount(),
                result.existingCount(),
                result.pagesProcessed(),
                completedAt);
        job.connection().synchronizedAt(result.resultCursor(), completedAt);
        event.complete(workItem.leaseToken(), completedAt);
    }

    @Override
    @Transactional
    public void failTerminal(
            SyncWorkItem workItem,
            ProviderFailure failure,
            SyncExecutionResult partialResult,
            Instant completedAt) {
        var event = leasedEvent(workItem);
        event.syncJob().failTerminal(
                failure.code(),
                abbreviate(failure.getMessage(), 500),
                retryAfterSeconds(failure),
                partialResult.importedCount(),
                partialResult.existingCount(),
                partialResult.pagesProcessed(),
                completedAt);
        event.complete(workItem.leaseToken(), completedAt);
    }

    @Override
    @Transactional
    public void scheduleRetry(
            SyncWorkItem workItem,
            ProviderFailure failure,
            SyncExecutionResult partialResult,
            Instant availableAt) {
        var event = leasedEvent(workItem);
        event.syncJob().scheduleRetry(
                failure.code(),
                abbreviate(failure.getMessage(), 500),
                retryAfterSeconds(failure),
                partialResult.importedCount(),
                partialResult.existingCount(),
                partialResult.pagesProcessed());
        event.scheduleRetry(workItem.leaseToken(), failure.code(), availableAt);
    }

    @Override
    @Transactional
    public void deadLetter(
            SyncWorkItem workItem,
            ProviderFailure failure,
            SyncExecutionResult partialResult,
            Instant completedAt) {
        var event = leasedEvent(workItem);
        event.syncJob().deadLetter(
                failure.code(),
                abbreviate(failure.getMessage(), 500),
                retryAfterSeconds(failure),
                partialResult.importedCount(),
                partialResult.existingCount(),
                partialResult.pagesProcessed(),
                completedAt);
        event.deadLetter(workItem.leaseToken(), failure.code(), completedAt);
    }

    @Override
    @Transactional
    public SyncJobView retryDeadLetter(UUID syncJobId, Instant availableAt) {
        var event = outboxEvents.findBySyncJob_Id(syncJobId)
                .orElseThrow(() -> new SyncJobNotFoundException(syncJobId));
        if (!"DEAD_LETTERED".equals(event.syncJob().status())) {
            throw new SyncRequestConflictException(
                    "Only a dead-lettered synchronization can be retried");
        }
        lockConnection(
                event.syncJob().userId(), event.syncJob().connection().id());
        var activeJob = syncJobs.findFirstByConnection_IdAndStatusIn(
                event.syncJob().connection().id(), ACTIVE_SYNC_STATUSES);
        if (activeJob.isPresent()) {
            throw new SyncRequestConflictException(
                    "Another synchronization is already active for this ProviderConnection");
        }
        event.syncJob().retryDeadLetter();
        event.retryDeadLetter(availableAt);
        return JpaIntegrationStore.toView(event.syncJob());
    }

    private void lockConnection(UUID userId, UUID connectionId) {
        jdbcTemplate.queryForObject(
                """
                SELECT 1
                FROM (SELECT pg_advisory_xact_lock(
                    hashtextextended(CAST(? AS text), 0))) AS lock_result
                """,
                Integer.class,
                userId + ":sync-connection:" + connectionId);
    }

    private SyncOutboxEventEntity leasedEvent(SyncWorkItem workItem) {
        var event = outboxEvents.findById(workItem.eventId()).orElseThrow();
        if (!event.syncJob().id().equals(workItem.syncJobId())) {
            throw new IllegalStateException("Outbox event does not match its SyncJob");
        }
        return event;
    }

    private static Long retryAfterSeconds(ProviderFailure failure) {
        return failure.retryAfter() == null ? null : failure.retryAfter().toSeconds();
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record ClaimedEvent(
            UUID eventId,
            UUID syncJobId,
            int attempt,
            UUID correlationId,
            String traceparent) {
    }
}
