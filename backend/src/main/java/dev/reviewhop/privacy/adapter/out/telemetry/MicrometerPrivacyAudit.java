package dev.reviewhop.privacy.adapter.out.telemetry;

import dev.reviewhop.privacy.application.DeletedAccountData;
import dev.reviewhop.privacy.application.port.out.PrivacyAudit;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class MicrometerPrivacyAudit implements PrivacyAudit {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerPrivacyAudit.class);

    private final MeterRegistry meterRegistry;

    MicrometerPrivacyAudit(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void accountDataExported() {
        meterRegistry.counter("ankileet.privacy.account.data.exports").increment();
        LOGGER.info("account_data_exported");
    }

    @Override
    public void accountDataDeleted(DeletedAccountData deleted) {
        meterRegistry.counter("ankileet.privacy.account.data.deletions").increment();
        LOGGER.info(
                "account_data_deleted providerConnections={} syncJobs={} outboxEvents={} "
                        + "learningItems={} reviewCards={} reviewLogs={} orphanedSourceItems={}",
                deleted.providerConnections(),
                deleted.syncJobs(),
                deleted.outboxEvents(),
                deleted.learningItems(),
                deleted.reviewCards(),
                deleted.reviewLogs(),
                deleted.orphanedSourceItems());
    }
}
