#!/usr/bin/env bash
set -euo pipefail

: "${ANKILEET_APP_DB_USERNAME:?ANKILEET_APP_DB_USERNAME is required}"
: "${ANKILEET_APP_DB_PASSWORD:?ANKILEET_APP_DB_PASSWORD is required}"

psql --set=ON_ERROR_STOP=1 \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}" \
  --set=app_user="${ANKILEET_APP_DB_USERNAME}" \
  --set=app_password="${ANKILEET_APP_DB_PASSWORD}" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'app_user') \gexec

SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', current_database()) \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'app_user') \gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user') \gexec
SQL
