package dev.reviewhop.learning.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LearningItemJpaRepository extends JpaRepository<LearningItemEntity, UUID> {

    boolean existsByUserIdAndSourceItemId(UUID userId, UUID sourceItemId);

    @Query("""
            select learningItem from LearningItemEntity learningItem
            join fetch learningItem.sourceItem sourceItem
            where learningItem.userId = :userId
              and sourceItem.provider = 'MANUAL'
              and sourceItem.externalId = :externalId
            """)
    Optional<LearningItemEntity> findManualByUserAndExternalId(
            @Param("userId") UUID userId,
            @Param("externalId") String externalId);
}
