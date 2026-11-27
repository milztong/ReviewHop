DO $$
DECLARE
    application_role TEXT := '${applicationRole}';
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = application_role) THEN
        EXECUTE format(
            'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE '
            || 'system_marker, app_user, source_item, source_item_tag, learning_item, review_card, review_log '
            || 'TO %I',
            application_role
        );
    END IF;
END
$$;
