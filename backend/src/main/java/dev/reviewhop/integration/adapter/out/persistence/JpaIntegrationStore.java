package dev.reviewhop.integration.adapter.out.persistence;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.integration.application.ProviderConnectionView;
import dev.reviewhop.integration.application.SyncJobView;
import dev.reviewhop.integration.application.SyncRequestConflictException;
import dev.reviewhop.integration.application.SyncStart;
import dev.reviewhop.integration.application.port.out.IntegrationStore;
import dev.reviewhop.integration.domain.ConnectionMode;
import dev.reviewhop.integration.domain.ConnectionStatus;
import dev.reviewhop.integration.domain.Provider;
import dev.reviewhop.integration.domain.SyncJobStatus;
import dev.reviewhop.integration.domain.VerificationStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaIntegrationStore implements IntegrationStore {

    private static final Set<String> ACTIVE_SYNC_STATUSES = Set.of(
            "REQUESTED", "RUNNING", "RETRY_SCHEDULED");

    private final ProviderConnectionJpaRepository connections;
    private final SyncJobJpaRepository syncJobs;
    private final SyncOutboxEventJpaRepository outboxEvents;
    private final EntityManager entityManager;

    JpaIntegrationStore(
            ProviderConnectionJpaRepository connections,
            SyncJobJpaRepository syncJobs,
            SyncOutboxEventJpaRepository outboxEvents,
            EntityManager entityManager) {
        this.connections = connections;
        this.syncJobs = syncJobs;
        this.outboxEvents = outboxEvents;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public ProviderConnectionView findOrCreateCodewarsConnection(
            AppUserId userId, String externalUserId, Instant now) {
        lock(userId.value() + ":CODEWARS:" + externalUserId);
        var connection = connections.findByUserIdAndProviderAndExternalUserId(
                        userId.value(), Provider.CODEWARS.name(), externalUserId)
                .orElseGet(() -> connections.save(new ProviderConnectionEntity(
                        UUID.randomUUID(), userId.value(), externalUserId, now)));
        return toView(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderConnectionView> findConnections(AppUserId userId) {
        return connections.findAllByUserIdOrderByCreatedAtAsc(userId.value()).stream()
                .map(JpaIntegrationStore::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProviderConnectionView> findConnection(
            AppUserId userId, UUID connectionId) {
        return connections.findByIdAndUserId(connectionId, userId.value())
                .map(JpaIntegrationStore::toView);
    }

    @Override
    @Transactional
    public SyncStart requestSync(
            AppUserId userId,
            UUID connectionId,
            UUID idempotencyKey,
            UUID correlationId,
            String traceparent,
            Instant requestedAt) {
        lock(userId.value() + ":sync-connection:" + connectionId);
        var existing = syncJobs.findByUserIdAndIdempotencyKey(userId.value(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().connection().id().equals(connectionId)) {
                throw new SyncRequestConflictException(
                        "Idempotency-Key was already used for another ProviderConnection");
            }
            return new SyncStart(toView(existing.get()), false);
        }
        if (syncJobs.findFirstByConnection_IdAndStatusIn(
                        connectionId, ACTIVE_SYNC_STATUSES)
                .isPresent()) {
            throw new SyncRequestConflictException(
                    "A synchronization is already active for this ProviderConnection");
        }
        var connection = connections.findByIdAndUserId(connectionId, userId.value())
                .orElseThrow();
        var job = syncJobs.save(new SyncJobEntity(
                UUID.randomUUID(), connection, userId.value(), idempotencyKey, requestedAt));
        outboxEvents.save(new SyncOutboxEventEntity(
                UUID.randomUUID(),
                job,
                correlationId,
                abbreviate(traceparent, 255),
                requestedAt));
        return new SyncStart(toView(job), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SyncJobView> findSyncJob(
            AppUserId userId, UUID connectionId, UUID syncJobId) {
        return syncJobs.findByIdAndConnection_IdAndUserId(
                        syncJobId, connectionId, userId.value())
                .map(JpaIntegrationStore::toView);
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

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    static ProviderConnectionView toView(ProviderConnectionEntity entity) {
        return new ProviderConnectionView(
                entity.id(),
                Provider.valueOf(entity.provider()),
                ConnectionMode.valueOf(entity.mode()),
                ConnectionStatus.valueOf(entity.status()),
                VerificationStatus.valueOf(entity.verificationStatus()),
                entity.externalUserId(),
                entity.lastSyncedAt());
    }

    static SyncJobView toView(SyncJobEntity entity) {
        return new SyncJobView(
                entity.id(),
                entity.connection().id(),
                SyncJobStatus.valueOf(entity.status()),
                entity.importedCount(),
                entity.existingCount(),
                entity.pagesProcessed(),
                entity.failureCode(),
                entity.failureDetail(),
                entity.retryAfterSeconds(),
                entity.startedAt(),
                entity.completedAt());
    }
}
