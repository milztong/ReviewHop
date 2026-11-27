#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${MSYSTEM:-}" ]]; then
  export MSYS_NO_PATHCONV=1
fi

required_variables=(
  KEYCLOAK_ADMIN_USERNAME
  KEYCLOAK_ADMIN_PASSWORD
  E2E_ALICE_USERNAME
  E2E_ALICE_PASSWORD
  E2E_BOB_USERNAME
  E2E_BOB_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "${variable_name} is required" >&2
    exit 1
  fi
done

keycloak_url="${KEYCLOAK_PUBLIC_URL:-http://localhost:8180}"
kcadm="/opt/keycloak/bin/kcadm.sh"

keycloak_ready=false
for _ in $(seq 1 60); do
  if curl --fail --silent --show-error \
    "${keycloak_url}/realms/ankileet/.well-known/openid-configuration" \
    >/dev/null; then
    keycloak_ready=true
    break
  fi
  sleep 2
done

if [[ "${keycloak_ready}" != "true" ]]; then
  echo "Keycloak did not become ready at ${keycloak_url}" >&2
  exit 1
fi

run_kcadm() {
  docker compose exec -T keycloak "${kcadm}" "$@"
}

run_kcadm config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user "${KEYCLOAK_ADMIN_USERNAME}" \
  --password "${KEYCLOAK_ADMIN_PASSWORD}" \
  >/dev/null
echo "Authenticated to the local Keycloak admin API."

provision_user() {
  local username="$1"
  local password="$2"
  local first_name="$3"
  local user_id

  echo "Provisioning Keycloak user ${username}."
  user_id="$(run_kcadm get users \
    -r ankileet \
    -q exact=true \
    -q "username=${username}" \
    --fields id \
    --format csv \
    --noquotes | tr -d '\r\n')"

  if [[ -z "${user_id}" ]]; then
    run_kcadm create users \
      -r ankileet \
      -s "username=${username}" \
      -s "email=${username}@ankileet.invalid" \
      -s "firstName=${first_name}" \
      -s lastName=Tester \
      -s enabled=true \
      -s emailVerified=true \
      >/dev/null
    echo "Created Keycloak user ${username}."
  else
    run_kcadm update "users/${user_id}" \
      -r ankileet \
      -s "firstName=${first_name}" \
      -s lastName=Tester \
      -s enabled=true \
      -s emailVerified=true \
      >/dev/null
    echo "Updated Keycloak profile for ${username}."
  fi

  run_kcadm set-password \
    -r ankileet \
    --username "${username}" \
    --new-password "${password}" \
    --temporary=false \
    >/dev/null
  echo "Configured a non-temporary E2E credential for ${username}."
}

provision_user "${E2E_ALICE_USERNAME}" "${E2E_ALICE_PASSWORD}" Alice
provision_user "${E2E_BOB_USERNAME}" "${E2E_BOB_PASSWORD}" Bob

echo "Provisioned two ephemeral Keycloak E2E users."
