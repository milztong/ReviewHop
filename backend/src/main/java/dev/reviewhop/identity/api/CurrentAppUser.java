package dev.reviewhop.identity.api;

import dev.reviewhop.identity.application.port.out.AppUserStore;
import dev.reviewhop.identity.application.port.out.CurrentOidcIdentity;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

public final class CurrentAppUser {

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Berlin");

    private final CurrentOidcIdentity currentIdentity;
    private final AppUserStore appUserStore;
    private final Clock clock;

    public CurrentAppUser(
            CurrentOidcIdentity currentIdentity,
            AppUserStore appUserStore,
            Clock clock) {
        this.currentIdentity = currentIdentity;
        this.appUserStore = appUserStore;
        this.clock = clock;
    }

    public AppUserProfile getOrCreate() {
        var identity = currentIdentity.requireCurrent();
        return appUserStore.findOrCreate(identity, DEFAULT_TIMEZONE, clock.instant());
    }

    public Optional<AppUserProfile> findExisting() {
        return appUserStore.find(currentIdentity.requireCurrent());
    }
}
