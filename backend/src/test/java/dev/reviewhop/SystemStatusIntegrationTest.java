package dev.reviewhop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SystemStatusIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17.12-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void reachesTheFlywayManagedDatabaseThroughTheApi() {
        var response = restTemplate.getForEntity("/api/v1/system/status", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"application\":\"reviewhop-api\"")
                .contains("\"database\":\"READY\"");
    }

    @Test
    void exposesReadinessAndOpenApiDocumentation() {
        var readiness = restTemplate.getForEntity("/actuator/health/readiness", String.class);
        var openApi = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readiness.getBody()).contains("\"status\":\"UP\"");
        assertThat(openApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openApi.getBody())
                .contains("/api/v1/system/status")
                .contains("/api/v1/reviews/history")
                .contains("/api/v1/users/me/export")
                .contains("deleteCurrentUserData");
    }
}
