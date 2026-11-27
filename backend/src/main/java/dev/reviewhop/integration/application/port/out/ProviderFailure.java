package dev.reviewhop.integration.application.port.out;

import java.time.Duration;

public final class ProviderFailure extends RuntimeException {

    private final String code;
    private final boolean retryable;
    private final Duration retryAfter;

    public ProviderFailure(String code, String message, boolean retryable, Duration retryAfter) {
        super(message);
        this.code = code;
        this.retryable = retryable;
        this.retryAfter = retryAfter;
    }

    public String code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
