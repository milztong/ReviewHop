package dev.reviewhop.learning.api;

import dev.reviewhop.identity.api.AppUserId;
import java.time.Instant;
import java.util.List;

public interface LearningItemImporter {

    LearningImportResult importCompleted(
            AppUserId userId,
            String provider,
            List<CompletedSourceItem> items,
            Instant importedAt);
}
