DROP INDEX uk_sync_job_connection_running;

ALTER TABLE sync_job
    DROP CONSTRAINT ck_sync_job_status,
    ADD CONSTRAINT ck_sync_job_status CHECK (
        status IN (
            'REQUESTED',
            'RUNNING',
            'RETRY_SCHEDULED',
            'SUCCEEDED',
            'RETRYABLE_FAILED',
            'TERMINAL_FAILED',
            'DEAD_LETTERED'
        )
    );

CREATE UNIQUE INDEX uk_sync_job_connection_active
    ON sync_job(connection_id)
    WHERE status IN ('REQUESTED', 'RUNNING', 'RETRY_SCHEDULED');

CREATE TABLE sync_outbox_event (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL REFERENCES sync_job(id),
    event_type VARCHAR(64) NOT NULL,
    payload_version SMALLINT NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lease_token UUID,
    lease_owner VARCHAR(128),
    lease_expires_at TIMESTAMP WITH TIME ZONE,
    correlation_id UUID NOT NULL,
    traceparent VARCHAR(255),
    last_failure_code VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_sync_outbox_event_type_aggregate
        UNIQUE (event_type, aggregate_id),
    CONSTRAINT ck_sync_outbox_event_type
        CHECK (event_type = 'SyncRequested'),
    CONSTRAINT ck_sync_outbox_payload_version
        CHECK (payload_version = 1),
    CONSTRAINT ck_sync_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'DEAD_LETTERED')),
    CONSTRAINT ck_sync_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_sync_outbox_lease CHECK (
        (status = 'PROCESSING'
            AND lease_token IS NOT NULL
            AND lease_owner IS NOT NULL
            AND lease_expires_at IS NOT NULL)
        OR
        (status <> 'PROCESSING'
            AND lease_token IS NULL
            AND lease_owner IS NULL
            AND lease_expires_at IS NULL)
    )
);

CREATE INDEX ix_sync_outbox_claim
    ON sync_outbox_event(available_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

DO $$
DECLARE
    application_role TEXT := '${applicationRole}';
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = application_role) THEN
        EXECUTE format(
            'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE sync_outbox_event TO %I',
            application_role
        );
    END IF;
END
$$;

