package dev.reviewhop.integration.application;

import dev.reviewhop.integration.application.port.out.SyncWorkQueue;
import java.time.Clock;
import java.util.UUID;

public final class RetryDeadLetterSync {

    private final SyncWorkQueue queue;
    private final Clock clock;

    public RetryDeadLetterSync(SyncWorkQueue queue, Clock clock) {
        this.queue = queue;
        this.clock = clock;
    }

    public SyncJobView execute(UUID syncJobId) {
        return queue.retryDeadLetter(syncJobId, clock.instant());
    }
}
