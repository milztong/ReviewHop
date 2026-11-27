package dev.reviewhop.integration.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public record CompletedChallenge(
        String externalId,
        String title,
        URI sourceUrl,
        Instant completedAt) {

    public CompletedChallenge {
        Objects.requireNonNull(externalId, "externalId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }
}
