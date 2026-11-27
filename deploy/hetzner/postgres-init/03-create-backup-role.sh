#!/usr/bin/env bash
set -euo pipefail

: "${BACKUP_DB_USERNAME:?BACKUP_DB_USERNAME is required}"
: "${BACKUP_DB_PASSWORD:?BACKUP_DB_PASSWORD is required}"
: "${KEYCLOAK_DB:?KEYCLOAK_DB is required}"

psql --set=ON_ERROR_STOP=1 \
  --username "${POSTGRES_USER}" \
  --dbname postgres \
  --set=backup_user="${BACKUP_DB_USERNAME}" \
  --set=backup_password="${BACKUP_DB_PASSWORD}" \
  --set=application_database="${POSTGRES_DB}" \
  --set=keycloak_database="${KEYCLOAK_DB}" <<'SQL'
SELECT format(
  'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE IN ROLE pg_read_all_data',
  :'backup_user',
  :'backup_password'
)
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'backup_user') \gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'application_database', :'backup_user') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'keycloak_database', :'backup_user') \gexec
SQL
