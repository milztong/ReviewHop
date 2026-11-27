package dev.reviewhop.learning.adapter.out.persistence;

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
@Table(name = "learning_item")
class LearningItemEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_item_id", nullable = false)
    private SourceItemEntity sourceItem;

    @Column(length = 4000)
    private String notes;

    @Column(name = "recall_prompt", length = 4000)
    private String recallPrompt;

    @Column(name = "first_completed_at", nullable = false)
    private Instant firstCompletedAt;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LearningItemEntity() {
    }

    LearningItemEntity(
            UUID id,
            UUID userId,
            SourceItemEntity sourceItem,
            String notes,
            String recallPrompt,
            Instant firstCompletedAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.sourceItem = sourceItem;
        this.notes = notes;
        this.recallPrompt = recallPrompt;
        this.firstCompletedAt = firstCompletedAt;
        this.status = "ACTIVE";
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    UUID userId() {
        return userId;
    }

    SourceItemEntity sourceItem() {
        return sourceItem;
    }

    String notes() {
        return notes;
    }

    String recallPrompt() {
        return recallPrompt;
    }
}
