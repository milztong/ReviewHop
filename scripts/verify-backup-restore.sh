#!/usr/bin/env bash
set -euo pipefail

run_id="${GITHUB_RUN_ID:-local}-$$"
prefix="ankileet-backup-test-${run_id}"
network="${prefix}-network"
source_container="${prefix}-source"
restore_container="${prefix}-restore"
repository_volume="${prefix}-repository"
image="reviewhop-backup:test"
work_directory="$(mktemp -d)"

cleanup() {
  docker rm --force "${source_container}" "${restore_container}" >/dev/null 2>&1 || true
  docker volume rm --force "${repository_volume}" >/dev/null 2>&1 || true
  docker network rm "${network}" >/dev/null 2>&1 || true
  rm -rf "${work_directory}"
}
trap cleanup EXIT

wait_for_postgres() {
  local container="$1"
  local username="$2"
  local database="$3"

  # The PostgreSQL entrypoint uses a socket-only server while it loads fixtures.
  for _ in {1..30}; do
    if docker exec "${container}" pg_isready --host 127.0.0.1 --timeout 1 \
      --username "${username}" --dbname "${database}" --quiet; then
      return 0
    fi
    sleep 1
  done

  printf 'PostgreSQL in %s did not become ready over TCP.\n' "${container}" >&2
  docker logs --tail 50 "${container}" >&2 || true
  return 1
}

printf '%s' 'backup-test-database-password' > "${work_directory}/postgres-password"
printf '%s' 'backup-test-read-only-password' > "${work_directory}/backup-db-password"
printf '%s' 'backup-test-restic-password-with-sufficient-entropy' > "${work_directory}/restic-password"
chmod 600 "${work_directory}/postgres-password" "${work_directory}/backup-db-password" \
  "${work_directory}/restic-password"

cat > "${work_directory}/fixture.sql" <<'SQL'
CREATE TABLE app_user (id UUID PRIMARY KEY);
CREATE TABLE learning_item (id UUID PRIMARY KEY);
CREATE TABLE review_card (id UUID PRIMARY KEY);
CREATE TABLE review_log (id UUID PRIMARY KEY);
CREATE TABLE sync_outbox_event (id UUID PRIMARY KEY);
INSERT INTO app_user (id) VALUES ('00000000-0000-0000-0000-000000000001');

CREATE DATABASE keycloak;
\connect keycloak
CREATE TABLE realm (id VARCHAR(36) PRIMARY KEY, name VARCHAR(255) NOT NULL);
CREATE TABLE client (
  id VARCHAR(36) PRIMARY KEY,
  realm_id VARCHAR(36) NOT NULL REFERENCES realm(id),
  client_id VARCHAR(255) NOT NULL
);
INSERT INTO realm (id, name) VALUES ('realm-ankileet', 'ankileet');
INSERT INTO client (id, realm_id, client_id)
VALUES ('client-ankileet-web', 'realm-ankileet', 'ankileet-web');

CREATE ROLE ankileet_backup LOGIN PASSWORD 'backup-test-read-only-password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE IN ROLE pg_read_all_data;
GRANT CONNECT ON DATABASE ankileet TO ankileet_backup;
GRANT CONNECT ON DATABASE keycloak TO ankileet_backup;
SQL

docker build --tag "${image}" deploy/backup
docker network create "${network}" >/dev/null
docker volume create "${repository_volume}" >/dev/null

docker run --detach --name "${source_container}" --network "${network}" \
  --env POSTGRES_DB=ankileet \
  --env POSTGRES_USER=ankileet_migrator \
  --env POSTGRES_PASSWORD=backup-test-database-password \
  --mount "type=bind,source=${work_directory}/fixture.sql,target=/docker-entrypoint-initdb.d/fixture.sql,readonly" \
  postgres:17.12-alpine >/dev/null

wait_for_postgres "${source_container}" ankileet_migrator ankileet

# A restart must keep both databases available before a backup is attempted.
docker restart "${source_container}" >/dev/null
wait_for_postgres "${source_container}" ankileet_migrator ankileet
docker exec "${source_container}" psql --username ankileet_migrator --dbname ankileet \
  --tuples-only --no-align --command='SELECT count(*) FROM app_user' | grep -qx '1'

common_backup_arguments=(
  --rm
  --read-only
  --network "${network}"
  --tmpfs "/tmp:rw,noexec,nosuid,size=256m"
  --mount "type=volume,source=${repository_volume},target=/repository"
  --mount "type=bind,source=${work_directory}/restic-password,target=/run/secrets/restic-password,readonly"
  --env RESTIC_REPOSITORY=/repository
  --env RESTIC_PASSWORD_FILE=/run/secrets/restic-password
  --env RESTIC_CACHE_DIR=/tmp/restic-cache
  --env BACKUP_HOST=backup-contract-test
)

docker run "${common_backup_arguments[@]}" "${image}" /usr/local/bin/backup.sh init
docker run "${common_backup_arguments[@]}" \
  --mount "type=bind,source=${work_directory}/backup-db-password,target=/run/secrets/backup-db-password,readonly" \
  --env PGHOST="${source_container}" \
  --env PGUSER=ankileet_backup \
  --env BACKUP_DB_PASSWORD_FILE=/run/secrets/backup-db-password \
  --env POSTGRES_DB=ankileet \
  --env KEYCLOAK_DB=keycloak \
  "${image}" /usr/local/bin/backup.sh backup

docker run --detach --name "${restore_container}" --network "${network}" \
  --env POSTGRES_PASSWORD=backup-test-database-password \
  postgres:17.12-alpine >/dev/null
wait_for_postgres "${restore_container}" postgres postgres

docker run "${common_backup_arguments[@]}" \
  --mount "type=bind,source=${work_directory}/postgres-password,target=/run/secrets/postgres-password,readonly" \
  --env RESTORE_CONFIRM_EPHEMERAL_TARGET=RESTORE-EPHEMERAL-ONLY \
  --env RESTORE_PGHOST="${restore_container}" \
  --env RESTORE_PGUSER=postgres \
  --env RESTORE_POSTGRES_PASSWORD_FILE=/run/secrets/postgres-password \
  "${image}" /usr/local/bin/restore-verify.sh

echo 'Backup, restart and isolated restore verification passed.'
