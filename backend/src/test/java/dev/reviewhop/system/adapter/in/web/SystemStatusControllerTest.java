package dev.reviewhop.system.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.reviewhop.system.application.GetSystemStatus;
import dev.reviewhop.system.domain.DatabaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SystemStatusControllerTest {

    @Test
    void reportsServiceUnavailableWhenTheDatabaseProbeFails() {
        var useCase = new GetSystemStatus(() -> DatabaseStatus.UNAVAILABLE);
        var controller = new SystemStatusController(useCase);

        var response = controller.getStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().database()).isEqualTo("UNAVAILABLE");
    }
}
