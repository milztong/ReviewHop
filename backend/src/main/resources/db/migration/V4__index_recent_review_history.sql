CREATE INDEX ix_review_log_user_reviewed_at
    ON review_log(user_id, reviewed_at DESC);
