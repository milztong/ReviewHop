ALTER TABLE app_user
    ADD COLUMN desired_retention DOUBLE PRECISION NOT NULL DEFAULT 0.90,
    ADD CONSTRAINT ck_app_user_desired_retention
        CHECK (desired_retention > 0.0 AND desired_retention < 1.0);

ALTER TABLE review_card
    ADD COLUMN scheduler_state VARCHAR(16) NOT NULL DEFAULT 'LEARNING',
    ADD COLUMN scheduler_step INTEGER DEFAULT 0,
    ADD COLUMN stability DOUBLE PRECISION,
    ADD COLUMN scheduler_difficulty DOUBLE PRECISION,
    ADD COLUMN last_reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT ck_review_card_scheduler_step
        CHECK (scheduler_step IS NULL OR scheduler_step >= 0),
    ADD CONSTRAINT ck_review_card_stability
        CHECK (stability IS NULL OR stability > 0.0),
    ADD CONSTRAINT ck_review_card_scheduler_difficulty
        CHECK (scheduler_difficulty IS NULL
            OR scheduler_difficulty BETWEEN 1.0 AND 10.0),
    ADD CONSTRAINT ck_review_card_scheduler_state
        CHECK (scheduler_state IN ('LEARNING', 'REVIEW', 'RELEARNING')),
    ADD CONSTRAINT ck_review_card_memory_state CHECK (
        (stability IS NULL
            AND scheduler_difficulty IS NULL
            AND last_reviewed_at IS NULL)
        OR (stability IS NOT NULL
            AND scheduler_difficulty IS NOT NULL
            AND last_reviewed_at IS NOT NULL)
    ),
    ADD CONSTRAINT ck_review_card_learning_step CHECK (
        (scheduler_state = 'REVIEW' AND scheduler_step IS NULL)
        OR (scheduler_state IN ('LEARNING', 'RELEARNING') AND scheduler_step IS NOT NULL)
    ),
    ADD CONSTRAINT ck_review_card_established_memory CHECK (
        scheduler_state = 'LEARNING' OR stability IS NOT NULL
    );

ALTER TABLE review_log
    ADD COLUMN desired_retention DOUBLE PRECISION,
    ADD COLUMN previous_scheduler_state VARCHAR(16),
    ADD COLUMN previous_scheduler_step INTEGER,
    ADD COLUMN previous_stability DOUBLE PRECISION,
    ADD COLUMN previous_scheduler_difficulty DOUBLE PRECISION,
    ADD COLUMN previous_last_reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN next_scheduler_state VARCHAR(16),
    ADD COLUMN next_scheduler_step INTEGER,
    ADD COLUMN next_stability DOUBLE PRECISION,
    ADD COLUMN next_scheduler_difficulty DOUBLE PRECISION,
    ADD COLUMN next_last_reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT ck_review_log_fsrs_snapshot CHECK (
        scheduler_version NOT LIKE 'fsrs-%'
        OR (
            desired_retention > 0.0
            AND desired_retention < 1.0
            AND previous_scheduler_state IS NOT NULL
            AND next_scheduler_state IS NOT NULL
            AND next_stability > 0.0
            AND next_scheduler_difficulty BETWEEN 1.0 AND 10.0
            AND next_last_reviewed_at IS NOT NULL
        )
    );
