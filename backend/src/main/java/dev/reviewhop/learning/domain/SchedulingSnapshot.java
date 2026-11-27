package dev.reviewhop.learning.domain;

import java.time.Instant;
import java.util.Objects;

public record SchedulingSnapshot(
        Instant dueAt,
        SchedulerState state,
        Integer step,
        Double stability,
        Double difficulty,
        Instant lastReviewedAt) {

    public SchedulingSnapshot {
        Objects.requireNonNull(dueAt, "dueAt must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (step != null && step < 0) {
            throw new IllegalArgumentException("step must not be negative");
        }
        if ((stability == null) != (difficulty == null)) {
            throw new IllegalArgumentException("stability and difficulty must both be present or absent");
        }
        if (stability != null && (!Double.isFinite(stability) || stability <= 0.0)) {
            throw new IllegalArgumentException("stability must be positive and finite");
        }
        if (difficulty != null
                && (!Double.isFinite(difficulty) || difficulty < 1.0 || difficulty > 10.0)) {
            throw new IllegalArgumentException("difficulty must be finite and between 1 and 10");
        }
        if (state == SchedulerState.REVIEW && step != null) {
            throw new IllegalArgumentException("review state must not have a learning step");
        }
        if (state != SchedulerState.REVIEW && step == null) {
            throw new IllegalArgumentException("learning states require a step");
        }
        if (state != SchedulerState.LEARNING && stability == null) {
            throw new IllegalArgumentException("non-initial states require a memory state");
        }
        if ((stability == null) != (lastReviewedAt == null)) {
            throw new IllegalArgumentException("memory state and last review must be present together");
        }
    }

    public static SchedulingSnapshot initial(Instant dueAt) {
        return new SchedulingSnapshot(
                dueAt, SchedulerState.LEARNING, 0, null, null, null);
    }
}
