package dev.reviewhop.learning.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReviewLogJpaRepository extends JpaRepository<ReviewLogEntity, UUID> {

    Optional<ReviewLogEntity> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    @EntityGraph(attributePaths = {
        "reviewCard",
        "reviewCard.learningItem",
        "reviewCard.learningItem.sourceItem"
    })
    List<ReviewLogEntity> findByReviewCardIdOrderByReviewedAtDesc(UUID reviewCardId);

    @EntityGraph(attributePaths = {
        "reviewCard",
        "reviewCard.learningItem",
        "reviewCard.learningItem.sourceItem"
    })
    List<ReviewLogEntity> findByUserIdOrderByReviewedAtDesc(UUID userId, Pageable pageable);
}
