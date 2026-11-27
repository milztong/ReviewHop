package dev.reviewhop.identity;

import dev.reviewhop.identity.api.CurrentAppUser;
import dev.reviewhop.identity.application.port.out.AppUserStore;
import dev.reviewhop.identity.application.port.out.CurrentOidcIdentity;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IdentityConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    CurrentAppUser currentAppUser(
            CurrentOidcIdentity currentOidcIdentity,
            AppUserStore appUserStore,
            Clock clock) {
        return new CurrentAppUser(currentOidcIdentity, appUserStore, clock);
    }
}
