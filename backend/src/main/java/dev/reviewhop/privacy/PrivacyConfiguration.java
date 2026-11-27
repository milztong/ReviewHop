package dev.reviewhop.privacy;

import dev.reviewhop.identity.api.CurrentAppUser;
import dev.reviewhop.privacy.application.DeleteOwnAccountData;
import dev.reviewhop.privacy.application.ExportOwnAccountData;
import dev.reviewhop.privacy.application.port.out.PrivacyAudit;
import dev.reviewhop.privacy.application.port.out.PrivacyDataStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PrivacyConfiguration {

    @Bean
    ExportOwnAccountData exportOwnAccountData(
            CurrentAppUser currentAppUser,
            PrivacyDataStore dataStore,
            PrivacyAudit audit,
            Clock clock) {
        return new ExportOwnAccountData(currentAppUser, dataStore, audit, clock);
    }

    @Bean
    DeleteOwnAccountData deleteOwnAccountData(
            CurrentAppUser currentAppUser,
            PrivacyDataStore dataStore,
            PrivacyAudit audit) {
        return new DeleteOwnAccountData(currentAppUser, dataStore, audit);
    }
}
