package dev.reviewhop.learning.application;

import dev.reviewhop.learning.domain.ReviewMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningItemView(
        UUID learningItemId,
        UUID reviewCardId,
        String title,
        String sourceUrl,
        String difficulty,
        List<String> tags,
        String notes,
        String recallPrompt,
        ReviewMode reviewMode,
        Instant dueAt) {
}
