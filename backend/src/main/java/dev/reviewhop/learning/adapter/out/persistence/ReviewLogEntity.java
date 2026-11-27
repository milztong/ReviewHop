package dev.reviewhop.learning.adapter.out.persistence;

import dev.reviewhop.learning.domain.SchedulingSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_log")
class ReviewLogEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_card_id", nullable = false)
    private ReviewCardEntity reviewCard;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(nullable = false, length = 16)
    private String rating;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(length = 2000)
    private String note;

    @Column(name = "previous_due_at", nullable = false)
    private Instant previousDueAt;

    @Column(name = "next_due_at", nullable = false)
    private Instant nextDueAt;

    @Column(name = "scheduler_version", nullable = false, length = 64)
    private String schedulerVersion;

    @Column(name = "desired_retention")
    private Double desiredRetention;

    @Column(name = "previous_scheduler_state", length = 16)
    private String previousSchedulerState;

    @Column(name = "previous_scheduler_step")
    private Integer previousSchedulerStep;

    @Column(name = "previous_stability")
    private Double previousStability;

    @Column(name = "previous_scheduler_difficulty")
    private Double previousSchedulerDifficulty;

    @Column(name = "previous_last_reviewed_at")
    private Instant previousLastReviewedAt;

    @Column(name = "next_scheduler_state", length = 16)
    private String nextSchedulerState;

    @Column(name = "next_scheduler_step")
    private Integer nextSchedulerStep;

    @Column(name = "next_stability")
    private Double nextStability;

    @Column(name = "next_scheduler_difficulty")
    private Double nextSchedulerDifficulty;

    @Column(name = "next_last_reviewed_at")
    private Instant nextLastReviewedAt;

    protected ReviewLogEntity() {
    }

    ReviewLogEntity(
            UUID id,
            ReviewCardEntity reviewCard,
            UUID userId,
            UUID idempotencyKey,
            String rating,
            Instant reviewedAt,
            long durationMs,
            String note,
            Instant previousDueAt,
            Instant nextDueAt,
            String schedulerVersion,
            double desiredRetention,
            SchedulingSnapshot previous,
            SchedulingSnapshot next) {
        this.id = id;
        this.reviewCard = reviewCard;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.rating = rating;
        this.reviewedAt = reviewedAt;
        this.durationMs = durationMs;
        this.note = note;
        this.previousDueAt = previousDueAt;
        this.nextDueAt = nextDueAt;
        this.schedulerVersion = schedulerVersion;
        this.desiredRetention = desiredRetention;
        this.previousSchedulerState = previous.state().name();
        this.previousSchedulerStep = previous.step();
        this.previousStability = previous.stability();
        this.previousSchedulerDifficulty = previous.difficulty();
        this.previousLastReviewedAt = previous.lastReviewedAt();
        this.nextSchedulerState = next.state().name();
        this.nextSchedulerStep = next.step();
        this.nextStability = next.stability();
        this.nextSchedulerDifficulty = next.difficulty();
        this.nextLastReviewedAt = next.lastReviewedAt();
    }

    UUID id() {
        return id;
    }

    ReviewCardEntity reviewCard() {
        return reviewCard;
    }

    String rating() {
        return rating;
    }

    Instant reviewedAt() {
        return reviewedAt;
    }

    long durationMs() {
        return durationMs;
    }

    String note() {
        return note;
    }

    Instant previousDueAt() {
        return previousDueAt;
    }

    Instant nextDueAt() {
        return nextDueAt;
    }

    String schedulerVersion() {
        return schedulerVersion;
    }
}
