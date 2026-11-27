package dev.reviewhop.identity.adapter.in.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("ankileet.security.rate-limit")
record ApiRateLimitProperties(
        boolean enabled,
        @Min(1) int readRequestsPerWindow,
        @Min(1) int writeRequestsPerWindow,
        @NotNull Duration window,
        @Min(1) @Max(1_000_000) long maxTrackedIdentities) {

    ApiRateLimitProperties {
        if (window != null && window.toMillis() < 1) {
            throw new IllegalArgumentException("Rate-limit window must be at least one millisecond");
        }
    }
}
