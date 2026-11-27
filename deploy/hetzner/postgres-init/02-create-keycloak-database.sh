#!/usr/bin/env bash
set -euo pipefail

: "${KEYCLOAK_DB:?KEYCLOAK_DB is required}"
: "${KEYCLOAK_DB_USERNAME:?KEYCLOAK_DB_USERNAME is required}"
: "${KEYCLOAK_DB_PASSWORD:?KEYCLOAK_DB_PASSWORD is required}"

psql --set=ON_ERROR_STOP=1 \
  --username "${POSTGRES_USER}" \
  --dbname postgres \
  --set=keycloak_database="${KEYCLOAK_DB}" \
  --set=keycloak_user="${KEYCLOAK_DB_USERNAME}" \
  --set=keycloak_password="${KEYCLOAK_DB_PASSWORD}" <<'SQL'
SELECT format(
  'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE',
  :'keycloak_user',
  :'keycloak_password'
)
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'keycloak_user') \gexec

SELECT format(
  'CREATE DATABASE %I OWNER %I ENCODING %L',
  :'keycloak_database',
  :'keycloak_user',
  'UTF8'
)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'keycloak_database') \gexec

SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'keycloak_database') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'keycloak_database', :'keycloak_user') \gexec
SQL
