package dev.reviewhop.learning.adapter.out.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import dev.reviewhop.learning.domain.DesiredRetention;
import dev.reviewhop.learning.domain.ReviewRating;
import dev.reviewhop.learning.domain.SchedulerState;
import dev.reviewhop.learning.domain.SchedulingSnapshot;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FsrsSchedulingPolicyTest {

    private static final Instant REVIEWED_AT = Instant.parse("2026-11-21T10:00:00Z");

    private final FsrsSchedulingPolicy policy = new FsrsSchedulingPolicy();

    @ParameterizedTest
    @EnumSource(ReviewRating.class)
    void producesDeterministicValidSnapshotsForEveryRating(ReviewRating rating) {
        var initial = SchedulingSnapshot.initial(Instant.parse("2026-11-20T10:00:00Z"));

        var first = policy.schedule(initial, DesiredRetention.DEFAULT, rating, REVIEWED_AT);
        var repeated = policy.schedule(initial, DesiredRetention.DEFAULT, rating, REVIEWED_AT);

        assertThat(first).isEqualTo(repeated);
        assertThat(first.next().lastReviewedAt()).isEqualTo(REVIEWED_AT);
        assertThat(first.next().stability()).isPositive();
        assertThat(first.next().difficulty()).isBetween(1.0, 10.0);
        assertThat(first.schedulerVersion()).isEqualTo(FsrsSchedulingPolicy.VERSION);
    }

    @Test
    void matchesThePinnedJavaFsrsReferenceSequence() {
        var initial = SchedulingSnapshot.initial(Instant.parse("2026-11-20T10:00:00Z"));

        var learned = policy.schedule(
                initial, DesiredRetention.DEFAULT, ReviewRating.GOOD, REVIEWED_AT).next();
        var reviewed = policy.schedule(
                learned,
                DesiredRetention.DEFAULT,
                ReviewRating.GOOD,
                Instant.parse("2026-11-24T10:00:00Z")).next();

        assertThat(learned.state()).isEqualTo(SchedulerState.REVIEW);
        assertThat(learned.dueAt()).isEqualTo(Instant.parse("2026-11-24T10:00:00Z"));
        assertThat(learned.stability()).isEqualTo(3.2602);
        assertThat(learned.difficulty()).isEqualTo(4.884631634813845);
        assertThat(reviewed.state()).isEqualTo(SchedulerState.REVIEW);
        assertThat(reviewed.dueAt()).isEqualTo(Instant.parse("2026-12-05T10:00:00Z"));
    }

    @Test
    void usesTheRealTimestampForAnEarlyReview() {
        var initial = SchedulingSnapshot.initial(Instant.parse("2026-11-20T10:00:00Z"));
        var learned = policy.schedule(
                initial, DesiredRetention.DEFAULT, ReviewRating.GOOD, REVIEWED_AT).next();
        var earlyReviewAt = Instant.parse("2026-11-22T09:30:00Z");

        var result = policy.schedule(
                learned, DesiredRetention.DEFAULT, ReviewRating.HARD, earlyReviewAt);

        assertThat(result.next().lastReviewedAt()).isEqualTo(earlyReviewAt);
        assertThat(result.next().dueAt()).isAfter(earlyReviewAt);
    }

    @Test
    void higherDesiredRetentionDoesNotScheduleLater() {
        var current = new SchedulingSnapshot(
                Instant.parse("2026-11-21T10:00:00Z"),
                SchedulerState.REVIEW,
                null,
                10.0,
                5.0,
                Instant.parse("2026-11-11T10:00:00Z"));

        var standard = policy.schedule(
                current, DesiredRetention.DEFAULT, ReviewRating.GOOD, REVIEWED_AT);
        var high = policy.schedule(
                current, new DesiredRetention(0.95), ReviewRating.GOOD, REVIEWED_AT);

        assertThat(high.next().dueAt()).isBeforeOrEqualTo(standard.next().dueAt());
    }
}
