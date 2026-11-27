package dev.reviewhop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reviewhop.integration.application.ProcessNextSyncWork;
import dev.reviewhop.integration.application.port.out.ProviderConnector;
import dev.reviewhop.integration.application.port.out.ProviderFailure;
import dev.reviewhop.integration.application.port.out.SyncWorkQueue;
import dev.reviewhop.integration.domain.CompletedChallenge;
import dev.reviewhop.integration.domain.CompletedChallengePage;
import dev.reviewhop.integration.domain.Provider;
import dev.reviewhop.integration.domain.ProviderCapability;
import dev.reviewhop.integration.domain.ProviderProfile;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
class CodewarsSyncIntegrationTest {

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

    @Autowired
    private ProcessNextSyncWork processNextSyncWork;

    @Autowired
    private SyncWorkQueue syncWorkQueue;

    @MockitoBean
    private ProviderConnector connector;

    @BeforeEach
    void cleanIntegrationData() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    sync_outbox_event,
                    sync_job,
                    provider_connection,
                    review_log,
                    review_card,
                    learning_item,
                    source_item,
                    app_user
                CASCADE
                """);
    }

    @Test
    void importsAllPagesIdempotentlyAndEnforcesTheOwningUserBoundary() throws Exception {
        when(connector.provider()).thenReturn(Provider.CODEWARS);
        when(connector.capabilities()).thenReturn(Set.of(
                ProviderCapability.PUBLIC_PROFILE,
                ProviderCapability.MANUAL_IMPORT,
                ProviderCapability.INCREMENTAL_SYNC));
        when(connector.resolvePublicProfile(anyString()))
                .thenReturn(new ProviderProfile("AliceCanonical"));
        when(connector.fetchCompletedChallenges("AliceCanonical", 0))
                .thenReturn(page(0, 2, challenge(
                        "kata-1", "Valid Braces", "valid-braces", "2026-11-20T10:00:00Z")));
        when(connector.fetchCompletedChallenges("AliceCanonical", 1))
                .thenReturn(page(1, 2, challenge(
                        "kata-2", "Two Sum", "two-sum", "2026-11-19T10:00:00Z")));

        var connectionBody = mockMvc.perform(post("/api/v1/provider-connections/codewars")
                        .with(user("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode connection = objectMapper.readTree(connectionBody);
        var connectionId = connection.required("id").asText();
        assertThat(connection.required("verificationStatus").asText()).isEqualTo("UNVERIFIED");

        var firstKey = UUID.randomUUID();
        var firstSync = synchronize("alice", connectionId, firstKey);
        assertThat(firstSync.required("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(firstSync.required("importedCount").asInt()).isEqualTo(2);
        assertThat(firstSync.required("pagesProcessed").asInt()).isEqualTo(2);

        var repeatedIdempotentSync = synchronize("alice", connectionId, firstKey);
        assertThat(repeatedIdempotentSync.required("id").asText())
                .isEqualTo(firstSync.required("id").asText());

        when(connector.resolvePublicProfile("second"))
                .thenReturn(new ProviderProfile("SecondProfile"));
        var secondConnectionBody = mockMvc.perform(post("/api/v1/provider-connections/codewars")
                        .with(user("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"second\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var secondConnectionId = objectMapper.readTree(secondConnectionBody)
                .required("id").asText();
        mockMvc.perform(post(
                                "/api/v1/provider-connections/{id}/sync-jobs",
                                secondConnectionId)
                        .with(user("alice"))
                        .header("Idempotency-Key", firstKey))
                .andExpect(status().isConflict());

        var secondSync = synchronize("alice", connectionId, UUID.randomUUID());
        assertThat(secondSync.required("importedCount").asInt()).isZero();
        assertThat(secondSync.required("existingCount").asInt()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM source_item
                WHERE provider = 'CODEWARS' AND external_id IN ('kata-1', 'kata-2')
                """,
                Long.class))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM learning_item li
                JOIN app_user au ON au.id = li.user_id
                WHERE au.oidc_subject = 'alice'
                """,
                Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM review_card rc
                JOIN learning_item li ON li.id = rc.learning_item_id
                JOIN app_user au ON au.id = li.user_id
                WHERE au.oidc_subject = 'alice'
                """,
                Long.class)).isEqualTo(2L);

        mockMvc.perform(post("/api/v1/provider-connections/{id}/sync-jobs", connectionId)
                        .with(user("bob"))
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(
                                "/api/v1/provider-connections/{connectionId}/sync-jobs/{syncJobId}",
                                connectionId,
                                firstSync.required("id").asText())
                        .with(user("bob")))
                .andExpect(status().isNotFound());
        var bobConnections = mockMvc.perform(get("/api/v1/provider-connections")
                        .with(user("bob")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(bobConnections).isEmpty()).isTrue();
    }

    @Test
    void leasesEachOutboxEventOnceAndRedeliversItAfterLeaseExpiry() throws Exception {
        when(connector.provider()).thenReturn(Provider.CODEWARS);
        when(connector.resolvePublicProfile(anyString()))
                .thenReturn(new ProviderProfile("LeaseProfile"));

        var connectionBody = mockMvc.perform(post("/api/v1/provider-connections/codewars")
                        .with(user("lease-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lease\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var connectionId = objectMapper.readTree(connectionBody).required("id").asText();
        var accepted = requestSync("lease-user", connectionId, UUID.randomUUID());
        var now = Instant.now();

        var firstLease = syncWorkQueue.claimNext(
                "worker-one", now, java.time.Duration.ofMinutes(10));
        assertThat(firstLease).isPresent();
        assertThat(syncWorkQueue.claimNext(
                "worker-two", now, java.time.Duration.ofMinutes(10))).isEmpty();

        jdbcTemplate.update(
                "UPDATE sync_outbox_event SET lease_expires_at = ? WHERE aggregate_id = ?",
                java.sql.Timestamp.from(now.minusSeconds(1)),
                UUID.fromString(accepted.required("id").asText()));
        var redelivery = syncWorkQueue.claimNext(
                "worker-two", now, java.time.Duration.ofMinutes(10));

        assertThat(redelivery).isPresent();
        assertThat(redelivery.orElseThrow().eventId())
                .isEqualTo(firstLease.orElseThrow().eventId());
        assertThat(redelivery.orElseThrow().attempt()).isEqualTo(2);
        assertThat(redelivery.orElseThrow().leaseToken())
                .isNotEqualTo(firstLease.orElseThrow().leaseToken());
    }

    @Test
    void keepsTheOldCursorAndClassifiesAPartialProviderFailure() throws Exception {
        when(connector.provider()).thenReturn(Provider.CODEWARS);
        when(connector.resolvePublicProfile(anyString()))
                .thenReturn(new ProviderProfile("FailureProfile"));
        when(connector.fetchCompletedChallenges("FailureProfile", 0))
                .thenReturn(page(0, 2, challenge(
                        "partial-1", "Imported First", "imported-first", "2026-11-20T10:00:00Z")));
        when(connector.fetchCompletedChallenges("FailureProfile", 1))
                .thenThrow(new ProviderFailure(
                        "RATE_LIMITED", "Codewars rate limit reached", true,
                        java.time.Duration.ofSeconds(20)));

        var connectionBody = mockMvc.perform(post("/api/v1/provider-connections/codewars")
                        .with(user("failure-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"failure\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var connectionId = objectMapper.readTree(connectionBody).required("id").asText();
        var failedSync = synchronize("failure-user", connectionId, UUID.randomUUID());

        assertThat(failedSync.required("status").asText()).isEqualTo("RETRY_SCHEDULED");
        assertThat(failedSync.required("failureCode").asText()).isEqualTo("RATE_LIMITED");
        assertThat(failedSync.required("retryAfterSeconds").asLong()).isEqualTo(20L);
        assertThat(failedSync.required("importedCount").asInt()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cursor_value FROM provider_connection WHERE id = ?",
                String.class,
                UUID.fromString(connectionId))).isNull();
    }

    @Test
    void deadLettersAfterBoundedRetriesAndAllowsOnlyAnOperatorToRetry() throws Exception {
        when(connector.provider()).thenReturn(Provider.CODEWARS);
        when(connector.resolvePublicProfile(anyString()))
                .thenReturn(new ProviderProfile("DeadLetterProfile"));
        when(connector.fetchCompletedChallenges("DeadLetterProfile", 0))
                .thenThrow(new ProviderFailure(
                        "PROVIDER_UNAVAILABLE",
                        "Provider temporarily unavailable",
                        true,
                        null));

        var connectionBody = mockMvc.perform(post("/api/v1/provider-connections/codewars")
                        .with(user("dead-letter-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dead-letter\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var connectionId = objectMapper.readTree(connectionBody).required("id").asText();
        var accepted = requestSync(
                "dead-letter-user", connectionId, UUID.randomUUID());
        var syncJobId = accepted.required("id").asText();

        for (var attempt = 1; attempt <= 5; attempt++) {
            assertThat(processNextSyncWork.execute("test-worker")).isTrue();
            if (attempt < 5) {
                jdbcTemplate.update(
                        """
                        UPDATE sync_outbox_event
                        SET available_at = now() - interval '1 second'
                        WHERE aggregate_id = ?
                        """,
                        UUID.fromString(syncJobId));
            }
        }

        var deadLettered = getSyncJob("dead-letter-user", connectionId, syncJobId);
        assertThat(deadLettered.required("status").asText()).isEqualTo("DEAD_LETTERED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM sync_outbox_event WHERE aggregate_id = ?",
                Integer.class,
                UUID.fromString(syncJobId))).isEqualTo(5);

        mockMvc.perform(post("/api/v1/operations/sync-jobs/{id}/retry", syncJobId)
                        .with(user("dead-letter-user")))
                .andExpect(status().isForbidden());

        var retriedBody = mockMvc.perform(post(
                                "/api/v1/operations/sync-jobs/{id}/retry", syncJobId)
                        .with(jwt().jwt(token -> token
                                        .issuer(ISSUER)
                                        .subject("operator")
                                        .audience(List.of("ankileet-api")))
                                .authorities(new SimpleGrantedAuthority(
                                        "ROLE_ankileet-operator"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(retriedBody).required("status").asText())
                .isEqualTo("REQUESTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM sync_outbox_event WHERE aggregate_id = ?",
                Integer.class,
                UUID.fromString(syncJobId))).isZero();
    }

    private JsonNode synchronize(String subject, String connectionId, UUID idempotencyKey)
            throws Exception {
        var accepted = requestSync(subject, connectionId, idempotencyKey);
        processNextSyncWork.execute("test-worker");
        return getSyncJob(
                subject, connectionId, accepted.required("id").asText());
    }

    private JsonNode requestSync(
            String subject, String connectionId, UUID idempotencyKey) throws Exception {
        var body = mockMvc.perform(post(
                                "/api/v1/provider-connections/{id}/sync-jobs", connectionId)
                        .with(user(subject))
                        .header("Idempotency-Key", idempotencyKey)
                        .header(
                                "traceparent",
                                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode getSyncJob(
            String subject, String connectionId, String syncJobId) throws Exception {
        var body = mockMvc.perform(get(
                                "/api/v1/provider-connections/{connectionId}/sync-jobs/{syncJobId}",
                                connectionId,
                                syncJobId)
                        .with(user(subject)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static CompletedChallengePage page(
            int number, int totalPages, CompletedChallenge... challenges) {
        return new CompletedChallengePage(
                number, totalPages, challenges.length, List.of(challenges));
    }

    private static CompletedChallenge challenge(
            String id, String title, String slug, String completedAt) {
        return new CompletedChallenge(
                id,
                title,
                URI.create("https://www.codewars.com/kata/" + slug),
                Instant.parse(completedAt));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(
            String subject) {
        return jwt().jwt(token -> token
                .issuer(ISSUER)
                .subject(subject)
                .audience(List.of("ankileet-api")));
    }
}
