package dev.reviewhop.identity.adapter.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

final class AuthenticatedRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final ApiRateLimitProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Cache<RateLimitKey, WindowCounter> counters;

    AuthenticatedRateLimitFilter(
            ApiRateLimitProperties properties,
            Clock clock,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.counters = Caffeine.newBuilder()
                .maximumSize(properties.maxTrackedIdentities() * AccessType.values().length)
                .expireAfterAccess(properties.window().multipliedBy(2))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled()
                || !request.getRequestURI().startsWith("/api/v1/")
                || "/api/v1/system/status".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        var accessType = READ_METHODS.contains(request.getMethod())
                ? AccessType.READ
                : AccessType.WRITE;
        var limit = accessType == AccessType.READ
                ? properties.readRequestsPerWindow()
                : properties.writeRequestsPerWindow();
        var token = jwtAuthentication.getToken();
        var decision = consume(
                token.getIssuer().toString(), token.getSubject(), accessType, limit);
        response.setHeader("X-RateLimit-Limit", Integer.toString(limit));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(decision.remaining()));
        record(accessType, decision.allowed() ? "allowed" : "rejected");

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "The authenticated API rate limit was exceeded; retry after the indicated delay");
        problem.setTitle("Rate limit exceeded");
        problem.setProperty("retryAfterSeconds", decision.retryAfterSeconds());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private Decision consume(
            String issuer, String subject, AccessType accessType, int limit) {
        var now = clock.millis();
        var windowMillis = properties.window().toMillis();
        var windowStartedAt = now - Math.floorMod(now, windowMillis);
        var updated = new AtomicReference<WindowCounter>();
        counters.asMap().compute(new RateLimitKey(issuer, subject, accessType), (key, current) -> {
            var next = current == null || current.windowStartedAt() != windowStartedAt
                    ? new WindowCounter(windowStartedAt, 1)
                    : new WindowCounter(windowStartedAt, current.requests() + 1);
            updated.set(next);
            return next;
        });
        var current = updated.get();
        var allowed = current.requests() <= limit;
        var remaining = Math.max(0, limit - current.requests());
        var retryAfterMillis = Math.max(1, windowStartedAt + windowMillis - now);
        var retryAfterSeconds = Math.max(1, (retryAfterMillis + 999) / 1000);
        return new Decision(allowed, remaining, retryAfterSeconds);
    }

    private void record(AccessType accessType, String outcome) {
        meterRegistry.counter(
                "ankileet.security.rate_limit.requests",
                "access", accessType.name().toLowerCase(java.util.Locale.ROOT),
                "outcome", outcome).increment();
    }

    private enum AccessType {
        READ,
        WRITE
    }

    private record RateLimitKey(String issuer, String subject, AccessType accessType) {
    }

    private record WindowCounter(long windowStartedAt, int requests) {
    }

    private record Decision(boolean allowed, int remaining, long retryAfterSeconds) {
    }
}
