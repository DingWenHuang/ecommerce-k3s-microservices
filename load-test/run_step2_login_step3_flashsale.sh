#!/usr/bin/env bash
# Step 2 + 3：登入取得 token → 搶購壓測 → 產生 MD 報告
#
# 用法：bash load-test/run_step2_login_step3_flashsale.sh
#       BASE_URL=http://my-host PRODUCT_ID=2 bash load-test/run_step2_login_step3_flashsale.sh
#
# 前置條件：先執行 run_step1_sign_up.sh 確保帳號已建立
set -euo pipefail

mkdir -p load-test/results

BASE_URL="${BASE_URL:-http://localhost}"
HOST_HEADER="${HOST_HEADER:-api.localtest.me}"
PRODUCT_ID="${PRODUCT_ID:-1}"
USER_COUNT="${USER_COUNT:-100}"

# ── Step 2：以 Node.js 登入並匯出 tokens ─────────────────
# 說明：k6 的 handleSummary() 在獨立 JS runtime 執行，
#       無法讀取 setup() 中設定的模組級變數，
#       因此改用 Node.js 直接寫入 tokens.json。
echo "==> [Step 2] Logging in ${USER_COUNT} users and exporting tokens ..."

BASE_URL="$BASE_URL" \
HOST_HEADER="$HOST_HEADER" \
USER_COUNT="$USER_COUNT" \
TOKEN_FILE="load-test/results/tokens.json" \
node load-test/scripts/step_2_log_in.js

echo "==> [Step 2] Tokens written to load-test/results/tokens.json"

# ── Step 3：搶購壓測 ──────────────────────────────────────
echo "==> [Step 3] Running flashsale load test (${USER_COUNT} VUs, product=${PRODUCT_ID}) ..."

docker run --rm -i \
  --network host \
  --user "$(id -u):$(id -g)" \
  -v "$(pwd):/work" -w /work \
  -e BASE_URL="$BASE_URL" \
  -e HOST_HEADER="$HOST_HEADER" \
  -e PRODUCT_ID="$PRODUCT_ID" \
  -e USER_COUNT="$USER_COUNT" \
  -e POLL_MAX_SECONDS="${POLL_MAX_SECONDS:-20}" \
  -e POLL_INTERVAL_MS="${POLL_INTERVAL_MS:-500}" \
  grafana/k6:0.49.0 run /work/load-test/scripts/step_3_flashsale.js \
  --summary-export=/work/load-test/results/step3_summary.json

echo "==> [Step 3] Summary: load-test/results/step3_summary.json"

# ── 產生 MD 報告 ──────────────────────────────────────────
echo "==> Generating Markdown report ..."

node load-test/results/generate_report.js \
  load-test/results/step3_summary.json \
  load-test/results/REPORT.md

echo "==> Report: load-test/results/REPORT.md"
