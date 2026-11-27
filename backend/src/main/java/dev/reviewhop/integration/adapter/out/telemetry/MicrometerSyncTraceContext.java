package dev.reviewhop.integration.adapter.out.telemetry;

import dev.reviewhop.integration.application.port.out.SyncTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class MicrometerSyncTraceContext implements SyncTraceContext {

    private static final String TRACEPARENT = "traceparent";

    private final Tracer tracer;
    private final Propagator propagator;

    public MicrometerSyncTraceContext(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public Optional<String> currentTraceparent() {
        var current = tracer.currentSpan();
        if (current == null) {
            return Optional.empty();
        }
        var context = current.context();
        var traceFlags = Boolean.TRUE.equals(context.sampled()) ? "01" : "00";
        return Optional.of("00-%s-%s-%s".formatted(context.traceId(), context.spanId(), traceFlags));
    }

    @Override
    public <T> T continueTrace(String traceparent, String spanName, Supplier<T> action) {
        var carrier = traceparent == null ? Map.<String, String>of() : Map.of(TRACEPARENT, traceparent);
        Span span = propagator.extract(carrier, Map::get)
                .name(spanName)
                .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return action.get();
        } catch (RuntimeException failure) {
            span.error(failure);
            throw failure;
        } finally {
            span.end();
        }
    }
}
