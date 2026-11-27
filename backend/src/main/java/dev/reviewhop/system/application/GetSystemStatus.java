package dev.reviewhop.system.application;

import dev.reviewhop.system.application.port.out.DatabaseProbe;
import dev.reviewhop.system.domain.SystemStatus;

public final class GetSystemStatus {

    private static final String APPLICATION_NAME = "reviewhop-api";

    private final DatabaseProbe databaseProbe;

    public GetSystemStatus(DatabaseProbe databaseProbe) {
        this.databaseProbe = databaseProbe;
    }

    public SystemStatus execute() {
        return new SystemStatus(APPLICATION_NAME, databaseProbe.status());
    }
}
