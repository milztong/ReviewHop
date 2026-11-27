package dev.reviewhop.learning.adapter.in.web;

import dev.reviewhop.identity.api.CurrentAppUser;
import dev.reviewhop.learning.application.CompleteReview;
import dev.reviewhop.learning.application.DueReviewView;
import dev.reviewhop.learning.application.ListDueReviews;
import dev.reviewhop.learning.application.ListRecentReviewHistory;
import dev.reviewhop.learning.application.ListReviewHistory;
import dev.reviewhop.learning.application.ReviewCompletionView;
import dev.reviewhop.learning.application.ReviewHistoryEntry;
import dev.reviewhop.learning.domain.DesiredRetention;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController {

    private final CurrentAppUser currentAppUser;
    private final ListDueReviews listDueReviews;
    private final CompleteReview completeReview;
    private final ListReviewHistory listReviewHistory;
    private final ListRecentReviewHistory listRecentReviewHistory;

    ReviewController(
            CurrentAppUser currentAppUser,
            ListDueReviews listDueReviews,
            CompleteReview completeReview,
            ListReviewHistory listReviewHistory,
            ListRecentReviewHistory listRecentReviewHistory) {
        this.currentAppUser = currentAppUser;
        this.listDueReviews = listDueReviews;
        this.completeReview = completeReview;
        this.listReviewHistory = listReviewHistory;
        this.listRecentReviewHistory = listRecentReviewHistory;
    }

    @Operation(summary = "List ReviewCards due for the current user")
    @GetMapping("/due")
    List<DueReviewView> listDue() {
        return listDueReviews.execute(currentAppUser.getOrCreate().id());
    }

    @Operation(
            summary = "Complete a Review",
            description = "Stores one immutable ReviewLog and schedules the ReviewCard. "
                    + "Reusing the Idempotency-Key returns the first completion.")
    @PostMapping("/{reviewCardId}/completions")
    ReviewCompletionView complete(
            @PathVariable UUID reviewCardId,
            @Parameter(description = "Client-generated UUID for retry-safe completion", required = true)
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CompleteReviewRequest request) {
        var user = currentAppUser.getOrCreate();
        return completeReview.execute(
                user.id(),
                reviewCardId,
                idempotencyKey,
                new DesiredRetention(user.desiredRetention()),
                request.rating(),
                request.startedAt(),
                request.note());
    }

    @Operation(summary = "Get immutable ReviewLog history for one owned ReviewCard")
    @GetMapping("/{reviewCardId}/history")
    List<ReviewHistoryEntry> history(@PathVariable UUID reviewCardId) {
        return listReviewHistory.execute(currentAppUser.getOrCreate().id(), reviewCardId);
    }

    @Operation(summary = "Get the most recent ReviewLog entries for the current user")
    @GetMapping("/history")
    List<ReviewHistoryEntry> recentHistory() {
        return listRecentReviewHistory.execute(currentAppUser.getOrCreate().id());
    }
}
