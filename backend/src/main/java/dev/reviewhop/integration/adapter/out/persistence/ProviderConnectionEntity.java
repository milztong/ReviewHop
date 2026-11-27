package dev.reviewhop.integration.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_connection")
class ProviderConnectionEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 32)
    private String mode;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "verification_status", nullable = false, length = 32)
    private String verificationStatus;

    @Column(name = "external_user_id", nullable = false, length = 255)
    private String externalUserId;

    @Column(name = "cursor_value", length = 1024)
    private String cursorValue;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProviderConnectionEntity() {
    }

    ProviderConnectionEntity(UUID id, UUID userId, String externalUserId, Instant now) {
        this.id = id;
        this.userId = userId;
        this.provider = "CODEWARS";
        this.mode = "PUBLIC_PROFILE";
        this.status = "ACTIVE";
        this.verificationStatus = "UNVERIFIED";
        this.externalUserId = externalUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void synchronizedAt(String resultCursor, Instant completedAt) {
        if (resultCursor != null) {
            this.cursorValue = resultCursor;
        }
        this.lastSyncedAt = completedAt;
        this.updatedAt = completedAt;
    }

    UUID id() {
        return id;
    }

    UUID userId() {
        return userId;
    }

    String provider() {
        return provider;
    }

    String mode() {
        return mode;
    }

    String status() {
        return status;
    }

    String verificationStatus() {
        return verificationStatus;
    }

    String externalUserId() {
        return externalUserId;
    }

    String cursorValue() {
        return cursorValue;
    }

    Instant lastSyncedAt() {
        return lastSyncedAt;
    }
}
