package dev.reviewhop.integration.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.integration.application.port.out.IntegrationStore;
import dev.reviewhop.integration.application.port.out.ProviderConnector;
import java.time.Clock;

public final class ConnectCodewarsProfile {

    private final ProviderConnector connector;
    private final IntegrationStore store;
    private final Clock clock;

    public ConnectCodewarsProfile(ProviderConnector connector, IntegrationStore store, Clock clock) {
        this.connector = connector;
        this.store = store;
        this.clock = clock;
    }

    public ProviderConnectionView execute(AppUserId userId, String requestedUsername) {
        var username = requestedUsername == null ? "" : requestedUsername.trim();
        if (username.isBlank() || username.length() > 255) {
            throw new IllegalArgumentException("username must contain between 1 and 255 characters");
        }
        var profile = connector.resolvePublicProfile(username);
        return store.findOrCreateCodewarsConnection(
                userId, profile.externalUserId(), clock.instant());
    }
}
