package dev.reviewhop.learning.adapter.out.persistence;

import dev.reviewhop.learning.domain.SchedulingSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_card")
class ReviewCardEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_item_id", nullable = false)
    private LearningItemEntity learningItem;

    @Column(nullable = false, length = 32)
    private String mode;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(nullable = false, length = 32)
    private String state;

    @Column(name = "scheduler_state", nullable = false, length = 16)
    private String schedulerState;

    @Column(name = "scheduler_step")
    private Integer schedulerStep;

    private Double stability;

    @Column(name = "scheduler_difficulty")
    private Double schedulerDifficulty;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Version
    private long version;

    protected ReviewCardEntity() {
    }

    ReviewCardEntity(UUID id, LearningItemEntity learningItem, Instant dueAt) {
        this.id = id;
        this.learningItem = learningItem;
        this.mode = "APPROACH_RECALL";
        this.dueAt = dueAt;
        this.state = "ACTIVE";
        this.schedulerState = "LEARNING";
        this.schedulerStep = 0;
    }

    UUID id() {
        return id;
    }

    LearningItemEntity learningItem() {
        return learningItem;
    }

    String mode() {
        return mode;
    }

    Instant dueAt() {
        return dueAt;
    }

    long version() {
        return version;
    }

    String schedulerState() {
        return schedulerState;
    }

    Integer schedulerStep() {
        return schedulerStep;
    }

    Double stability() {
        return stability;
    }

    Double schedulerDifficulty() {
        return schedulerDifficulty;
    }

    Instant lastReviewedAt() {
        return lastReviewedAt;
    }

    void reschedule(SchedulingSnapshot next) {
        dueAt = next.dueAt();
        schedulerState = next.state().name();
        schedulerStep = next.step();
        stability = next.stability();
        schedulerDifficulty = next.difficulty();
        lastReviewedAt = next.lastReviewedAt();
    }
}
