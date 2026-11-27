package dev.reviewhop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "ankileet.security.rate-limit.read-requests-per-window=2",
    "ankileet.security.rate-limit.write-requests-per-window=1",
    "ankileet.security.rate-limit.window=PT1M"
})
class RateLimitIntegrationTest {

    private static final String ISSUER = "http://localhost:8180/realms/ankileet";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17.12-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void limitsAuthenticatedReadsAndWritesIndependentlyWithoutLimitingStatus() throws Exception {
        for (var request = 0; request < 3; request++) {
            mockMvc.perform(get("/api/v1/system/status"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/reviews/due").with(user("read-user")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "1"));
        mockMvc.perform(get("/api/v1/reviews/due").with(user("read-user")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
        var rejected = mockMvc.perform(get("/api/v1/reviews/due").with(user("read-user")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Rate limit exceeded"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber())
                .andReturn();
        assertThat(Integer.parseInt(rejected.getResponse().getHeader("Retry-After")))
                .isBetween(1, 60);

        mockMvc.perform(post("/api/v1/learning-items/manual")
                        .with(user("write-user"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualItem("Allowed write")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/learning-items/manual")
                        .with(user("write-user"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualItem("Rejected write")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "1"));

        assertThat(meterRegistry.counter(
                "ankileet.security.rate_limit.requests",
                "access", "read",
                "outcome", "rejected").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter(
                "ankileet.security.rate_limit.requests",
                "access", "write",
                "outcome", "rejected").count()).isEqualTo(1.0);
    }

    private static String manualItem(String title) {
        return """
                {
                  "title": "%s",
                  "sourceUrl": "https://example.com/rate-limit",
                  "tags": [],
                  "firstCompletedAt": "%s"
                }
                """.formatted(title, Instant.now().minusSeconds(60));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(
            String subject) {
        return jwt().jwt(token -> token
                .issuer(ISSUER)
                .subject(subject)
                .audience(List.of("ankileet-api")));
    }
}
