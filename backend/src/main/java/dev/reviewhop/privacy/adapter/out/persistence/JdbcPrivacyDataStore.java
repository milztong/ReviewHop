package dev.reviewhop.privacy.adapter.out.persistence;

import dev.reviewhop.privacy.application.AccountDataExport;
import dev.reviewhop.privacy.application.AccountDataExport.LearningItem;
import dev.reviewhop.privacy.application.AccountDataExport.Profile;
import dev.reviewhop.privacy.application.AccountDataExport.ProviderConnection;
import dev.reviewhop.privacy.application.AccountDataExport.ReviewCard;
import dev.reviewhop.privacy.application.AccountDataExport.ReviewLog;
import dev.reviewhop.privacy.application.AccountDataExport.SchedulingSnapshot;
import dev.reviewhop.privacy.application.AccountDataExport.SyncJob;
import dev.reviewhop.privacy.application.DeletedAccountData;
import dev.reviewhop.privacy.application.port.out.PrivacyDataStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
class JdbcPrivacyDataStore implements PrivacyDataStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcPrivacyDataStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AccountDataExport export(UUID userId, Instant exportedAt) {
        var profile = Objects.requireNonNull(jdbcTemplate.queryForObject(
                """
                SELECT timezone, desired_retention, created_at
                FROM app_user
                WHERE id = ?
                """,
                (resultSet, rowNumber) -> new Profile(
                        resultSet.getString("timezone"),
                        resultSet.getDouble("desired_retention"),
                        instant(resultSet, "created_at")),
                userId));

