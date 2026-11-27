package dev.reviewhop.integration.adapter.in.worker;

import dev.reviewhop.integration.application.ProcessNextSyncWork;
import org.springframework.scheduling.annotation.Scheduled;

final class SyncWorkerRunner {

    private static final int MAX_BATCH_SIZE = 10;

    private final ProcessNextSyncWork processNextSyncWork;
    private final String workerId;

    SyncWorkerRunner(ProcessNextSyncWork processNextSyncWork, String workerId) {
        this.processNextSyncWork = processNextSyncWork;
        this.workerId = workerId;
    }

    @Scheduled(fixedDelayString = "${ankileet.worker.poll-delay:PT2S}")
    void poll() {
        for (var processed = 0; processed < MAX_BATCH_SIZE; processed++) {
            if (!processNextSyncWork.execute(workerId)) {
                return;
            }
        }
    }
}

