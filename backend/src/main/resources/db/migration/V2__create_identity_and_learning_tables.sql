CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    oidc_issuer VARCHAR(512) NOT NULL,
    oidc_subject VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_user_oidc_identity UNIQUE (oidc_issuer, oidc_subject)
);

CREATE TABLE source_item (
    id UUID PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    external_id VARCHAR(512) NOT NULL,
    title VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    difficulty VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_source_item_provider_external UNIQUE (provider, external_id),
    CONSTRAINT ck_source_item_manual_provider CHECK (provider = 'MANUAL')
);

CREATE TABLE source_item_tag (
    source_item_id UUID NOT NULL REFERENCES source_item(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (source_item_id, tag)
);

CREATE TABLE learning_item (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    source_item_id UUID NOT NULL REFERENCES source_item(id),
    notes VARCHAR(4000),
    first_completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_learning_item_user_source UNIQUE (user_id, source_item_id)
);

CREATE TABLE review_card (
    id UUID PRIMARY KEY,
    learning_item_id UUID NOT NULL REFERENCES learning_item(id),
    mode VARCHAR(32) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    state VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_review_card_learning_mode UNIQUE (learning_item_id, mode)
);

CREATE INDEX ix_review_card_due_at ON review_card(due_at);

CREATE TABLE review_log (
    id UUID PRIMARY KEY,
    review_card_id UUID NOT NULL REFERENCES review_card(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    idempotency_key UUID NOT NULL,
    rating VARCHAR(16) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms BIGINT NOT NULL,
    note VARCHAR(2000),
    previous_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduler_version VARCHAR(64) NOT NULL,
    CONSTRAINT uk_review_log_user_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_review_log_duration CHECK (duration_ms >= 0)
);

CREATE INDEX ix_review_log_card_reviewed_at ON review_log(review_card_id, reviewed_at DESC);
