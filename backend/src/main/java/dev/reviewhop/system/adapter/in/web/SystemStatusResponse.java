package dev.reviewhop.system.adapter.in.web;

import dev.reviewhop.system.domain.SystemStatus;

record SystemStatusResponse(String application, String database) {

    static SystemStatusResponse from(SystemStatus status) {
        return new SystemStatusResponse(status.application(), status.database().name());
    }
}
