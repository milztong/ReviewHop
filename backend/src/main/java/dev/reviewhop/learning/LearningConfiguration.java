package dev.reviewhop.learning;

import dev.reviewhop.learning.application.CompleteReview;
import dev.reviewhop.learning.application.CreateManualLearningItem;
import dev.reviewhop.learning.application.ListDueReviews;
import dev.reviewhop.learning.application.ListRecentReviewHistory;
import dev.reviewhop.learning.application.ListReviewHistory;
import dev.reviewhop.learning.application.ImportCompletedSourceItems;
import dev.reviewhop.learning.api.LearningItemImporter;
import dev.reviewhop.learning.application.port.out.LearningStore;
import dev.reviewhop.learning.application.port.out.SchedulingPolicy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class LearningConfiguration {

    @Bean
    LearningItemImporter learningItemImporter(LearningStore store) {
        return new ImportCompletedSourceItems(store);
    }

    @Bean
    CreateManualLearningItem createManualLearningItem(LearningStore store, Clock clock) {
        return new CreateManualLearningItem(store, clock);
    }

    @Bean
    ListDueReviews listDueReviews(LearningStore store, Clock clock) {
        return new ListDueReviews(store, clock);
    }

    @Bean
    CompleteReview completeReview(LearningStore store, SchedulingPolicy policy, Clock clock) {
        return new CompleteReview(store, policy, clock);
    }

    @Bean
    ListReviewHistory listReviewHistory(LearningStore store) {
        return new ListReviewHistory(store);
    }

    @Bean
    ListRecentReviewHistory listRecentReviewHistory(LearningStore store) {
        return new ListRecentReviewHistory(store);
    }
}
