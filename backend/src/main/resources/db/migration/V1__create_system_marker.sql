CREATE TABLE system_marker (
    id SMALLINT PRIMARY KEY,
    initialized_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_marker_singleton CHECK (id = 1)
);

INSERT INTO system_marker (id) VALUES (1);
