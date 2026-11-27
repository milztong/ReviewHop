package dev.reviewhop.learning.application.port.out;

import dev.reviewhop.learning.domain.DesiredRetention;
import dev.reviewhop.learning.domain.ReviewRating;
import dev.reviewhop.learning.domain.ScheduleResult;
import dev.reviewhop.learning.domain.SchedulingSnapshot;
import java.time.Instant;

public interface SchedulingPolicy {

    ScheduleResult schedule(
            SchedulingSnapshot current,
            DesiredRetention desiredRetention,
            ReviewRating rating,
            Instant reviewedAt);
}
