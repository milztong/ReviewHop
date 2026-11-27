package dev.reviewhop.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
class AppUserEntity {

    @Id
    private UUID id;

    @Column(name = "oidc_issuer", nullable = false, length = 512)
    private String oidcIssuer;

    @Column(name = "oidc_subject", nullable = false, length = 255)
    private String oidcSubject;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "desired_retention", nullable = false)
    private double desiredRetention;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUserEntity() {
    }

    UUID id() {
        return id;
    }

    String timezone() {
        return timezone;
    }

    double desiredRetention() {
        return desiredRetention;
    }
}
