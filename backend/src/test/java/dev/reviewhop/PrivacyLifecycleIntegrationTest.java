package dev.reviewhop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
class PrivacyLifecycleIntegrationTest {

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
    void exportsOnlyOwnedPortableDataAndDeletesTheCompleteUserGraphIdempotently()
            throws Exception {
        mockMvc.perform(get("/api/v1/users/me/export"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        var aliceReviewCardId = createManualItem("alice", "Alice note", "alice-item");
        createManualItem("bob", "Bob note", "bob-item");
        completeReview("alice", aliceReviewCardId);
        var aliceId = userId("alice");
        var bobId = userId("bob");
        addCompletedSyncGraph(aliceId);

        var exportBody = mockMvc.perform(get("/api/v1/users/me/export").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reviewhop-data-export.json\""))
                .andReturn().getResponse().getContentAsString();

        JsonNode export = objectMapper.readTree(exportBody);
        assertThat(export.required("formatVersion").asInt()).isEqualTo(1);
        assertThat(export.required("profile").required("timezone").asText())
                .isEqualTo("Europe/Berlin");
        assertThat(export.required("learningItems").toString())
                .contains("Alice note")
                .contains("Recall prompt for alice-item")
                .doesNotContain("Bob note", "Recall prompt for bob-item");
        assertThat(export.required("providerConnections").size()).isEqualTo(1);
        assertThat(export.required("reviewLogs").size()).isEqualTo(1);
        assertThat(export.required("reviewLogs").toString()).contains("Private review note");
        assertThat(export.required("syncJobs").size()).isEqualTo(1);
        assertThat(exportBody)
                .doesNotContain("oidcIssuer")
                .doesNotContain("oidcSubject")
                .doesNotContain("idempotencyKey")
                .doesNotContain("traceparent")
                .doesNotContain("outbox");

        mockMvc.perform(delete("/api/v1/users/me")
                        .with(user("alice"))
                        .header("X-Account-Deletion-Confirmation", "wrong"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/users/me")
                        .with(user("alice"))
                        .header("X-Account-Deletion-Confirmation", "DELETE"))
                .andExpect(status().isNoContent());

        assertUserGraphDeleted(aliceId);
        assertThat(count("app_user", bobId)).isEqualTo(1);
        assertThat(count("learning_item", bobId)).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/users/me")
                        .with(user("alice"))
                        .header("X-Account-Deletion-Confirmation", "DELETE"))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE oidc_subject = 'alice'",
                Long.class)).isZero();
    }

    private String createManualItem(String subject, String notes, String title) throws Exception {
        var response = mockMvc.perform(post("/api/v1/learning-items/manual")
                        .with(user(subject))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "sourceUrl": "https://example.com/%s",
                                  "tags": ["privacy-test"],
                                  "notes": "%s",
                                  "recallPrompt": "Recall prompt for %s",
                                  "firstCompletedAt": "%s"
                                }
                                """.formatted(title, title, notes, title, Instant.now().minusSeconds(60))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).required("reviewCardId").asText();
    }

    private void completeReview(String subject, String reviewCardId) throws Exception {
        mockMvc.perform(post("/api/v1/reviews/{id}/completions", reviewCardId)
                        .with(user(subject))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": "GOOD",
                                  "startedAt": "%s",
                                  "note": "Private review note"
                                }
                                """.formatted(Instant.now().minusSeconds(30))))
                .andExpect(status().isOk());
    }

    private UUID userId(String subject) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE oidc_subject = ?",
                UUID.class,
                subject);
    }

    private void addCompletedSyncGraph(UUID userId) {
        var connectionId = UUID.randomUUID();
        var syncJobId = UUID.randomUUID();
        var now = Instant.now();
        var timestamp = Timestamp.from(now);
        jdbcTemplate.update(
                """
                INSERT INTO provider_connection (
                    id, user_id, provider, mode, status, verification_status,
                    external_user_id, created_at, updated_at
                ) VALUES (?, ?, 'CODEWARS', 'PUBLIC_PROFILE', 'ACTIVE',
                          'UNVERIFIED', 'alice-codewars', ?, ?)
                """,
                connectionId,
                userId,
                timestamp,
                timestamp);
        jdbcTemplate.update(
                """
                INSERT INTO sync_job (
                    id, connection_id, user_id, idempotency_key, status,
                    imported_count, existing_count, pages_processed,
                    started_at, completed_at
                ) VALUES (?, ?, ?, ?, 'SUCCEEDED', 2, 1, 1, ?, ?)
                """,
                syncJobId,
                connectionId,
                userId,
                UUID.randomUUID(),
                timestamp,
                timestamp);
        jdbcTemplate.update(
                """
                INSERT INTO sync_outbox_event (
                    id, aggregate_id, event_type, payload_version, payload,
                    status, attempt_count, available_at, correlation_id,
                    created_at, completed_at
                ) VALUES (?, ?, 'SyncRequested', 1, CAST(? AS jsonb),
                          'COMPLETED', 1, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                syncJobId,
                "{\"syncJobId\":\"" + syncJobId + "\"}",
                timestamp,
                UUID.randomUUID(),
                timestamp,
                timestamp);
    }

    private void assertUserGraphDeleted(UUID userId) {
        assertThat(count("app_user", userId)).isZero();
        assertThat(count("provider_connection", userId)).isZero();
        assertThat(count("sync_job", userId)).isZero();
        assertThat(count("learning_item", userId)).isZero();
        assertThat(count("review_log", userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sync_outbox_event",
                Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM review_card rc
                JOIN learning_item li ON li.id = rc.learning_item_id
                WHERE li.user_id = ?
                """,
                Long.class,
                userId)).isZero();
    }

    private long count(String table, UUID userId) {
        var allowedTable = switch (table) {
            case "app_user", "provider_connection", "sync_job", "learning_item", "review_log" ->
                    table;
            default -> throw new IllegalArgumentException("Unexpected table");
        };
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + allowedTable + " WHERE "
                        + ("app_user".equals(allowedTable) ? "id" : "user_id") + " = ?",
                Long.class,
                userId);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(
            String subject) {
        return jwt().jwt(token -> token
                .issuer(ISSUER)
                .subject(subject)
                .audience(java.util.List.of("ankileet-api")));
    }
}
