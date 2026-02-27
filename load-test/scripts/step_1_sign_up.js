import http from "k6/http";
import { check } from "k6";

/**
 * Step 1：批次註冊帳號
 *
 * 執行方式（從 load-test/ 目錄）：
 *   k6 run scripts/step_1_sign_up.js
 *
 * 環境變數：
 *   BASE_URL       - 預設 http://localhost
 *   HOST_HEADER    - 預設 api.localtest.me
 *   USER_COUNT     - 預設 100
 *
 * 說明：
 *   每個 VU 負責註冊一個帳號（test001..test{USER_COUNT}）。
 *   若帳號已存在（HTTP 409）視為成功，可重複執行。
 */

export const options = {
    scenarios: {
        register: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.USER_COUNT || "100"),
            iterations: 1,
            maxDuration: "60s",
        },
    },
    thresholds: {
        // 允許 409（already exists）不計入失敗，只要網路層沒有錯誤
        http_req_failed: ["rate<0.01"],
    },
};

const BASE_URL    = __ENV.BASE_URL    || "http://localhost";
const HOST_HEADER = __ENV.HOST_HEADER || "api.localtest.me";

function gwHeaders() {
    return {
        headers: {
            Host: HOST_HEADER,
            "Content-Type": "application/json",
        },
        timeout: "10s",
    };
}

function pad3(n) {
    return String(n).padStart(3, "0");
}

export default function () {
    // __VU 從 1 開始，對應 test001..test{USER_COUNT}
    const username = `test${pad3(__VU)}`;
    const password = username;

    const res = http.post(
        `${BASE_URL}/auth/register`,
        JSON.stringify({ username, password }),
        gwHeaders()
    );

    const ok = check(res, {
        [`register ${username} ok (2xx/409)`]: (r) =>
            [200, 201, 409].includes(r.status),
    });

    if (!ok) {
        console.log(
            `register failed: ${username} status=${res.status} body=${res.body}`
        );
    }
}
