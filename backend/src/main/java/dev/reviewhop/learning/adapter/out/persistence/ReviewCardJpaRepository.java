package dev.reviewhop.learning.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReviewCardJpaRepository extends JpaRepository<ReviewCardEntity, UUID> {

    Optional<ReviewCardEntity> findByLearningItemId(UUID learningItemId);

    @Query("""
            select distinct card from ReviewCardEntity card
            join fetch card.learningItem learningItem
            join fetch learningItem.sourceItem sourceItem
            where learningItem.userId = :userId
              and card.state = 'ACTIVE'
              and card.dueAt <= :dueAt
            order by card.dueAt asc
            """)
    List<ReviewCardEntity> findDue(
            @Param("userId") UUID userId,
            @Param("dueAt") Instant dueAt);

    @Query("""
            select card from ReviewCardEntity card
            join fetch card.learningItem learningItem
            join fetch learningItem.sourceItem
            where card.id = :cardId and learningItem.userId = :userId
            """)
    Optional<ReviewCardEntity> findOwned(
            @Param("userId") UUID userId,
            @Param("cardId") UUID cardId);
}
