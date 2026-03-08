# 電商微服務平台

一個仿電商平台，模擬**商品瀏覽 → 加入購物車 → 下單**的購物流程，並支援 **Redis FIFO 排隊搶購**功能，部署於 Kubernetes (k3s) 並透過 Cloudflare Tunnel 對外服務。

|                 | 連結                                                   |
| --------------- | ------------------------------------------------------ |
|  前端購物網站  | [shop.dingwenhuang.com](https://shop.dingwenhuang.com) |
|  後端 API 入口 | [api.dingwenhuang.com](https://api.dingwenhuang.com)   |

**帳號角色說明**

| 角色  | 可做什麼                                     |
| ----- | -------------------------------------------- |
| USER  | 瀏覽商品、加入購物車、下單、查訂單、參與搶購 |
| ADMIN | 以上全部 + 新增/編輯商品、補貨（Admin 頁面） |

---

## 技術棧

| 分類        | 技術                                    |
| ----------- | --------------------------------------- |
| 後端語言    | Java 17                                 |
| 後端框架    | Spring Boot 3.2、Spring Cloud Gateway   |
| 認證        | Spring Security + JWT (HMAC-SHA256)     |
| 服務間通信  | OpenFeign (HTTP)                        |
| 快取 / 排隊 | Redis（List、Hash、Lua 腳本、Sentinel） |
| 資料庫      | PostgreSQL 16 + JPA (Hibernate)         |
| 容器化      | Docker (multi-stage build)              |
| 容器編排    | Kubernetes (k3s via k3d)                |
| 對外曝露    | Cloudflare Tunnel + Ingress             |
| 前端        | React 19、TypeScript、Vite、Ant Design  |
| 壓測        | k6                                      |

---

## 系統架構

```
使用者瀏覽器
     │ HTTPS
     ▼
Cloudflare Tunnel
     ├─ shop.dingwenhuang.com ──► Ingress ──► React 前端 (Nginx)
     │
     └─ api.dingwenhuang.com ───► Ingress ──► Gateway
                                               │ JWT 驗證 + 路由轉發
                                  ┌────────────┼────────────┐
                                  ▼            ▼            ▼
                             auth-service  product-service  order-service
                                  │            │            │
                                  └────────────┴────────────┘
                                               │
                                   ┌───────────┴───────────┐
                                   ▼                       ▼
                              PostgreSQL                 Redis
                              (StatefulSet + PVC)       (Sentinel)
```

### 微服務職責

| 服務              | 職責                                    |
| ----------------- | --------------------------------------- |
| `auth-service`    | 帳號註冊、登入、JWT 發放（BCrypt 密碼） |
| `gateway`         | JWT 驗簽、路由轉發、CORS 統一處理       |
| `product-service` | 商品 CRUD、庫存管理（原子 SQL 扣減）    |
| `order-service`   | 建立訂單、Flash Sale FIFO 排隊 Worker   |
| `frontend`        | React 靜態網站（Nginx 托管）            |

服務間透過 **Kubernetes Service DNS** 直接溝通，不繞回 Gateway（避免 JWT 認證開銷），內部 API 以獨立 `INTERNAL_API_TOKEN` 驗證。

---

## 核心功能設計

### JWT 認證流程

```
① 使用者 POST /auth/login → auth-service 驗密碼 → 回傳 JWT
② 後續請求帶  Authorization: Bearer <token>
③ Gateway 驗簽 JWT，成功後注入 X-User-Id / X-User-Roles Header 轉發
④ 下游服務從 Header 讀取身份，無需再次驗 JWT
```

JWT 採 HMAC-SHA256 對稱簽章，密鑰透過 k8s Secret 注入環境變數，不寫死在程式碼中。

### 搶購機制（Redis FIFO 排隊）

解決傳統搶購的「超賣」與「不公平」問題：

```
① 使用者點擊「立即搶購」
② Redis Lua 腳本原子執行：INCR 入隊序號 + RPUSH 入隊（不可中斷，FIFO 正確）
③ 前端每秒 polling 票券狀態（兼作心跳，60 秒未 polling 自動視為離線）
④ Background Worker（每 30ms）
   → 取得分布式鎖 → LPOP 出隊 → 扣庫存 → 建訂單 → 更新票券狀態
⑤ 前端收到 SUCCESS，顯示訂單號
```

**防超賣雙保險**

| 層級     | 機制                                              |
| -------- | ------------------------------------------------- |
| Redis 層 | 排隊序列化，Worker 一次處理一張票，無並發扣庫存   |
| 資料庫層 | `UPDATE ... WHERE stock >= amount`，原子 SQL 保底 |

---

## 專案結構

```
ecommerce-k3s-microservices/
├── services/
│   ├── auth-service/       # 登入 / 註冊 / JWT 發放
│   ├── gateway/            # API 路由 / JWT 驗證 / CORS
│   ├── product-service/    # 商品管理 / 庫存扣減
│   └── order-service/      # 訂單 / Flash Sale Worker
├── frontend/
│   └── ecommerce-web/      # React 19 + TypeScript + Ant Design
├── infra/
│   └── k8s/
│       ├── apps/           # Deployment + Service YAML
│       ├── base/           # ConfigMap / Secret
│       ├── postgres/       # PostgreSQL StatefulSet
│       └── redis/          # Redis + Sentinel
└── load-test/              # k6 壓測腳本與結果
```

---

## 壓測驗證（防超賣 / FIFO 公平性）

使用 k6 模擬 100 人同時搶購庫存 10 的商品：

```bash
# Step 1：批次建立 100 個測試帳號
bash load-test/run_step1_sign_up.sh

# Step 2+3：登入並同時發起搶購
bash load-test/run_step2_login_step3_flashsale.sh PRODUCT_ID=1 USER_COUNT=100
```

### 測試結果

| 指標        | 數值                                |
| ----------- | ----------------------------------- |
| 模擬使用者  | 100 人同時搶購                      |
| 商品庫存    | 10                                  |
| 成功搶到    | **10 人**（精確等於庫存，無超賣）   |
| 總請求數    | 1,655                               |
| HTTP 失敗率 | **0.00%**                           |
| P95 延遲    | ~1,403 ms                           |
| FIFO 公平性 | enqueueSeq 與 successSeq 完全吻合 ✓ |

### 驗證 FIFO 結果

```bash
# Port-forward 到 order-service
kubectl port-forward svc/order-service 8081:8080 -n ecommerce

# 查詢搶購結果（enqueueSeq = 入隊順序，successSeq = 處理順序）
curl http://localhost:8081/internal/flashsale/1/winners \
  -H "X-Internal-Token: CHANGE_ME_INTERNAL_TOKEN"
```

回應範例：

```json
{
  "winners": [
    { "username": "test003", "enqueueSeq": 1, "successSeq": 1 },
    { "username": "test051", "enqueueSeq": 2, "successSeq": 2 },
    { "username": "test027", "enqueueSeq": 3, "successSeq": 3 }
  ],
  "fifoVerified": true
}
```

`fifoVerified: true` 代表所有得獎者的入隊順序與處理順序完全一致，FIFO 成立。