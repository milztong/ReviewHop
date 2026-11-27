package dev.reviewhop.integration.adapter.in.web;

import dev.reviewhop.identity.api.CurrentAppUser;
import dev.reviewhop.integration.application.ConnectCodewarsProfile;
import dev.reviewhop.integration.application.ListProviderConnections;
import dev.reviewhop.integration.application.ProviderConnectionView;
import dev.reviewhop.integration.application.GetSyncJob;
import dev.reviewhop.integration.application.RequestCodewarsSynchronization;
import dev.reviewhop.integration.application.SyncJobView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider-connections")
class ProviderConnectionController {

    private final CurrentAppUser currentAppUser;
    private final ConnectCodewarsProfile connectCodewarsProfile;
    private final ListProviderConnections listProviderConnections;
    private final RequestCodewarsSynchronization requestCodewarsSynchronization;
    private final GetSyncJob getSyncJob;

    ProviderConnectionController(
            CurrentAppUser currentAppUser,
            ConnectCodewarsProfile connectCodewarsProfile,
            ListProviderConnections listProviderConnections,
            RequestCodewarsSynchronization requestCodewarsSynchronization,
            GetSyncJob getSyncJob) {
        this.currentAppUser = currentAppUser;
        this.connectCodewarsProfile = connectCodewarsProfile;
        this.listProviderConnections = listProviderConnections;
        this.requestCodewarsSynchronization = requestCodewarsSynchronization;
        this.getSyncJob = getSyncJob;
    }

    @Operation(summary = "List the current user's provider connections")
    @GetMapping
    List<ProviderConnectionView> list() {
        return listProviderConnections.execute(currentAppUser.getOrCreate().id());
    }

    @Operation(
            summary = "Connect a public Codewars profile",
            description = "Creates an unverified public profile association. It does not prove account ownership.")
    @ApiResponse(responseCode = "201", description = "Public profile connection created or returned")
    @PostMapping("/codewars")
    @ResponseStatus(HttpStatus.CREATED)
    ProviderConnectionView connectCodewars(@Valid @RequestBody ConnectCodewarsRequest request) {
        return connectCodewarsProfile.execute(
                currentAppUser.getOrCreate().id(), request.username());
    }

    @Operation(
            summary = "Synchronize completed Codewars challenges",
            description = "Queues an asynchronous import. Reusing the Idempotency-Key returns the original SyncJob.")
    @ApiResponse(responseCode = "202", description = "SyncJob accepted for asynchronous processing")
    @PostMapping("/{connectionId}/sync-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    SyncJobView synchronize(
            @PathVariable UUID connectionId,
            @Parameter(description = "Client-generated UUID for retry-safe synchronization", required = true)
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestHeader(name = "traceparent", required = false) String traceparent) {
        return requestCodewarsSynchronization.execute(
                currentAppUser.getOrCreate().id(),
                connectionId,
                idempotencyKey,
                traceparent);
    }

    @Operation(summary = "Get one asynchronous synchronization job")
    @GetMapping("/{connectionId}/sync-jobs/{syncJobId}")
    SyncJobView getSyncJob(
            @PathVariable UUID connectionId, @PathVariable UUID syncJobId) {
        return getSyncJob.execute(
                currentAppUser.getOrCreate().id(), connectionId, syncJobId);
    }
}
