package dev.reviewhop.identity.api;

import java.util.Objects;
import java.util.UUID;

public record AppUserId(UUID value) {

    public AppUserId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
