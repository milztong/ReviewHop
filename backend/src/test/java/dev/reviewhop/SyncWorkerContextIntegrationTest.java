package dev.reviewhop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "ankileet.worker.enabled=true",
            "ankileet.worker.poll-delay=PT1H"
        })
class SyncWorkerContextIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17.12-alpine");

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startsTheWorkerWithoutAServletWebServer() {
        assertThat(applicationContext.containsBean("syncWorkerRunner")).isTrue();
        assertThat(applicationContext.containsBean("apiSecurity")).isFalse();
    }
}
