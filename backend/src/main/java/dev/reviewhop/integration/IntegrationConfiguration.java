package dev.reviewhop.integration;

import dev.reviewhop.integration.adapter.out.codewars.CodewarsConnector;
import dev.reviewhop.integration.adapter.out.telemetry.MicrometerSyncTraceContext;
import dev.reviewhop.integration.application.ConnectCodewarsProfile;
import dev.reviewhop.integration.application.ListProviderConnections;
import dev.reviewhop.integration.application.ListProviderCatalog;
import dev.reviewhop.integration.application.GetSyncJob;
import dev.reviewhop.integration.application.ProcessCodewarsSynchronization;
import dev.reviewhop.integration.application.ProcessNextSyncWork;
import dev.reviewhop.integration.application.RequestCodewarsSynchronization;
import dev.reviewhop.integration.application.RetryDeadLetterSync;
import dev.reviewhop.integration.application.port.out.IntegrationStore;
import dev.reviewhop.integration.application.port.out.ProviderConnector;
import dev.reviewhop.integration.application.port.out.SyncWorkQueue;
import dev.reviewhop.integration.application.port.out.SyncTraceContext;
import dev.reviewhop.learning.api.LearningItemImporter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class IntegrationConfiguration {

    @Bean
    ProviderConnector codewarsConnector(
            RestClient.Builder restClientBuilder,
            MeterRegistry meterRegistry,
            @Value("${ankileet.providers.codewars.api-base}") URI apiBase,
            @Value("${ankileet.providers.codewars.public-site}") URI publicSite) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        var restClient = restClientBuilder
                .baseUrl(apiBase.toString())
                .requestFactory(requestFactory)
                .build();
        return new CodewarsConnector(restClient, publicSite, meterRegistry);
    }

    @Bean
    ConnectCodewarsProfile connectCodewarsProfile(
            ProviderConnector connector, IntegrationStore store, Clock clock) {
        return new ConnectCodewarsProfile(connector, store, clock);
    }

    @Bean
    ListProviderConnections listProviderConnections(IntegrationStore store) {
        return new ListProviderConnections(store);
    }

    @Bean
    ListProviderCatalog listProviderCatalog(List<ProviderConnector> connectors) {
        return new ListProviderCatalog(connectors);
    }

    @Bean
    RequestCodewarsSynchronization requestCodewarsSynchronization(
            IntegrationStore store, Clock clock, SyncTraceContext traceContext) {
        return new RequestCodewarsSynchronization(store, clock, traceContext);
    }

    @Bean
    GetSyncJob getSyncJob(IntegrationStore store) {
        return new GetSyncJob(store);
    }

    @Bean
    ProcessCodewarsSynchronization processCodewarsSynchronization(
            ProviderConnector connector,
            LearningItemImporter importer,
            Clock clock) {
        return new ProcessCodewarsSynchronization(connector, importer, clock);
    }

    @Bean
    ProcessNextSyncWork processNextSyncWork(
            SyncWorkQueue queue,
            ProcessCodewarsSynchronization synchronization,
            Clock clock,
            MeterRegistry meterRegistry,
            SyncTraceContext traceContext) {
        DoubleSupplier jitter = () -> ThreadLocalRandom.current().nextDouble();
        return new ProcessNextSyncWork(
                queue, synchronization, clock, jitter, meterRegistry, traceContext);
    }

    @Bean
    SyncTraceContext syncTraceContext(Tracer tracer, Propagator propagator) {
        return new MicrometerSyncTraceContext(tracer, propagator);
    }

    @Bean
    RetryDeadLetterSync retryDeadLetterSync(SyncWorkQueue queue, Clock clock) {
        return new RetryDeadLetterSync(queue, clock);
    }
}
