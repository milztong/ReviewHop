package dev.reviewhop.learning.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "source_item")
class SourceItemEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 512)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(length = 50)
    private String difficulty;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "source_item_tag", joinColumns = @JoinColumn(name = "source_item_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SourceItemEntity() {
    }

    SourceItemEntity(
            UUID id,
            String provider,
            String externalId,
            String title,
            String sourceUrl,
            String difficulty,
            List<String> tags,
            Instant createdAt) {
        this.id = id;
        this.provider = provider;
        this.externalId = externalId;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.difficulty = difficulty;
        this.tags = new LinkedHashSet<>(tags);
        this.createdAt = createdAt;
    }

    SourceItemEntity(
            UUID id,
            String externalId,
            String title,
            String sourceUrl,
            String difficulty,
            List<String> tags,
            Instant createdAt) {
        this(id, "MANUAL", externalId, title, sourceUrl, difficulty, tags, createdAt);
    }

    UUID id() {
        return id;
    }

    String title() {
        return title;
    }

    String sourceUrl() {
        return sourceUrl;
    }

    String difficulty() {
        return difficulty;
    }

    List<String> tags() {
        return List.copyOf(tags);
    }
}
