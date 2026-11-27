package dev.reviewhop.learning.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;

record CreateManualLearningItemRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull URI sourceUrl,
        @Size(max = 50) String difficulty,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags,
        @Size(max = 4000) String notes,
        @Size(max = 4000) String recallPrompt,
        @NotNull Instant firstCompletedAt) {

    CreateManualLearningItemRequest {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
