package dev.reviewhop.integration.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SyncJobJpaRepository extends JpaRepository<SyncJobEntity, UUID> {

    Optional<SyncJobEntity> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    Optional<SyncJobEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<SyncJobEntity> findByIdAndConnection_IdAndUserId(
            UUID id, UUID connectionId, UUID userId);

    Optional<SyncJobEntity> findFirstByConnection_IdAndStatus(UUID connectionId, String status);

    Optional<SyncJobEntity> findFirstByConnection_IdAndStatusIn(
            UUID connectionId, java.util.Collection<String> statuses);
}
