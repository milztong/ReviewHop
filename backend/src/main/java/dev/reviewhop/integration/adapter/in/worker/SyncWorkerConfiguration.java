package dev.reviewhop.integration.adapter.in.worker;

import dev.reviewhop.integration.application.ProcessNextSyncWork;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "ankileet.worker.enabled", havingValue = "true")
class SyncWorkerConfiguration {

    @Bean
    SyncWorkerRunner syncWorkerRunner(ProcessNextSyncWork processNextSyncWork) {
        return new SyncWorkerRunner(
                processNextSyncWork, "worker-" + UUID.randomUUID());
    }
}

