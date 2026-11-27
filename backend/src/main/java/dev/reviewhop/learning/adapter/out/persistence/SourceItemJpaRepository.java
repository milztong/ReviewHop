package dev.reviewhop.learning.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SourceItemJpaRepository extends JpaRepository<SourceItemEntity, UUID> {

    Optional<SourceItemEntity> findByProviderAndExternalId(String provider, String externalId);
}
