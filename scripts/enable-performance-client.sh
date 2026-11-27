#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${MSYSTEM:-}" ]]; then
  export MSYS_NO_PATHCONV=1
fi

required_variables=(KEYCLOAK_ADMIN_USERNAME KEYCLOAK_ADMIN_PASSWORD)
for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "${variable_name} is required" >&2
    exit 1
  fi
done

kcadm="/opt/keycloak/bin/kcadm.sh"

run_kcadm() {
  docker compose exec -T keycloak "${kcadm}" "$@"
}

authenticated=false
for _ in {1..30}; do
  if run_kcadm config credentials \
    --server http://localhost:8080 \
    --realm master \
    --user "${KEYCLOAK_ADMIN_USERNAME}" \
    --password "${KEYCLOAK_ADMIN_PASSWORD}" \
    >/dev/null 2>&1; then
    authenticated=true
    break
  fi
  sleep 2
done

if [[ "${authenticated}" != "true" ]]; then
  echo "Keycloak did not become ready within 60 seconds" >&2
  exit 1
fi

client_id="$(run_kcadm get clients \
  -r ankileet \
  -q clientId=ankileet-web \
  --fields id \
  --format csv \
  --noquotes | tr -d '\r\n')"

if [[ -z "${client_id}" ]]; then
  echo "The ankileet-web client was not found" >&2
  exit 1
fi

run_kcadm update "clients/${client_id}" \
  -r ankileet \
  -s serviceAccountsEnabled=true \
  >/dev/null

echo "Enabled client credentials for the ephemeral performance environment."
