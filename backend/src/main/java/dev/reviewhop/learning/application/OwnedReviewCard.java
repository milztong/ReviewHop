package dev.reviewhop.learning.application;

import dev.reviewhop.learning.domain.SchedulingSnapshot;
import java.util.UUID;

public record OwnedReviewCard(UUID id, SchedulingSnapshot scheduling, long version) {
}
