package dev.reviewhop.integration.application;

import dev.reviewhop.integration.application.port.out.ProviderFailure;

public final class SyncProcessingFailure extends RuntimeException {

    private final ProviderFailure providerFailure;
    private final SyncExecutionResult partialResult;

    public SyncProcessingFailure(
            ProviderFailure providerFailure, SyncExecutionResult partialResult) {
        super(providerFailure.getMessage(), providerFailure);
        this.providerFailure = providerFailure;
        this.partialResult = partialResult;
    }

    public ProviderFailure providerFailure() {
        return providerFailure;
    }

    public SyncExecutionResult partialResult() {
        return partialResult;
    }
}

