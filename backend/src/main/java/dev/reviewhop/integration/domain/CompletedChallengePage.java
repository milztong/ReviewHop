package dev.reviewhop.integration.domain;

import java.util.List;

public record CompletedChallengePage(
        int pageNumber,
        int totalPages,
        long totalItems,
        List<CompletedChallenge> challenges) {

    public CompletedChallengePage {
        if (pageNumber < 0 || totalPages < 0 || totalItems < 0) {
            throw new IllegalArgumentException("Page metadata must not be negative");
        }
        challenges = List.copyOf(challenges);
    }
}
