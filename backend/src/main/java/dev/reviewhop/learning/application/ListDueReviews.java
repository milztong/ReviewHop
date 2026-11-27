package dev.reviewhop.learning.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.application.port.out.LearningStore;
import java.time.Clock;
import java.util.List;

public final class ListDueReviews {

    private final LearningStore learningStore;
    private final Clock clock;

    public ListDueReviews(LearningStore learningStore, Clock clock) {
        this.learningStore = learningStore;
        this.clock = clock;
    }

    public List<DueReviewView> execute(AppUserId userId) {
        return learningStore.findDueReviews(userId, clock.instant());
    }
}
