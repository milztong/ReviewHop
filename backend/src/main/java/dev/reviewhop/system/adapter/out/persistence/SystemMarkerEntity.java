package dev.reviewhop.system.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "system_marker")
class SystemMarkerEntity {

    @Id
    private Short id;

    @Column(name = "initialized_at", nullable = false, updatable = false)
    private Instant initializedAt;

    protected SystemMarkerEntity() {
    }

    Short getId() {
        return id;
    }

    Instant getInitializedAt() {
        return initializedAt;
    }
}
