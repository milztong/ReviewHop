package dev.reviewhop.integration.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.integration.application.port.out.IntegrationStore;
import dev.reviewhop.integration.application.port.out.SyncTraceContext;
import java.time.Clock;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestCodewarsSynchronization {

    private static final Pattern TRACEPARENT = Pattern.compile(
            "^[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");

    private final IntegrationStore store;
    private final Clock clock;
    private final SyncTraceContext traceContext;

    public RequestCodewarsSynchronization(
            IntegrationStore store, Clock clock, SyncTraceContext traceContext) {
        this.store = store;
        this.clock = clock;
        this.traceContext = traceContext;
    }

    public SyncJobView execute(
            AppUserId userId,
            UUID connectionId,
            UUID idempotencyKey,
            String traceparent) {
        store.findConnection(userId, connectionId)
                .orElseThrow(() -> new ProviderConnectionNotFoundException(connectionId));
        return store.requestSync(
                        userId,
                        connectionId,
                        idempotencyKey,
                        idempotencyKey,
                        traceContext.currentTraceparent()
                                .orElseGet(() -> normalizedTraceparent(traceparent)),
                        clock.instant())
                .job();
    }

    private static String normalizedTraceparent(String traceparent) {
        if (traceparent == null || !TRACEPARENT.matcher(traceparent).matches()) {
            return null;
        }
        return traceparent;
    }
}

