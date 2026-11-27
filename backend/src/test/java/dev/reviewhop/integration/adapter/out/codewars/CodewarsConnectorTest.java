package dev.reviewhop.integration.adapter.out.codewars;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.reviewhop.integration.application.port.out.ProviderFailure;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class CodewarsConnectorTest {

    private WireMockServer wireMock;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        meterRegistry = new SimpleMeterRegistry();
    }

    @AfterEach
    void stopWireMock() {
        meterRegistry.close();
        wireMock.stop();
    }

    @Test
    void mapsPublicProfileAndCompletedChallengePages() {
        wireMock.stubFor(get(urlPathEqualTo("/users/alice"))
                .willReturn(json("""
                        {"username":"AliceCanonical"}
                        """)));
        wireMock.stubFor(get(urlPathEqualTo(
                        "/users/AliceCanonical/code-challenges/completed"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(json("""
                        {
                          "totalPages": 2,
                          "totalItems": 201,
                          "data": [{
                            "id": "kata-1",
                            "name": "Valid Braces",
                            "slug": "valid-braces",
                            "completedAt": "2026-11-20T10:15:30Z",
                            "completedLanguages": ["java", "java", "typescript"]
                          }]
                        }
                        """)));

        var connector = connector(Duration.ofSeconds(1));
        var profile = connector.resolvePublicProfile("alice");
        var page = connector.fetchCompletedChallenges(profile.externalUserId(), 1);

        assertThat(profile.externalUserId()).isEqualTo("AliceCanonical");
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.totalItems()).isEqualTo(201);
        assertThat(page.challenges()).singleElement().satisfies(challenge -> {
            assertThat(challenge.externalId()).isEqualTo("kata-1");
            assertThat(challenge.sourceUrl()).isEqualTo(URI.create(
                    "https://www.codewars.com/kata/valid-braces"));
        });
        assertThat(meterRegistry.counter(
                "ankileet.provider.requests",
                "provider", "codewars",
                "operation", "completed_challenges",
                "outcome", "success").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer(
                "ankileet.provider.request.duration",
                "provider", "codewars",
                "operation", "completed_challenges",
                "outcome", "success").count()).isEqualTo(1L);
    }

    @Test
    void classifiesRateLimitsWithRetryAfter() {
        wireMock.stubFor(get(urlPathEqualTo("/users/alice"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "30")));

        assertThatThrownBy(() -> connector(Duration.ofSeconds(1))
                .resolvePublicProfile("alice"))
                .isInstanceOfSatisfying(ProviderFailure.class, failure -> {
                    assertThat(failure.code()).isEqualTo("RATE_LIMITED");
                    assertThat(failure.retryable()).isTrue();
                    assertThat(failure.retryAfter()).isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void classifiesTimeoutsAndServerFailuresAsRetryable() {
        wireMock.stubFor(get(urlPathEqualTo("/users/slow"))
                .willReturn(json("{\"username\":\"slow\"}")
                        .withFixedDelay(250)));
        wireMock.stubFor(get(urlPathEqualTo("/users/broken"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> connector(Duration.ofMillis(30))
                .resolvePublicProfile("slow"))
                .isInstanceOfSatisfying(ProviderFailure.class, failure -> {
                    assertThat(failure.code()).isEqualTo("TIMEOUT");
                    assertThat(failure.retryable()).isTrue();
                });
        assertThatThrownBy(() -> connector(Duration.ofSeconds(1))
                .resolvePublicProfile("broken"))
                .isInstanceOfSatisfying(ProviderFailure.class, failure -> {
                    assertThat(failure.code()).isEqualTo("PROVIDER_UNAVAILABLE");
                    assertThat(failure.retryable()).isTrue();
                });
    }

    @Test
    void rejectsMalformedProviderDataWithoutImportingIt() {
        wireMock.stubFor(get(urlPathEqualTo("/users/alice/code-challenges/completed"))
                .withQueryParam("page", equalTo("0"))
                .willReturn(json("""
                        {
                          "totalPages": 1,
                          "totalItems": 1,
                          "data": [{"id":"kata-1","name":"Missing fields"}]
                        }
                        """)));

        assertThatThrownBy(() -> connector(Duration.ofSeconds(1))
                .fetchCompletedChallenges("alice", 0))
                .isInstanceOfSatisfying(ProviderFailure.class, failure -> {
                    assertThat(failure.code()).isEqualTo("INVALID_RESPONSE");
                    assertThat(failure.retryable()).isFalse();
                });
    }

    private CodewarsConnector connector(Duration readTimeout) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(1));
        requestFactory.setReadTimeout(readTimeout);
        var restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(requestFactory)
                .build();
        return new CodewarsConnector(
                restClient, URI.create("https://www.codewars.com"), meterRegistry);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(
            String body) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }
}
