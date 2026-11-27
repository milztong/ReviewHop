package dev.reviewhop.integration.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.integration.application.port.out.IntegrationStore;
import java.util.List;

public final class ListProviderConnections {

    private final IntegrationStore store;

    public ListProviderConnections(IntegrationStore store) {
        this.store = store;
    }

    public List<ProviderConnectionView> execute(AppUserId userId) {
        return store.findConnections(userId);
    }
}
