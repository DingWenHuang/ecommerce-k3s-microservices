#!/usr/bin/env node
/**
 * step_2_log_in.js  ── Node.js 腳本（非 k6）
 *
 * 依序登入 test001..test{USER_COUNT}，將 accessToken 寫入 tokens.json。
 * 此步驟為壓測前的準備作業，不需要 k6。
 *
 * 用法（從專案根目錄）：
 *   node load-test/scripts/step_2_log_in.js
 *
 * 環境變數：
 *   BASE_URL      - 預設 http://localhost
 *   HOST_HEADER   - 預設 api.localtest.me
 *   USER_COUNT    - 預設 100
 *   TOKEN_FILE    - 輸出路徑，預設 load-test/results/tokens.json
 */

import http from "http";
import https from "https";
import fs from "fs";
import path from "path";

const BASE_URL    = process.env.BASE_URL    || "http://localhost";
const HOST_HEADER = process.env.HOST_HEADER || "api.localtest.me";
const USER_COUNT  = Number(process.env.USER_COUNT  || "100");
const TOKEN_FILE  = process.env.TOKEN_FILE  || "load-test/results/tokens.json";

function pad3(n) {
    return String(n).padStart(3, "0");
}

function postJson(url, body, extraHeaders = {}) {
    return new Promise((resolve, reject) => {
        const parsed   = new URL(url);
        const bodyStr  = JSON.stringify(body);
        const lib      = parsed.protocol === "https:" ? https : http;
        const options  = {
            hostname : parsed.hostname,
            port     : parsed.port || (parsed.protocol === "https:" ? 443 : 80),
            path     : parsed.pathname + parsed.search,
            method   : "POST",
            headers  : {
                Host             : HOST_HEADER,
                "Content-Type"   : "application/json",
                "Content-Length" : Buffer.byteLength(bodyStr),
                ...extraHeaders,
            },
        };

        const req = lib.request(options, (res) => {
            let data = "";
            res.on("data", (chunk) => (data += chunk));
            res.on("end", () => resolve({ status: res.statusCode, body: data }));
        });

        req.on("error", reject);
        req.write(bodyStr);
        req.end();
    });
}

async function main() {
    const results = [];

    for (let i = 1; i <= USER_COUNT; i++) {
        const username = `test${pad3(i)}`;
        const password = username;

        try {
            const { status, body } = await postJson(
                `${BASE_URL}/auth/login`,
                { username, password }
            );

            if (status !== 200) {
                console.error(`[WARN] login failed: ${username} status=${status} body=${body}`);
                continue;
            }

            const json  = JSON.parse(body);
            const token = json.accessToken;

            if (!token) {
                console.error(`[WARN] token missing: ${username} body=${body}`);
                continue;
            }

            results.push({ username, token });

            if (i % 10 === 0) {
                process.stdout.write(`  logged in ${i}/${USER_COUNT}\r`);
            }
        } catch (err) {
            console.error(`[ERROR] ${username}:`, err.message);
        }
    }

    console.log(`\nLogged in ${results.length}/${USER_COUNT} users`);

    // ── 寫入 tokens.json ──────────────────────────────────
    fs.mkdirSync(path.dirname(path.resolve(TOKEN_FILE)), { recursive: true });
    fs.writeFileSync(TOKEN_FILE, JSON.stringify({ tokens: results }, null, 2), "utf-8");
    console.log(`Tokens written to ${TOKEN_FILE}`);

    if (results.length < USER_COUNT) {
        console.error(`[WARN] Only ${results.length}/${USER_COUNT} tokens obtained. Check if accounts are registered.`);
        process.exit(1);
    }
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
