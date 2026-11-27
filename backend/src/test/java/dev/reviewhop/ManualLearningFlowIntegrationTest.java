package dev.reviewhop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
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
class ManualLearningFlowIntegrationTest {

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
    void protectsTheApiAndKeepsReviewDataInsideTheOwningUserBoundary() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/due"))
                .andExpect(status().isUnauthorized());

        var creationKey = UUID.randomUUID();
        var firstCompletedAt = Instant.now().minusSeconds(60);
        var createdBody = mockMvc.perform(post("/api/v1/learning-items/manual")
                        .with(user("alice"))
                        .header("Idempotency-Key", creationKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Two Sum",
                                  "sourceUrl": "https://leetcode.com/problems/two-sum/",
                                  "difficulty": "EASY",
                                  "tags": ["arrays", "hash-map"],
                                  "notes": "Remember the complement map.",
                                  "recallPrompt": "Which two numbers add up to the target?",
                                  "firstCompletedAt": "%s"
                                }
                                """.formatted(firstCompletedAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createdBody);
        var reviewCardId = created.required("reviewCardId").asText();

        var repeatedCreationBody = mockMvc.perform(post("/api/v1/learning-items/manual")
                        .with(user("alice"))
                        .header("Idempotency-Key", creationKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Ignored retry payload",
                                  "sourceUrl": "https://example.com/ignored",
                                  "tags": [],
                                  "firstCompletedAt": "%s"
                                }
                                """.formatted(firstCompletedAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(repeatedCreationBody).required("reviewCardId").asText())
                .isEqualTo(reviewCardId);

        var bobDue = mockMvc.perform(get("/api/v1/reviews/due").with(user("bob")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(bobDue).isEmpty()).isTrue();

        var completionKey = UUID.randomUUID();
        var startedAt = Instant.now().minusSeconds(30);
        mockMvc.perform(post("/api/v1/reviews/{id}/completions", reviewCardId)
                        .with(user("bob"))
                        .header("Idempotency-Key", completionKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":"GOOD","startedAt":"%s"}
                                """.formatted(startedAt)))
                .andExpect(status().isNotFound());

        var aliceDue = mockMvc.perform(get("/api/v1/reviews/due").with(user("alice")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var dueCard = objectMapper.readTree(aliceDue).required(0);
        assertThat(dueCard.required("reviewCardId").asText()).isEqualTo(reviewCardId);
        assertThat(dueCard.required("title").asText()).isEqualTo("Two Sum");
        assertThat(dueCard.required("recallPrompt").asText())
                .isEqualTo("Which two numbers add up to the target?");
        assertThat(dueCard.required("approachNotes").asText()).isEqualTo("Remember the complement map.");

        var completionBody = mockMvc.perform(post("/api/v1/reviews/{id}/completions", reviewCardId)
                        .with(user("alice"))
                        .header("Idempotency-Key", completionKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": "GOOD",
                                  "startedAt": "%s",
                                  "note": "Recalled without hints."
                                }
                                """.formatted(startedAt)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var reviewLogId = objectMapper.readTree(completionBody).required("reviewLogId").asText();

        var repeatedCompletion = mockMvc.perform(post(
                                "/api/v1/reviews/{id}/completions", reviewCardId)
                        .with(user("alice"))
                        .header("Idempotency-Key", completionKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":"AGAIN","startedAt":"%s"}
                                """.formatted(startedAt)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(repeatedCompletion).required("reviewLogId").asText())
                .isEqualTo(reviewLogId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_log WHERE idempotency_key = ?",
                Long.class,
                completionKey)).isEqualTo(1L);
        var persistedScheduler = jdbcTemplate.queryForMap(
                """
                SELECT scheduler_version, desired_retention,
                       previous_scheduler_state, next_scheduler_state,
                       next_stability, next_scheduler_difficulty,
                       next_last_reviewed_at
                FROM review_log
                WHERE id = ?
                """,
                UUID.fromString(reviewLogId));
        assertThat(persistedScheduler.get("scheduler_version"))
                .isEqualTo("fsrs-6-java-1.0.0-defaults-v1");
        assertThat(((Number) persistedScheduler.get("desired_retention")).doubleValue())
                .isEqualTo(0.90);
        assertThat(persistedScheduler.get("previous_scheduler_state")).isEqualTo("LEARNING");
        assertThat(persistedScheduler.get("next_scheduler_state")).isEqualTo("REVIEW");
        assertThat(((Number) persistedScheduler.get("next_stability")).doubleValue()).isPositive();
        assertThat(((Number) persistedScheduler.get("next_scheduler_difficulty")).doubleValue())
                .isBetween(1.0, 10.0);
        assertThat(persistedScheduler.get("next_last_reviewed_at")).isNotNull();

        var history = mockMvc.perform(get("/api/v1/reviews/{id}/history", reviewCardId)
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(history).contains(reviewLogId).contains("Recalled without hints.");

        var recentHistory = mockMvc.perform(get("/api/v1/reviews/history").with(user("alice")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(recentHistory).contains(reviewLogId).contains(reviewCardId).contains("Two Sum");

        var bobHistory = mockMvc.perform(get("/api/v1/reviews/history").with(user("bob")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(bobHistory).isEmpty()).isTrue();

        mockMvc.perform(get("/api/v1/reviews/{id}/history", reviewCardId).with(user("bob")))
                .andExpect(status().isNotFound());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(
            String subject) {
        return jwt().jwt(token -> token
                .issuer(ISSUER)
                .subject(subject)
                .audience(java.util.List.of("ankileet-api")));
    }
}
