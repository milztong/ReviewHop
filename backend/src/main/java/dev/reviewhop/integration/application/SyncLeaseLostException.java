package dev.reviewhop.integration.application;

import java.util.UUID;

public final class SyncLeaseLostException extends RuntimeException {

    public SyncLeaseLostException(UUID eventId) {
        super("Sync outbox lease was lost: " + eventId);
    }
}

