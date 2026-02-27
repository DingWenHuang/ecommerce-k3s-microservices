#!/usr/bin/env node
/**
 * generate_report.js
 * 將 k6 --summary-export 產生的 JSON 轉換為壓測 Markdown 報告
 *
 * 用法（從 load-test/ 目錄）：
 *   node results/generate_report.js [step3_summary.json] [output.md]
 *
 * 範例：
 *   node results/generate_report.js \
 *     results/step3_summary.json \
 *     results/REPORT.md
 *
 * 環境變數（選用，用於覆寫報告內的描述欄位）：
 *   VU_COUNT    - 併發用戶數，預設從 summary 的 vus_max 讀取
 *   DURATION    - 測試 duration 描述，預設 "10s"
 *   PRODUCT_ID  - 商品 ID，預設 "1"
 *   STOCK       - 庫存數量描述（顯示用），預設不顯示
 */

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ── 參數解析 ─────────────────────────────────────────────
const summaryPath = process.argv[2]
    ? path.resolve(process.argv[2])
    : path.join(__dirname, "step3_summary.json");

const outPath = process.argv[3]
    ? path.resolve(process.argv[3])
    : path.join(__dirname, "REPORT.md");

// ── 讀取 JSON ─────────────────────────────────────────────
if (!fs.existsSync(summaryPath)) {
    console.error(`Error: summary file not found: ${summaryPath}`);
    console.error("Usage: node generate_report.js <step3_summary.json> [output.md]");
    process.exit(1);
}

const data = JSON.parse(fs.readFileSync(summaryPath, "utf-8"));
const m    = data.metrics ?? {};

// ── 擷取指標 ─────────────────────────────────────────────
const httpReqsCount = m.http_reqs?.count ?? "N/A";

// http_req_failed.value: 0 = 0% 失敗、1 = 100% 失敗
const failedValue = m.http_req_failed?.value;
const failedRateStr =
    failedValue != null
        ? `${(failedValue * 100).toFixed(2)}%`
        : "N/A";

// k6 summary JSON 中 p95 可能在兩個 key 下
const durationMetric =
    m["http_req_duration{expected_response:true}"] ?? m["http_req_duration"];
const p95Val = durationMetric?.["p(95)"];
const p95Str = p95Val != null ? `${p95Val.toFixed(2)} ms` : "N/A";

// ── 描述欄位（可由環境變數覆寫） ──────────────────────────
const vuCount = process.env.VU_COUNT
    ? Number(process.env.VU_COUNT)
    : (m.vus_max?.max ?? m.vus_max?.value ?? "?");

const duration   = process.env.DURATION   || "10s";
const productId  = process.env.PRODUCT_ID || "1";

// 庫存描述（可選）
const stockNote = process.env.STOCK
    ? `成功數量 **應等於庫存（${process.env.STOCK}）**，不可超賣`
    : `成功數量 **應等於庫存**，不可超賣`;

// ── 產生 Markdown ─────────────────────────────────────────
const content = `## 壓測證據（k6 搶購 ${vuCount.toLocaleString()} 併發）

- 測試工具：k6（Docker 執行）
- 目標：\`POST /flashsale/products/${productId}/join\` + \`GET /flashsale/tickets/{ticketId}\`
- 併發設定：\`constant-vus\`，**${vuCount.toLocaleString()} VUs**，duration **${duration}**
- 驗證重點：
  - ${stockNote}
  - 搶購成功順序應符合 **FIFO 排隊順序**（以 enqueueSeq / successSeq 驗證）

### k6 結果摘要

| 指標 | 數值 |
|------|------|
| 總請求數（http_reqs） | ${httpReqsCount} |
| http_req_failed（網路/腳本失敗率） | ${failedRateStr} |
| http_req_duration p95 | ${p95Str} |

> 備註：FIFO 驗證需搭配後端票券欄位 enqueueSeq/successSeq 或日誌輸出佐證。
`;

// ── 寫出 ──────────────────────────────────────────────────
fs.writeFileSync(outPath, content, "utf-8");
console.log(`[generate_report] Wrote: ${outPath}`);
console.log(`  VUs           : ${vuCount}`);
console.log(`  http_reqs     : ${httpReqsCount}`);
console.log(`  failed rate   : ${failedRateStr}`);
console.log(`  p95 duration  : ${p95Str}`);
