package dev.reviewhop.learning.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CompletedSourceItem(
        String externalId,
        String title,
        URI sourceUrl,
        String difficulty,
        List<String> tags,
        Instant completedAt) {

    public CompletedSourceItem {
        Objects.requireNonNull(externalId, "externalId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        tags = List.copyOf(tags);
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }
}
