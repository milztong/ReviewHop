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
  printf '{"timestamp":"%s","level":"ERROR","event":"backup_failed","message":"%s"}\n' \
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

validate_identifier() {
  identifier="$1"
  case "${identifier}" in
    ''|*[!A-Za-z0-9_]*) fail "Database identifiers may contain only letters, digits and underscores" ;;
  esac
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

initialize_repository() {
  configure_repository_credentials
  if restic cat config >/dev/null 2>&1; then
    log repository_exists "Backup repository is already initialized"
    return
  fi
  restic init
  log repository_initialized "Encrypted backup repository initialized"
}

create_backup() {
  require_variable PGHOST
  require_variable PGUSER
  require_variable BACKUP_DB_PASSWORD_FILE
  require_variable POSTGRES_DB
  require_variable KEYCLOAK_DB
  configure_repository_credentials
  validate_identifier "${POSTGRES_DB}"
  validate_identifier "${KEYCLOAK_DB}"

  export PGPORT="${PGPORT:-5432}"
  PGPASSWORD="$(read_secret "${BACKUP_DB_PASSWORD_FILE}")"
  export PGPASSWORD
  backup_host="${BACKUP_HOST:-ankileet-demo}"
  work_root="$(mktemp -d /tmp/ankileet-backup.XXXXXX)"
  snapshot_directory="${work_root}/snapshot"
  trap 'rm -rf "${work_root}"' EXIT
  trap 'exit 130' HUP INT TERM
  mkdir -p "${snapshot_directory}"

  run_id="$(date -u +%Y%m%dT%H%M%SZ)"
  log backup_started "Creating logical PostgreSQL dumps"
  pg_dump --format=custom --no-owner --no-privileges \
    --file="${snapshot_directory}/ankileet.dump" --dbname="${POSTGRES_DB}"
  pg_dump --format=custom --no-owner --no-privileges \
    --file="${snapshot_directory}/keycloak.dump" --dbname="${KEYCLOAK_DB}"

  (
    cd "${snapshot_directory}"
    sha256sum ankileet.dump keycloak.dump > manifest.sha256
  )
  {
    printf 'format_version=1\n'
    printf 'created_at=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'run_id=%s\n' "${run_id}"
    printf 'postgres_database=%s\n' "${POSTGRES_DB}"
    printf 'keycloak_database=%s\n' "${KEYCLOAK_DB}"
    printf 'pg_dump_version=%s\n' "$(pg_dump --version)"
  } > "${snapshot_directory}/manifest.properties"

  (
    cd "${snapshot_directory}"
    restic backup . --host "${backup_host}" --tag ankileet-postgresql --tag automated
  )
  log backup_stored "Encrypted backup snapshot stored"

  restic check
  restic forget --prune --host "${backup_host}" --tag ankileet-postgresql \
    --keep-daily 7 --keep-weekly 4 --keep-monthly 3
  log backup_completed "Backup integrity and retention policy completed"
}

case "${1:-backup}" in
  init) initialize_repository ;;
  backup) create_backup ;;
  *) fail "Usage: backup.sh [init|backup]" ;;
esac
