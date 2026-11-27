package dev.reviewhop.integration.domain;

import java.util.Objects;

public record ProviderProfile(String externalUserId) {

    public ProviderProfile {
        Objects.requireNonNull(externalUserId, "externalUserId must not be null");
        if (externalUserId.isBlank()) {
            throw new IllegalArgumentException("externalUserId must not be blank");
        }
    }
}
