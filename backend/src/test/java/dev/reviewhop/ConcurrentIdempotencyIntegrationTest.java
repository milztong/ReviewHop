package dev.reviewhop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
class ConcurrentIdempotencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 8;
    private static final String ISSUER = "http://localhost:8180/realms/ankileet";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17.12-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentRetriesCreateExactlyOneLearningItemAndReviewLog() throws Exception {
        var creationKey = UUID.randomUUID();
        var firstCompletedAt = Instant.now().minusSeconds(60);
        var createdBodies = runConcurrently(() -> mockMvc.perform(
                        post("/api/v1/learning-items/manual")
                                .with(user("concurrent-user"))
                                .header("Idempotency-Key", creationKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Concurrent Two Sum",
                                          "sourceUrl": "https://example.com/concurrent-two-sum",
                                          "tags": ["concurrency"],
                                          "firstCompletedAt": "%s"
                                        }
                                        """.formatted(firstCompletedAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        var reviewCardIds = createdBodies.stream()
                .map(this::reviewCardId)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(reviewCardIds).hasSize(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM learning_item", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_card", Long.class)).isEqualTo(1L);

        var reviewCardId = reviewCardIds.iterator().next();
        var completionKey = UUID.randomUUID();
        var startedAt = Instant.now().minusSeconds(30);
        var completionBodies = runConcurrently(() -> mockMvc.perform(
                        post("/api/v1/reviews/{id}/completions", reviewCardId)
                                .with(user("concurrent-user"))
                                .header("Idempotency-Key", completionKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "rating": "GOOD",
                                          "startedAt": "%s"
                                        }
                                        """.formatted(startedAt)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        var reviewLogIds = completionBodies.stream()
                .map(this::reviewLogId)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(reviewLogIds).hasSize(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_log WHERE idempotency_key = ?",
                Long.class,
                completionKey)).isEqualTo(1L);
    }

    private List<String> runConcurrently(Callable<String> request) throws Exception {
        var ready = new CountDownLatch(CONCURRENT_REQUESTS);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        try {
            var futures = java.util.stream.IntStream.range(0, CONCURRENT_REQUESTS)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Concurrent requests did not start in time");
                        }
                        return request.call();
                    }))
                    .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return futures.stream().map(future -> {
                try {
                    return future.get(20, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException("Concurrent request failed", exception);
                }
            }).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private String reviewCardId(String body) {
        return field(body, "reviewCardId");
    }

    private String reviewLogId(String body) {
        return field(body, "reviewLogId");
    }

    private String field(String body, String name) {
        try {
            return objectMapper.readTree(body).required(name).asText();
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid response body", exception);
        }
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(
            String subject) {
        return jwt().jwt(token -> token
                .issuer(ISSUER)
                .subject(subject)
                .audience(List.of("ankileet-api")));
    }
}
