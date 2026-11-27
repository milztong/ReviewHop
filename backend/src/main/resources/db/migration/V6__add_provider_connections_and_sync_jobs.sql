ALTER TABLE source_item
    DROP CONSTRAINT ck_source_item_manual_provider,
    ADD CONSTRAINT ck_source_item_provider
        CHECK (provider IN ('MANUAL', 'CODEWARS'));

CREATE TABLE provider_connection (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    provider VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    verification_status VARCHAR(32) NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    cursor_value VARCHAR(1024),
    last_synced_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_provider_connection_user_profile
        UNIQUE (user_id, provider, external_user_id),
    CONSTRAINT ck_provider_connection_provider CHECK (provider = 'CODEWARS'),
    CONSTRAINT ck_provider_connection_mode CHECK (mode = 'PUBLIC_PROFILE'),
    CONSTRAINT ck_provider_connection_status CHECK (status = 'ACTIVE'),
    CONSTRAINT ck_provider_connection_verification
        CHECK (verification_status = 'UNVERIFIED')
);

CREATE TABLE sync_job (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL REFERENCES provider_connection(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    idempotency_key UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_cursor VARCHAR(1024),
    result_cursor VARCHAR(1024),
    imported_count INTEGER NOT NULL DEFAULT 0,
    existing_count INTEGER NOT NULL DEFAULT 0,
    pages_processed INTEGER NOT NULL DEFAULT 0,
    failure_code VARCHAR(64),
    failure_detail VARCHAR(500),
    retry_after_seconds BIGINT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_sync_job_user_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_sync_job_status CHECK (
        status IN ('RUNNING', 'SUCCEEDED', 'RETRYABLE_FAILED', 'TERMINAL_FAILED')
    ),
    CONSTRAINT ck_sync_job_counts CHECK (
        imported_count >= 0 AND existing_count >= 0 AND pages_processed >= 0
    )
);

CREATE INDEX ix_provider_connection_user
    ON provider_connection(user_id, provider);

CREATE INDEX ix_sync_job_connection_started
    ON sync_job(connection_id, started_at DESC);

CREATE UNIQUE INDEX uk_sync_job_connection_running
    ON sync_job(connection_id)
    WHERE status = 'RUNNING';

DO $$
DECLARE
    application_role TEXT := '${applicationRole}';
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = application_role) THEN
        EXECUTE format(
            'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE provider_connection, sync_job TO %I',
            application_role
        );
    END IF;
END
$$;
