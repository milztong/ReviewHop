package dev.reviewhop.system.adapter.out.persistence;

import dev.reviewhop.system.application.port.out.DatabaseProbe;
import dev.reviewhop.system.domain.DatabaseStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
class JpaDatabaseProbe implements DatabaseProbe {

    private static final short SYSTEM_MARKER_ID = 1;

    private final SystemMarkerRepository repository;

    JpaDatabaseProbe(SystemMarkerRepository repository) {
        this.repository = repository;
    }

    @Override
    public DatabaseStatus status() {
        try {
            return repository.existsById(SYSTEM_MARKER_ID)
                    ? DatabaseStatus.READY
                    : DatabaseStatus.UNAVAILABLE;
        } catch (DataAccessException exception) {
            return DatabaseStatus.UNAVAILABLE;
        }
    }
}
