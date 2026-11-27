package dev.reviewhop.system.domain;

import java.util.Objects;

public record SystemStatus(String application, DatabaseStatus database) {

    public SystemStatus {
        Objects.requireNonNull(application, "application must not be null");
        Objects.requireNonNull(database, "database must not be null");
    }
}
