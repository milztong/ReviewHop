package dev.reviewhop.integration.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_job")
class SyncJobEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private ProviderConnectionEntity connection;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "start_cursor", length = 1024)
    private String startCursor;

    @Column(name = "result_cursor", length = 1024)
    private String resultCursor;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "existing_count", nullable = false)
    private int existingCount;

    @Column(name = "pages_processed", nullable = false)
    private int pagesProcessed;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_detail", length = 500)
    private String failureDetail;

    @Column(name = "retry_after_seconds")
    private Long retryAfterSeconds;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected SyncJobEntity() {
    }

    SyncJobEntity(
            UUID id,
            ProviderConnectionEntity connection,
            UUID userId,
            UUID idempotencyKey,
            Instant startedAt) {
        this.id = id;
        this.connection = connection;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.status = "REQUESTED";
        this.startCursor = connection.cursorValue();
        this.startedAt = startedAt;
    }

    void markRunning() {
        if (!"REQUESTED".equals(status)
                && !"RETRY_SCHEDULED".equals(status)
                && !"RUNNING".equals(status)) {
            throw new IllegalStateException("Only an active SyncJob can start processing");
        }
        this.status = "RUNNING";
        this.completedAt = null;
    }

    void succeed(
            String resultCursor,
            int importedCount,
            int existingCount,
            int pagesProcessed,
            Instant completedAt) {
        requireRunning();
        this.status = "SUCCEEDED";
        this.resultCursor = resultCursor;
        this.importedCount = importedCount;
        this.existingCount = existingCount;
        this.pagesProcessed = pagesProcessed;
        this.completedAt = completedAt;
    }

    void failTerminal(
            String failureCode,
            String failureDetail,
            Long retryAfterSeconds,
            int importedCount,
            int existingCount,
            int pagesProcessed,
            Instant completedAt) {
        requireRunning();
        this.status = "TERMINAL_FAILED";
        this.failureCode = failureCode;
        this.failureDetail = failureDetail;
        this.retryAfterSeconds = retryAfterSeconds;
        this.importedCount = importedCount;
        this.existingCount = existingCount;
        this.pagesProcessed = pagesProcessed;
        this.completedAt = completedAt;
    }

    void scheduleRetry(
            String failureCode,
            String failureDetail,
            Long retryAfterSeconds,
            int importedCount,
            int existingCount,
            int pagesProcessed) {
        requireRunning();
        this.status = "RETRY_SCHEDULED";
        setFailure(
                failureCode,
                failureDetail,
                retryAfterSeconds,
                importedCount,
                existingCount,
                pagesProcessed);
    }

    void deadLetter(
            String failureCode,
            String failureDetail,
            Long retryAfterSeconds,
            int importedCount,
            int existingCount,
            int pagesProcessed,
            Instant completedAt) {
        requireRunning();
        this.status = "DEAD_LETTERED";
        setFailure(
                failureCode,
                failureDetail,
                retryAfterSeconds,
                importedCount,
                existingCount,
                pagesProcessed);
        this.completedAt = completedAt;
    }

    void retryDeadLetter() {
        if (!"DEAD_LETTERED".equals(status)) {
            throw new IllegalStateException("Only a DEAD_LETTERED SyncJob can be retried");
        }
        this.status = "REQUESTED";
        this.failureCode = null;
        this.failureDetail = null;
        this.retryAfterSeconds = null;
        this.completedAt = null;
    }

    private void setFailure(
            String failureCode,
            String failureDetail,
            Long retryAfterSeconds,
            int importedCount,
            int existingCount,
            int pagesProcessed) {
        this.failureCode = failureCode;
        this.failureDetail = failureDetail;
        this.retryAfterSeconds = retryAfterSeconds;
        this.importedCount = importedCount;
        this.existingCount = existingCount;
        this.pagesProcessed = pagesProcessed;
    }

    boolean isRunning() {
        return "RUNNING".equals(status);
    }

    private void requireRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("Only a RUNNING SyncJob can be completed");
        }
    }

    UUID id() {
        return id;
    }

    ProviderConnectionEntity connection() {
        return connection;
    }

    UUID userId() {
        return userId;
    }

    UUID idempotencyKey() {
        return idempotencyKey;
    }

    String status() {
        return status;
    }

    int importedCount() {
        return importedCount;
    }

    int existingCount() {
        return existingCount;
    }

    int pagesProcessed() {
        return pagesProcessed;
    }

    String failureCode() {
        return failureCode;
    }

    String failureDetail() {
        return failureDetail;
    }

    Long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant completedAt() {
        return completedAt;
    }
}
