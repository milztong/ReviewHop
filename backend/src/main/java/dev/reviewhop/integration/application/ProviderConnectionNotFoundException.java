package dev.reviewhop.integration.application;

import java.util.UUID;

public final class ProviderConnectionNotFoundException extends RuntimeException {

    public ProviderConnectionNotFoundException(UUID connectionId) {
        super("ProviderConnection not found: " + connectionId);
    }
}
