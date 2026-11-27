package dev.reviewhop.system;

import dev.reviewhop.system.application.GetSystemStatus;
import dev.reviewhop.system.application.port.out.DatabaseProbe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SystemConfiguration {

    @Bean
    GetSystemStatus getSystemStatus(DatabaseProbe databaseProbe) {
        return new GetSystemStatus(databaseProbe);
    }
}
