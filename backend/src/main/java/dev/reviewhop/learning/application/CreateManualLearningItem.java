package dev.reviewhop.learning.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.application.port.out.LearningStore;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CreateManualLearningItem {

    private final LearningStore learningStore;
    private final Clock clock;

    public CreateManualLearningItem(LearningStore learningStore, Clock clock) {
        this.learningStore = learningStore;
        this.clock = clock;
    }

    public LearningItemView execute(
            AppUserId userId,
            UUID idempotencyKey,
            Command command) {
        var createdAt = clock.instant();
        if (command.firstCompletedAt().isAfter(createdAt)) {
            throw new IllegalArgumentException("firstCompletedAt must not be in the future");
        }
        return learningStore.createManualLearningItem(
                userId,
                idempotencyKey,
                command.title().trim(),
                command.sourceUrl(),
                normalized(command.difficulty()),
                command.tags().stream().map(String::trim).filter(tag -> !tag.isBlank()).distinct().toList(),
                normalized(command.notes()),
                normalized(command.recallPrompt()),
                command.firstCompletedAt(),
                createdAt);
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record Command(
            String title,
            URI sourceUrl,
            String difficulty,
            List<String> tags,
            String notes,
            String recallPrompt,
            Instant firstCompletedAt) {
    }
}
