package dev.reviewhop.identity.adapter.out.persistence;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.identity.api.AppUserProfile;
import dev.reviewhop.identity.application.port.out.AppUserStore;
import dev.reviewhop.identity.domain.OidcIdentity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaAppUserStore implements AppUserStore {

    private final AppUserJpaRepository repository;

    JpaAppUserStore(AppUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppUserProfile> find(OidcIdentity identity) {
        return repository.findByOidcIssuerAndOidcSubject(
                        identity.issuer().toString(), identity.subject())
                .map(JpaAppUserStore::toProfile);
    }

    @Override
    @Transactional
    public AppUserProfile findOrCreate(
            OidcIdentity identity,
            ZoneId defaultTimezone,
            Instant createdAt) {
        var issuer = identity.issuer().toString();
        var existing = repository.findByOidcIssuerAndOidcSubject(issuer, identity.subject());
        if (existing.isPresent()) {
            return toProfile(existing.get());
        }
        repository.insertIfAbsent(
                UUID.randomUUID(),
                issuer,
                identity.subject(),
                defaultTimezone.getId(),
                createdAt);

        var entity = repository.findByOidcIssuerAndOidcSubject(issuer, identity.subject())
                .orElseThrow(() -> new IllegalStateException("AppUser provisioning failed"));
        return toProfile(entity);
    }

    private static AppUserProfile toProfile(AppUserEntity entity) {
        return new AppUserProfile(
                new AppUserId(entity.id()),
                ZoneId.of(entity.timezone()),
                entity.desiredRetention());
    }
}
