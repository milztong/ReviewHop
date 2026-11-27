package dev.reviewhop.learning.application;

import dev.reviewhop.learning.domain.ReviewMode;
import java.time.Instant;
import java.util.UUID;

public record DueReviewView(
        UUID reviewCardId,
        String title,
        String sourceUrl,
        String difficulty,
        String recallPrompt,
        String approachNotes,
        ReviewMode reviewMode,
        Instant dueAt) {
}
