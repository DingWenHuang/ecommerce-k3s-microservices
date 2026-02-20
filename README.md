## 前端入口

- 前端（React）：http://shop.localtest.me
- 後端 API（Gateway）：http://api.localtest.me

### 角色
- USER：可瀏覽商品、加入購物車、下單、查訂單
- ADMIN：可新增商品、補貨（Admin 頁面）



---

# 壓測與不超賣證據

（以下內容由 k6 summary 自動生成，詳見 `loadtest/results/README_SNIPPET.md`）

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
