import http from "k6/http";
import { check, sleep } from "k6";
import exec from "k6/execution";

/**
 * Step 3：搶購壓力測試（join queue + poll ticket）
 *
 * 執行方式（從 load-test/ 目錄）：
 *   k6 run scripts/step_3_flashsale.js \
 *       --summary-export results/step3_summary.json
 *
 * 環境變數：
 *   BASE_URL         - 預設 http://localhost
 *   HOST_HEADER      - 預設 api.localtest.me
 *   PRODUCT_ID       - 預設 1
 *   USER_COUNT       - 預設 100（須與 step_2 保持一致）
 *   POLL_MAX_SECONDS - 最長輪詢秒數，預設 20
 *   POLL_INTERVAL_MS - 輪詢間隔毫秒，預設 500
 *
 * 前置條件：
 *   必須先執行 step_2_log_in.js，確保 results/tokens.json 已產生。
 *
 * 說明：
 *   以 open() 在 init 階段讀取 tokens.json。
 *   每個 VU 使用對應 index 的 token 執行：
 *     1. POST /flashsale/products/{PRODUCT_ID}/join  → 取得 ticketId
 *     2. 輪詢 GET /flashsale/tickets/{ticketId}     → 等待最終狀態
 */

// open() 在 init 階段執行，路徑相對於腳本檔案位置
const rawTokens = JSON.parse(open("../results/tokens.json"));
// rawTokens 格式：{ tokens: [ { username, token }, ... ] }
const TOKEN_LIST = rawTokens.tokens || [];

const USER_COUNT = Number(__ENV.USER_COUNT || TOKEN_LIST.length || "100");

export const options = {
    scenarios: {
        flashsale: {
            executor: "per-vu-iterations",
            vus: USER_COUNT,
            iterations: 1,       // 每個 VU 只做一次 join + poll
            maxDuration: "60s",
            exec: "runFlow",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.01"],
    },
};

const BASE_URL         = __ENV.BASE_URL         || "http://localhost";
const HOST_HEADER      = __ENV.HOST_HEADER      || "api.localtest.me";
const PRODUCT_ID       = Number(__ENV.PRODUCT_ID       || "1");
const POLL_MAX_SECONDS = Number(__ENV.POLL_MAX_SECONDS || "20");
const POLL_INTERVAL_MS = Number(__ENV.POLL_INTERVAL_MS || "500");

function gwHeaders(extraHeaders) {
    const h = {
        Host: HOST_HEADER,
        "Content-Type": "application/json",
    };
    if (extraHeaders) {
        Object.keys(extraHeaders).forEach((k) => {
            h[k] = extraHeaders[k];
        });
    }
    return { headers: h, timeout: "10s" };
}

export function runFlow() {
    // exec.vu.idInTest 從 1 開始，對應 TOKEN_LIST index 0..n-1
    const vuIndex = exec.vu.idInTest - 1;
    const entry   = TOKEN_LIST[vuIndex];

    if (!entry || !entry.token) {
        console.log(`VU ${exec.vu.idInTest} missing token, skip`);
        return;
    }

    const token = entry.token;

    // 1) 加入搶購隊列
    const joinRes = http.post(
        `${BASE_URL}/flashsale/products/${PRODUCT_ID}/join`,
        JSON.stringify({}),
        gwHeaders({ Authorization: `Bearer ${token}` })
    );

    const joinOk = check(joinRes, {
        "join status is 200": (r) => r.status === 200,
    });

    if (!joinOk) {
        if (exec.vu.idInTest <= 5) {
            console.log(`join failed: status=${joinRes.status} body=${joinRes.body}`);
        }
        return;
    }

    const ticketId = joinRes.json("ticketId");
    if (!ticketId) return;

    // 2) 輪詢票券狀態，直到終態或逾時
    const start = Date.now();
    while ((Date.now() - start) / 1000 < POLL_MAX_SECONDS) {
        const statusRes = http.get(
            `${BASE_URL}/flashsale/tickets/${ticketId}`,
            gwHeaders({ Authorization: `Bearer ${token}` })
        );

        if (statusRes.status !== 200) {
            sleep(POLL_INTERVAL_MS / 1000);
            continue;
        }

        const status = statusRes.json("status");
        if (
            status === "SUCCESS" ||
            status === "SOLD_OUT" ||
            status === "EXPIRED"
        ) {
            break;
        }

        sleep(POLL_INTERVAL_MS / 1000);
    }

    sleep(0.01);
}