        return new AccountDataExport(
                1,
                exportedAt,
                profile,
                providerConnections(userId),
                learningItems(userId),
                reviewCards(userId),
                reviewLogs(userId),
                syncJobs(userId));
    }

    @Override
    @Transactional
    public DeletedAccountData delete(UUID userId) {
        var lockedUsers = jdbcTemplate.query(
                "SELECT id FROM app_user WHERE id = ? FOR UPDATE",
                (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
                userId);
        if (lockedUsers.isEmpty()) {
            return new DeletedAccountData(0, 0, 0, 0, 0, 0, 0, 0);
        }
        var sourceItemIds = jdbcTemplate.query(
                "SELECT source_item_id FROM learning_item WHERE user_id = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
                userId);

        var outboxEvents = jdbcTemplate.update(
                """
                DELETE FROM sync_outbox_event
                WHERE aggregate_id IN (SELECT id FROM sync_job WHERE user_id = ?)
                """,
                userId);
        var syncJobs = jdbcTemplate.update("DELETE FROM sync_job WHERE user_id = ?", userId);
        var providerConnections = jdbcTemplate.update(
                "DELETE FROM provider_connection WHERE user_id = ?", userId);
        var reviewLogs = jdbcTemplate.update("DELETE FROM review_log WHERE user_id = ?", userId);
        var reviewCards = jdbcTemplate.update(
                """
                DELETE FROM review_card
                WHERE learning_item_id IN (SELECT id FROM learning_item WHERE user_id = ?)
                """,
                userId);
        var learningItems = jdbcTemplate.update(
                "DELETE FROM learning_item WHERE user_id = ?", userId);
        var orphanedSourceItems = sourceItemIds.stream()
                .mapToInt(sourceItemId -> jdbcTemplate.update(
                        """
                        DELETE FROM source_item
                        WHERE id = ?
                          AND NOT EXISTS (
                              SELECT 1 FROM learning_item WHERE source_item_id = ?
                          )
                        """,
                        sourceItemId,
                        sourceItemId))
                .sum();
        var appUsers = jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", userId);

        return new DeletedAccountData(
                appUsers,
                providerConnections,
                syncJobs,
                outboxEvents,
                learningItems,
                reviewCards,
                reviewLogs,
                orphanedSourceItems);
    }

    private List<ProviderConnection> providerConnections(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, provider, mode, status, verification_status,
                       external_user_id, last_synced_at, created_at
                FROM provider_connection
                WHERE user_id = ?
                ORDER BY created_at, id
                """,
                (resultSet, rowNumber) -> new ProviderConnection(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("provider"),
                        resultSet.getString("mode"),
                        resultSet.getString("status"),
                        resultSet.getString("verification_status"),
                        resultSet.getString("external_user_id"),
                        nullableInstant(resultSet, "last_synced_at"),
                        instant(resultSet, "created_at")),
                userId);
    }

    private List<LearningItem> learningItems(UUID userId) {
        var tags = learningItemTags(userId);
        return jdbcTemplate.query(
                """
                SELECT li.id, si.provider,
                       CASE WHEN si.provider = 'MANUAL' THEN NULL ELSE si.external_id END
                           AS external_id,
                       si.title,
                       si.source_url, si.difficulty, li.notes, li.recall_prompt,
                       li.first_completed_at, li.status, li.created_at
                FROM learning_item li
                JOIN source_item si ON si.id = li.source_item_id
                WHERE li.user_id = ?
                ORDER BY li.created_at, li.id
                """,
                (resultSet, rowNumber) -> {
                    var learningItemId = resultSet.getObject("id", UUID.class);
                    return new LearningItem(
                            learningItemId,
                            resultSet.getString("provider"),
                            resultSet.getString("external_id"),
                            resultSet.getString("title"),
                            resultSet.getString("source_url"),
                            resultSet.getString("difficulty"),
                            List.copyOf(tags.getOrDefault(learningItemId, List.of())),
                            resultSet.getString("notes"),
                            resultSet.getString("recall_prompt"),
                            instant(resultSet, "first_completed_at"),
                            resultSet.getString("status"),
                            instant(resultSet, "created_at"));
                },
                userId);
    }

    private Map<UUID, List<String>> learningItemTags(UUID userId) {
        var tags = new HashMap<UUID, List<String>>();
        var rows = jdbcTemplate.query(
                """
                SELECT li.id AS learning_item_id, sit.tag
                FROM learning_item li
                JOIN source_item_tag sit ON sit.source_item_id = li.source_item_id
                WHERE li.user_id = ?
                ORDER BY sit.tag
                """,
                (resultSet, rowNumber) -> new LearningItemTag(
                        resultSet.getObject("learning_item_id", UUID.class),
                        resultSet.getString("tag")),
                userId);
        rows.forEach(row -> tags.computeIfAbsent(
                        row.learningItemId(), ignored -> new java.util.ArrayList<>())
                .add(row.tag()));
        return tags;
    }

    private List<ReviewCard> reviewCards(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT rc.id, rc.learning_item_id, rc.mode, rc.state, rc.due_at,
                       rc.scheduler_state, rc.scheduler_step, rc.stability,
                       rc.scheduler_difficulty, rc.last_reviewed_at
                FROM review_card rc
                JOIN learning_item li ON li.id = rc.learning_item_id
                WHERE li.user_id = ?
                ORDER BY rc.id
                """,
                (resultSet, rowNumber) -> new ReviewCard(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("learning_item_id", UUID.class),
                        resultSet.getString("mode"),
                        resultSet.getString("state"),
                        instant(resultSet, "due_at"),
                        snapshot(resultSet, "", "due_at")),
                userId);
    }

    private List<ReviewLog> reviewLogs(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, review_card_id, rating, reviewed_at, duration_ms,
                       note, scheduler_version, desired_retention,
                       previous_scheduler_state, previous_scheduler_step,
                       previous_stability, previous_scheduler_difficulty,
                       previous_last_reviewed_at, previous_due_at,
                       next_scheduler_state, next_scheduler_step,
                       next_stability, next_scheduler_difficulty,
                       next_last_reviewed_at, next_due_at
                FROM review_log
                WHERE user_id = ?
                ORDER BY reviewed_at, id
                """,
                (resultSet, rowNumber) -> new ReviewLog(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("review_card_id", UUID.class),
                        resultSet.getString("rating"),
                        instant(resultSet, "reviewed_at"),
                        resultSet.getLong("duration_ms"),
                        resultSet.getString("note"),
                        resultSet.getString("scheduler_version"),
                        resultSet.getObject("desired_retention", Double.class),
                        snapshot(resultSet, "previous_", "previous_due_at"),
                        snapshot(resultSet, "next_", "next_due_at")),
                userId);
    }

    private List<SyncJob> syncJobs(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, connection_id, status, imported_count,
                       existing_count, pages_processed, failure_code,
                       started_at, completed_at
                FROM sync_job
                WHERE user_id = ?
                ORDER BY started_at, id
                """,
                (resultSet, rowNumber) -> new SyncJob(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("connection_id", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getInt("imported_count"),
                        resultSet.getInt("existing_count"),
                        resultSet.getInt("pages_processed"),
                        resultSet.getString("failure_code"),
                        instant(resultSet, "started_at"),
                        nullableInstant(resultSet, "completed_at")),
                userId);
    }

    private static SchedulingSnapshot snapshot(
            ResultSet resultSet,
            String prefix,
            String dueAtColumn) throws SQLException {
        return new SchedulingSnapshot(
                resultSet.getString(prefix + "scheduler_state"),
                resultSet.getObject(prefix + "scheduler_step", Integer.class),
                resultSet.getObject(prefix + "stability", Double.class),
                resultSet.getObject(prefix + "scheduler_difficulty", Double.class),
                nullableInstant(resultSet, prefix + "last_reviewed_at"),
                instant(resultSet, dueAtColumn));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getTimestamp(column).toInstant();
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private record LearningItemTag(UUID learningItemId, String tag) {
    }
}
