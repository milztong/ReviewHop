package dev.reviewhop.integration.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sync_outbox_event")
class SyncOutboxEventEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aggregate_id", nullable = false)
    private SyncJobEntity syncJob;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload_version", nullable = false)
    private short payloadVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "lease_token")
    private UUID leaseToken;

    @Column(name = "lease_owner", length = 128)
    private String leaseOwner;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(length = 255)
    private String traceparent;

    @Column(name = "last_failure_code", length = 64)
    private String lastFailureCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected SyncOutboxEventEntity() {
    }

    SyncOutboxEventEntity(
            UUID id,
            SyncJobEntity syncJob,
            UUID correlationId,
            String traceparent,
            Instant createdAt) {
        this.id = id;
        this.syncJob = syncJob;
        this.eventType = "SyncRequested";
        this.payloadVersion = 1;
        this.payload = "{\"syncJobId\":\"" + syncJob.id() + "\"}";
        this.status = "PENDING";
        this.availableAt = createdAt;
        this.correlationId = correlationId;
        this.traceparent = traceparent;
        this.createdAt = createdAt;
    }

    void complete(UUID expectedLeaseToken, Instant now) {
        requireLease(expectedLeaseToken);
        status = "COMPLETED";
        completedAt = now;
        clearLease();
    }

    void scheduleRetry(UUID expectedLeaseToken, String failureCode, Instant nextAttemptAt) {
        requireLease(expectedLeaseToken);
        status = "PENDING";
        lastFailureCode = failureCode;
        availableAt = nextAttemptAt;
        clearLease();
    }

    void deadLetter(UUID expectedLeaseToken, String failureCode, Instant now) {
        requireLease(expectedLeaseToken);
        status = "DEAD_LETTERED";
        lastFailureCode = failureCode;
        completedAt = now;
        clearLease();
    }

    void retryDeadLetter(Instant availableAt) {
        if (!"DEAD_LETTERED".equals(status)) {
            throw new IllegalStateException("Only a DEAD_LETTERED event can be retried");
        }
        status = "PENDING";
        attemptCount = 0;
        lastFailureCode = null;
        completedAt = null;
        this.availableAt = availableAt;
    }

    private void requireLease(UUID expectedLeaseToken) {
        if (!"PROCESSING".equals(status) || !expectedLeaseToken.equals(leaseToken)) {
            throw new IllegalStateException("Sync outbox lease is no longer owned by this worker");
        }
    }

    private void clearLease() {
        leaseToken = null;
        leaseOwner = null;
        leaseExpiresAt = null;
    }

    UUID id() {
        return id;
    }

    SyncJobEntity syncJob() {
        return syncJob;
    }
}

