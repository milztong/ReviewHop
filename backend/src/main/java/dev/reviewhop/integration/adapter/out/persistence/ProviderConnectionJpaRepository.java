package dev.reviewhop.integration.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProviderConnectionJpaRepository extends JpaRepository<ProviderConnectionEntity, UUID> {

    Optional<ProviderConnectionEntity> findByUserIdAndProviderAndExternalUserId(
            UUID userId, String provider, String externalUserId);

    Optional<ProviderConnectionEntity> findByIdAndUserId(UUID id, UUID userId);

    List<ProviderConnectionEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
}
