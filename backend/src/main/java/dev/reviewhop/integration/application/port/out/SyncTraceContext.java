package dev.reviewhop.integration.application.port.out;

import java.util.Optional;
import java.util.function.Supplier;

public interface SyncTraceContext {

    Optional<String> currentTraceparent();

    <T> T continueTrace(String traceparent, String spanName, Supplier<T> action);
}
