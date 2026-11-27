package dev.reviewhop.learning.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.application.port.out.LearningStore;
import java.util.List;
import java.util.UUID;

public final class ListReviewHistory {

    private final LearningStore learningStore;

    public ListReviewHistory(LearningStore learningStore) {
        this.learningStore = learningStore;
    }

    public List<ReviewHistoryEntry> execute(AppUserId userId, UUID reviewCardId) {
        return learningStore.findReviewHistory(userId, reviewCardId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCard not found"));
    }
}
