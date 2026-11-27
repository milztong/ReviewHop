package dev.reviewhop.learning.domain;

import java.util.Objects;

public record ScheduleResult(SchedulingSnapshot next, String schedulerVersion) {

    public ScheduleResult {
        Objects.requireNonNull(next, "next must not be null");
        if (schedulerVersion == null || schedulerVersion.isBlank()) {
            throw new IllegalArgumentException("schedulerVersion must not be blank");
        }
    }
}
