#!/usr/bin/env bash
# Step 1：批次註冊帳號 test001..test{USER_COUNT}
# 用法：bash load-test/run_step1_sign_up.sh
#       BASE_URL=http://my-host bash load-test/run_step1_sign_up.sh
set -euo pipefail

mkdir -p load-test/results

BASE_URL="${BASE_URL:-http://localhost}"
HOST_HEADER="${HOST_HEADER:-api.localtest.me}"
USER_COUNT="${USER_COUNT:-100}"

echo "==> [Step 1] Registering ${USER_COUNT} users ..."

docker run --rm -i \
  --network host \
  --user "$(id -u):$(id -g)" \
  -v "$(pwd):/work" -w /work \
  -e BASE_URL="$BASE_URL" \
  -e HOST_HEADER="$HOST_HEADER" \
  -e USER_COUNT="$USER_COUNT" \
  grafana/k6:0.49.0 run /work/load-test/scripts/step_1_sign_up.js \
  --summary-export=/work/load-test/results/step1_summary.json

echo "==> [Step 1] Done. Summary: load-test/results/step1_summary.json"
