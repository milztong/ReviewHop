package dev.reviewhop.system.application.port.out;

import dev.reviewhop.system.domain.DatabaseStatus;

public interface DatabaseProbe {

    DatabaseStatus status();
}
