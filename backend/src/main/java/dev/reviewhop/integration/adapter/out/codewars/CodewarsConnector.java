package dev.reviewhop.integration.adapter.out.codewars;

import dev.reviewhop.integration.application.port.out.ProviderConnector;
import dev.reviewhop.integration.application.port.out.ProviderFailure;
import dev.reviewhop.integration.domain.CompletedChallenge;
import dev.reviewhop.integration.domain.CompletedChallengePage;
import dev.reviewhop.integration.domain.Provider;
import dev.reviewhop.integration.domain.ProviderCapability;
import dev.reviewhop.integration.domain.ProviderProfile;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class CodewarsConnector implements ProviderConnector {

    private static final Set<ProviderCapability> CAPABILITIES = Set.of(
            ProviderCapability.PUBLIC_PROFILE,
            ProviderCapability.MANUAL_IMPORT,
            ProviderCapability.INCREMENTAL_SYNC);

    private final RestClient restClient;
    private final URI publicSiteBaseUrl;
    private final MeterRegistry meterRegistry;

    public CodewarsConnector(
            RestClient restClient,
            URI publicSiteBaseUrl,
            MeterRegistry meterRegistry) {
        this.restClient = restClient;
        this.publicSiteBaseUrl = publicSiteBaseUrl;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Provider provider() {
        return Provider.CODEWARS;
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public ProviderProfile resolvePublicProfile(String externalUserId) {
        return measured("profile", () -> {
            var response = get("/users/{user}", CodewarsUserResponse.class, externalUserId);
            if (response == null || response.username() == null || response.username().isBlank()) {
                throw invalidResponse("Codewars returned a profile without a username");
            }
            return new ProviderProfile(response.username().trim());
        });
    }

    @Override
    public CompletedChallengePage fetchCompletedChallenges(
            String externalUserId, int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        return measured("completed_challenges", () -> {
            var response = get(
                    "/users/{user}/code-challenges/completed?page={page}",
                    CodewarsCompletedResponse.class,
                    externalUserId,
                    pageNumber);
            if (response == null || response.data() == null
                    || response.totalPages() < 0 || response.totalItems() < 0) {
                throw invalidResponse("Codewars returned invalid pagination metadata");
            }
            var challenges = response.data().stream()
                    .map(this::toChallenge)
                    .toList();
            return new CompletedChallengePage(
                    pageNumber, response.totalPages(), response.totalItems(), challenges);
        });
    }

    private CompletedChallenge toChallenge(CodewarsCompletedChallenge response) {
        if (response == null
                || blank(response.id())
                || blank(response.name())
                || blank(response.slug())
                || blank(response.completedAt())) {
            throw invalidResponse("Codewars returned an incomplete challenge");
        }
        if (response.id().length() > 512 || response.name().length() > 200
                || !response.slug().matches("[A-Za-z0-9-]+")) {
            throw invalidResponse("Codewars returned challenge fields outside accepted limits");
        }
        final Instant completedAt;
        try {
            completedAt = Instant.parse(response.completedAt());
        } catch (DateTimeParseException exception) {
            throw invalidResponse("Codewars returned an invalid completion timestamp");
        }
        return new CompletedChallenge(
                response.id().trim(),
                response.name().trim(),
                publicSiteBaseUrl.resolve("/kata/" + response.slug().trim()),
                completedAt);
    }

    private <T> T get(String path, Class<T> responseType, Object... uriVariables) {
        try {
            return restClient.get()
                    .uri(path, uriVariables)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw fromStatus(
                                response.getStatusCode().value(),
                                response.getHeaders().getFirst("Retry-After"));
                    })
                    .body(responseType);
        } catch (ProviderFailure failure) {
            throw failure;
        } catch (ResourceAccessException exception) {
            throw new ProviderFailure(
                    isTimeout(exception) ? "TIMEOUT" : "NETWORK_ERROR",
                    isTimeout(exception)
                            ? "Codewars did not respond before the configured timeout"
                            : "Codewars could not be reached",
                    true,
                    null);
        } catch (RestClientException exception) {
            throw invalidResponse("Codewars returned a response that could not be read");
        }
    }

    private <T> T measured(String operation, Supplier<T> request) {
        var startedAt = System.nanoTime();
        try {
            var result = request.get();
            record(operation, "success", startedAt);
            return result;
        } catch (ProviderFailure failure) {
            record(
                    operation,
                    failure.code().toLowerCase(java.util.Locale.ROOT),
                    startedAt);
            throw failure;
        }
    }

    private void record(String operation, String outcome, long startedAt) {
        meterRegistry.counter(
                "ankileet.provider.requests",
                "provider", "codewars",
                "operation", operation,
                "outcome", outcome).increment();
        meterRegistry.timer(
                "ankileet.provider.request.duration",
                "provider", "codewars",
                "operation", operation,
                "outcome", outcome)
                .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
    }

    private static ProviderFailure fromStatus(int status, String retryAfterHeader) {
        if (status == 404) {
            return new ProviderFailure(
                    "PROFILE_NOT_FOUND", "The public Codewars profile was not found", false, null);
        }
        if (status == 429) {
            return new ProviderFailure(
                    "RATE_LIMITED",
                    "Codewars rate limit reached; try again later",
                    true,
                    parseRetryAfter(retryAfterHeader));
        }
        if (status >= 500) {
            return new ProviderFailure(
                    "PROVIDER_UNAVAILABLE",
                    "Codewars is temporarily unavailable",
                    true,
                    null);
        }
        return new ProviderFailure(
                "PROVIDER_REJECTED_REQUEST",
                "Codewars rejected the request with status " + status,
                false,
                null);
    }

    private static Duration parseRetryAfter(String header) {
        if (header == null) {
            return null;
        }
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(header.trim())));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static ProviderFailure invalidResponse(String message) {
        return new ProviderFailure("INVALID_RESPONSE", message, false, null);
    }

    private static boolean isTimeout(Throwable throwable) {
        var current = throwable;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record CodewarsUserResponse(String username) {
    }

    private record CodewarsCompletedResponse(
            int totalPages,
            long totalItems,
            List<CodewarsCompletedChallenge> data) {
    }

    private record CodewarsCompletedChallenge(
            String id,
            String name,
            String slug,
            String completedAt) {
    }
}
