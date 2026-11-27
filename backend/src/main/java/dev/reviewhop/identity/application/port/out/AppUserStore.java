package dev.reviewhop.identity.application.port.out;

import dev.reviewhop.identity.api.AppUserProfile;
import dev.reviewhop.identity.domain.OidcIdentity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

public interface AppUserStore {

    Optional<AppUserProfile> find(OidcIdentity identity);

    AppUserProfile findOrCreate(OidcIdentity identity, ZoneId defaultTimezone, Instant createdAt);
}
