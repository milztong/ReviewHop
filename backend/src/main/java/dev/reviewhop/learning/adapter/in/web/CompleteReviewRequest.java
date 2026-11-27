package dev.reviewhop.learning.adapter.in.web;

import dev.reviewhop.learning.domain.ReviewRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

record CompleteReviewRequest(
        @NotNull ReviewRating rating,
        @NotNull Instant startedAt,
        @Size(max = 2000) String note) {
}
