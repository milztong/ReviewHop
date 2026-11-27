package dev.reviewhop.integration.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SyncOutboxEventJpaRepository
        extends JpaRepository<SyncOutboxEventEntity, UUID> {

    Optional<SyncOutboxEventEntity> findBySyncJob_Id(UUID syncJobId);
}
