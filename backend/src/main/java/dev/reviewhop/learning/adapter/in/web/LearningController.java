package dev.reviewhop.learning.adapter.in.web;

import dev.reviewhop.identity.api.CurrentAppUser;
import dev.reviewhop.learning.application.CreateManualLearningItem;
import dev.reviewhop.learning.application.LearningItemView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning-items")
class LearningController {

    private final CurrentAppUser currentAppUser;
    private final CreateManualLearningItem createManualLearningItem;

    LearningController(
            CurrentAppUser currentAppUser,
            CreateManualLearningItem createManualLearningItem) {
        this.currentAppUser = currentAppUser;
        this.createManualLearningItem = createManualLearningItem;
    }

    @Operation(
            summary = "Create a manual LearningItem",
            description = "Creates a MANUAL SourceItem, a user-owned LearningItem, and its initial "
                    + "APPROACH_RECALL ReviewCard. Reusing the Idempotency-Key returns the original result.")
    @ApiResponse(responseCode = "201", description = "LearningItem and ReviewCard created")
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    LearningItemView createManual(
            @Parameter(description = "Client-generated UUID for retry-safe creation", required = true)
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateManualLearningItemRequest request) {
        validateSourceUrl(request.sourceUrl());
        var user = currentAppUser.getOrCreate();
        return createManualLearningItem.execute(
                user.id(),
                idempotencyKey,
                new CreateManualLearningItem.Command(
                        request.title(),
                        request.sourceUrl(),
                        request.difficulty(),
                        request.tags(),
                        request.notes(),
                        request.recallPrompt(),
                        request.firstCompletedAt()));
    }

    private static void validateSourceUrl(URI sourceUrl) {
        if (!"https".equalsIgnoreCase(sourceUrl.getScheme())
                || sourceUrl.getHost() == null
                || sourceUrl.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "sourceUrl must be an absolute HTTPS URL without user information");
        }
    }
}
