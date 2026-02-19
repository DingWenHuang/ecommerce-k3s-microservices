import json
from pathlib import Path

p = Path("loadtest/results/k6_summary.json")
data = json.loads(p.read_text(encoding="utf-8"))

metrics = data.get("metrics", {})
def get_metric(name, field="value", default=None):
    m = metrics.get(name, {})
    return m.get(field, default)

# k6 built-in
p95 = get_metric("http_req_duration", "p(95)")
reqs = get_metric("http_reqs", "count", 0)
failed_rate = get_metric("http_req_failed", "rate", 0.0)

# custom counters
ok = get_metric("order_ok", "count", 0)
sold = get_metric("order_sold_out", "count", 0)
busy = get_metric("order_busy", "count", 0)
other = get_metric("order_other_fail", "count", 0)

snippet = f"""## 壓測證據（k6 搶購 1000 併發）

- 測試工具：k6（Docker 執行）
- 目標：`POST /orders`（透過 Ingress → Gateway → order-service）
- 併發設定：`constant-vus`，**1000 VUs**，duration **10s**
- 驗證重點：成功下單數量 **不應超過庫存**（搭配 Redis lock + PostgreSQL 原子扣庫存）

### 結果摘要
- 總請求數（http_reqs）：{reqs}
- http_req_failed（網路/腳本失敗率）：{failed_rate:.4f}
- http_req_duration p95：{p95:.2f} ms

### 業務結果（依 HTTP status 分類）
- ✅ 成功下單（200）：{ok}
- ⚠️ 售完/不足（409）：{sold}
- ⏳ 忙碌稍後重試（429）：{busy}
- ❓ 其他狀態：{other}

> 備註：成功數理論上不會超過庫存；售完/忙碌屬於預期行為（搶購場景）
"""
Path("loadtest/results/README_SNIPPET.md").write_text(snippet, encoding="utf-8")
print("Generated: loadtest/results/README_SNIPPET.md")