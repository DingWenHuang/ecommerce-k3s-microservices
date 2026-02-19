## 壓測證據（k6 搶購 1000 併發）

- 測試工具：k6（Docker 執行）
- 目標：`POST /orders`（透過 Ingress → Gateway → order-service）
- 併發設定：`constant-vus`，**1000 VUs**，duration **10s**
- 驗證重點：成功下單數量 **不應超過庫存**（搭配 Redis lock + PostgreSQL 原子扣庫存）

### 結果摘要
- 總請求數（http_reqs）：3472
- http_req_failed（網路/腳本失敗率）：0.0000
- http_req_duration p95：5394.95 ms

### 業務結果（依 HTTP status 分類）
- ✅ 成功下單（200）：91
- ⚠️ 售完/不足（409）：0
- ⏳ 忙碌稍後重試（429）：3381
- ❓ 其他狀態：0

> 備註：成功數理論上不會超過庫存；售完/忙碌屬於預期行為（搶購場景）
