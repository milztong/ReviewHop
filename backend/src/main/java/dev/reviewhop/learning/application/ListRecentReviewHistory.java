package dev.reviewhop.learning.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.application.port.out.LearningStore;
import java.util.List;

public final class ListRecentReviewHistory {

    private static final int DEFAULT_LIMIT = 20;

    private final LearningStore learningStore;

    public ListRecentReviewHistory(LearningStore learningStore) {
        this.learningStore = learningStore;
    }

    public List<ReviewHistoryEntry> execute(AppUserId userId) {
        return learningStore.findRecentReviewHistory(userId, DEFAULT_LIMIT);
    }
}
