package dev.reviewhop.integration.application;

public record SyncExecutionResult(
        String resultCursor,
        int importedCount,
        int existingCount,
        int pagesProcessed) {
}

