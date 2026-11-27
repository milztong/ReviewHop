#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${MSYSTEM:-}" ]]; then
  export MSYS_NO_PATHCONV=1
fi

output_dir="${1:-artifacts/sbom}"
mkdir -p "${output_dir}"

for component in backend frontend keycloak backup; do
  image="reviewhop-${component}:latest"
  docker image inspect "${image}" >/dev/null
  docker run --rm \
    --volume /var/run/docker.sock:/var/run/docker.sock \
    --volume ankileet-ci-trivy-cache:/root/.cache/trivy \
    aquasec/trivy:0.76.2 image --image-src docker --format cyclonedx "${image}" \
    > "${output_dir}/${component}.cdx.json"
  test -s "${output_dir}/${component}.cdx.json"
done
