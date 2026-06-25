#!/usr/bin/env bash

# Redis outage -> L2 degrades to a miss, requests still served from MongoDB (200).
set -euo pipefail
cd "$(dirname "$0")"; source ./lib.sh
proxy_enabled redis false
