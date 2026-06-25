#!/usr/bin/env bash

# JWKS endpoint outage -> cached keys keep auth working; cold fetches retry then fail.
set -euo pipefail
cd "$(dirname "$0")"; source ./lib.sh
proxy_enabled keycloak false
