package dev.reviewhop.integration.application;

public final class SyncRequestConflictException extends RuntimeException {

    public SyncRequestConflictException(String message) {
        super(message);
    }
}
