#!/usr/bin/env sh
set -eu

umask 077

log() {
  event="$1"
  message="$2"
  printf '{"timestamp":"%s","level":"INFO","event":"%s","message":"%s"}\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${event}" "${message}"
}

fail() {
  printf '{"timestamp":"%s","level":"ERROR","event":"restore_verification_failed","message":"%s"}\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1" >&2
  exit 1
}

require_variable() {
  variable_name="$1"
  eval "variable_value=\${${variable_name}:-}"
  [ -n "${variable_value}" ] || fail "Required variable ${variable_name} is missing"
}

read_secret() {
  secret_file="$1"
  [ -f "${secret_file}" ] || fail "Required secret file is missing"
  secret_value="$(cat "${secret_file}")"
  [ -n "${secret_value}" ] || fail "Required secret file is empty"
  printf '%s' "${secret_value}"
}

configure_repository_credentials() {
  require_variable RESTIC_REPOSITORY
  require_variable RESTIC_PASSWORD_FILE
  [ -f "${RESTIC_PASSWORD_FILE}" ] || fail "Restic password file is missing"
  case "${RESTIC_REPOSITORY}" in
    s3:*)
      require_variable BACKUP_S3_ACCESS_KEY_FILE
      require_variable BACKUP_S3_SECRET_KEY_FILE
      AWS_ACCESS_KEY_ID="$(read_secret "${BACKUP_S3_ACCESS_KEY_FILE}")"
      AWS_SECRET_ACCESS_KEY="$(read_secret "${BACKUP_S3_SECRET_KEY_FILE}")"
      export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
      ;;
  esac
}

require_variable RESTORE_CONFIRM_EPHEMERAL_TARGET
[ "${RESTORE_CONFIRM_EPHEMERAL_TARGET}" = "RESTORE-EPHEMERAL-ONLY" ] || \
  fail "Restore verification requires an explicit ephemeral-target confirmation"
require_variable RESTORE_PGHOST
require_variable RESTORE_PGUSER
require_variable RESTORE_POSTGRES_PASSWORD_FILE
configure_repository_credentials

export PGHOST="${RESTORE_PGHOST}"
export PGPORT="${RESTORE_PGPORT:-5432}"
export PGUSER="${RESTORE_PGUSER}"
PGPASSWORD="$(read_secret "${RESTORE_POSTGRES_PASSWORD_FILE}")"
export PGPASSWORD

app_database="ankileet_restore_verify"
keycloak_database="keycloak_restore_verify"
backup_host="${BACKUP_HOST:-ankileet-demo}"
work_root="$(mktemp -d /tmp/ankileet-restore.XXXXXX)"

cleanup() {
  psql --dbname=postgres --set=ON_ERROR_STOP=1 \
    --command="DROP DATABASE IF EXISTS ${app_database} WITH (FORCE)" >/dev/null 2>&1 || true
  psql --dbname=postgres --set=ON_ERROR_STOP=1 \
    --command="DROP DATABASE IF EXISTS ${keycloak_database} WITH (FORCE)" >/dev/null 2>&1 || true
  rm -rf "${work_root}"
}
trap cleanup EXIT
trap 'exit 130' HUP INT TERM

log restore_started "Restoring latest encrypted snapshot into isolated databases"
restic restore latest --host "${backup_host}" --tag ankileet-postgresql \
  --target "${work_root}"

(
  cd "${work_root}"
  sha256sum -c manifest.sha256
)

for database_name in "${app_database}" "${keycloak_database}"; do
  psql --dbname=postgres --set=ON_ERROR_STOP=1 \
    --command="DROP DATABASE IF EXISTS ${database_name} WITH (FORCE)"
  psql --dbname=postgres --set=ON_ERROR_STOP=1 \
    --command="CREATE DATABASE ${database_name} TEMPLATE template0 ENCODING 'UTF8'"
done

pg_restore --exit-on-error --no-owner --no-privileges \
  --dbname="${app_database}" "${work_root}/ankileet.dump"
pg_restore --exit-on-error --no-owner --no-privileges \
  --dbname="${keycloak_database}" "${work_root}/keycloak.dump"

app_table_count="$(psql --tuples-only --no-align --dbname="${app_database}" --command="
  SELECT count(*)
  FROM information_schema.tables
  WHERE table_schema = 'public'
    AND table_name IN ('app_user', 'learning_item', 'review_card', 'review_log', 'sync_outbox_event');
")"
[ "${app_table_count}" = "5" ] || fail "Restored ReviewHop schema is incomplete"

realm_count="$(psql --tuples-only --no-align --dbname="${keycloak_database}" --command="
  SELECT count(*) FROM realm WHERE name = 'ankileet';
")"
[ "${realm_count}" -ge 1 ] || fail "Restored Keycloak realm is missing"

client_count="$(psql --tuples-only --no-align --dbname="${keycloak_database}" --command="
  SELECT count(*)
  FROM client c
  JOIN realm r ON r.id = c.realm_id
  WHERE r.name = 'ankileet';
")"
[ "${client_count}" -ge 1 ] || fail "Restored Keycloak clients are missing"

log restore_verified "Checksums, ReviewHop schema, Keycloak realm and clients verified"
