package dev.reviewhop.learning.application;

import dev.reviewhop.identity.api.AppUserId;
import dev.reviewhop.learning.api.CompletedSourceItem;
import dev.reviewhop.learning.api.LearningImportResult;
import dev.reviewhop.learning.api.LearningItemImporter;
import dev.reviewhop.learning.application.port.out.LearningStore;
import java.time.Instant;
import java.util.List;

public final class ImportCompletedSourceItems implements LearningItemImporter {

    private final LearningStore learningStore;

    public ImportCompletedSourceItems(LearningStore learningStore) {
        this.learningStore = learningStore;
    }

    @Override
    public LearningImportResult importCompleted(
            AppUserId userId,
            String provider,
            List<CompletedSourceItem> items,
            Instant importedAt) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        return learningStore.importCompletedSourceItems(
                userId,
                provider.trim().toUpperCase(java.util.Locale.ROOT),
                List.copyOf(items),
                importedAt);
    }
}
