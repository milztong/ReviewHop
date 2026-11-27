package dev.reviewhop.integration.adapter.in.web;

import dev.reviewhop.integration.application.RetryDeadLetterSync;
import dev.reviewhop.integration.application.SyncJobView;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/sync-jobs")
class SyncOperationsController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SyncOperationsController.class);

    private final RetryDeadLetterSync retryDeadLetterSync;

    SyncOperationsController(RetryDeadLetterSync retryDeadLetterSync) {
        this.retryDeadLetterSync = retryDeadLetterSync;
    }

    @Operation(summary = "Retry a dead-lettered synchronization job")
    @PostMapping("/{syncJobId}/retry")
    @PreAuthorize("hasAuthority('ROLE_ankileet-operator')")
    SyncJobView retry(@PathVariable UUID syncJobId) {
        var job = retryDeadLetterSync.execute(syncJobId);
        LOGGER.info("dead_letter_sync_retried syncJobId={}", syncJobId);
        return job;
    }
}
