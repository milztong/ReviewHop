package dev.reviewhop.learning.adapter.out.scheduling;

import dev.reviewhop.learning.application.port.out.SchedulingPolicy;
import dev.reviewhop.learning.domain.DesiredRetention;
import dev.reviewhop.learning.domain.ReviewRating;
import dev.reviewhop.learning.domain.ScheduleResult;
import dev.reviewhop.learning.domain.SchedulerState;
import dev.reviewhop.learning.domain.SchedulingSnapshot;
import io.github.openspacedrepetition.Card;
import io.github.openspacedrepetition.Rating;
import io.github.openspacedrepetition.Scheduler;
import io.github.openspacedrepetition.State;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public final class FsrsSchedulingPolicy implements SchedulingPolicy {

    public static final String VERSION = "fsrs-6-java-1.0.0-defaults-v1";

    private static final Duration[] LEARNING_STEPS = {Duration.ofMinutes(10)};
    private static final Duration[] RELEARNING_STEPS = {Duration.ofMinutes(10)};
    private static final int MAXIMUM_INTERVAL_DAYS = 36_500;

    @Override
    public ScheduleResult schedule(
            SchedulingSnapshot current,
            DesiredRetention desiredRetention,
            ReviewRating rating,
            Instant reviewedAt) {
        var scheduler = Scheduler.builder()
                .desiredRetention(desiredRetention.value())
                .learningSteps(LEARNING_STEPS.clone())
                .relearningSteps(RELEARNING_STEPS.clone())
                .maximumInterval(MAXIMUM_INTERVAL_DAYS)
                .enableFuzzing(false)
                .build();
        var result = scheduler.reviewCard(toLibraryCard(current), toLibraryRating(rating), reviewedAt);
        return new ScheduleResult(toSnapshot(result.card()), VERSION);
    }

    private static Card toLibraryCard(SchedulingSnapshot snapshot) {
        return Card.builder()
                .cardId(0)
                .state(State.valueOf(snapshot.state().name()))
                .step(snapshot.step())
                .stability(snapshot.stability())
                .difficulty(snapshot.difficulty())
                .due(snapshot.dueAt())
                .lastReview(snapshot.lastReviewedAt())
                .build();
    }

    private static Rating toLibraryRating(ReviewRating rating) {
        return Rating.valueOf(rating.name());
    }

    private static SchedulingSnapshot toSnapshot(Card card) {
        return new SchedulingSnapshot(
                card.getDue(),
                SchedulerState.valueOf(card.getState().name()),
                card.getStep(),
                card.getStability(),
                card.getDifficulty(),
                card.getLastReview());
    }
}
