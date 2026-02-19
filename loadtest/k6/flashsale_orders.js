import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

/**
 * Step 10：搶購壓測腳本（1000 併發）
 *
 * 你可以用環境變數控制：
 * - BASE_URL：例如 http://localhost
 * - HOST：Ingress host header，例如 api.localtest.me
 * - TOKEN：JWT access token（USER）
 * - PRODUCT_ID：商品 id
 * - UNIT_PRICE：單價（示範用；真實可改為由服務查價格）
 *
 * 注意：
 * - 這裡的「成功數」理論上不應該超過庫存（搭配 Step 9 的 DB 原子扣庫存 + Redis lock）
 */

const okCount = new Counter('order_ok');
const soldOutCount = new Counter('order_sold_out');
const busyCount = new Counter('order_busy');
const otherFailCount = new Counter('order_other_fail');

export const options = {
  scenarios: {
    flashsale: {
      executor: 'constant-vus',
      vus: 1000,
      duration: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'], // 只統計網路/腳本層面的 failed（非 409/429）
    http_req_duration: ['p(95)<2000'], // p95 < 2s（本地環境可視情況調整）
  },
};

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost';
  const host = __ENV.HOST || 'api.localtest.me';
  const token = __ENV.TOKEN;
  const productId = __ENV.PRODUCT_ID || '1';
  const unitPrice = __ENV.UNIT_PRICE || '1999.99';

  // 若 TOKEN 未提供，直接讓測試失敗（避免測到匿名）
  if (!token) {
    throw new Error('ENV TOKEN is required');
  }

  const url = `${baseUrl}/orders`;
  const payload = JSON.stringify({
    productId: Number(productId),
    quantity: 1,
    unitPrice: Number(unitPrice),
  });

  const params = {
    headers: {
      'Host': host,
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    timeout: '10s',
  };

  const res = http.post(url, payload, params);

  // 你的 order-service 設計：
  // 200 = 成功下單
  // 409 = OUT_OF_STOCK（或 NOT_FOUND）
  // 429 = BUSY_TRY_AGAIN（搶購鎖競爭）
  if (res.status === 200) okCount.add(1);
  else if (res.status === 409) soldOutCount.add(1);
  else if (res.status === 429) busyCount.add(1);
  else otherFailCount.add(1);

  check(res, {
    'status is 200/409/429': (r) => [200, 409, 429].includes(r.status),
  });

  // 小 sleep 避免單 VU 在 10 秒內狂打太多次（可調整）
  sleep(0.01);
}