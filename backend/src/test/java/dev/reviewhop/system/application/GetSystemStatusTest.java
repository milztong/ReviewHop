package dev.reviewhop.system.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.reviewhop.system.domain.DatabaseStatus;
import org.junit.jupiter.api.Test;

class GetSystemStatusTest {

    @Test
    void reportsTheDatabaseStatusFromTheProbe() {
        var useCase = new GetSystemStatus(() -> DatabaseStatus.READY);

        var status = useCase.execute();

        assertThat(status.application()).isEqualTo("reviewhop-api");
        assertThat(status.database()).isEqualTo(DatabaseStatus.READY);
    }
}
