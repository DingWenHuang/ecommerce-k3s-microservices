## 壓測證據（k6 搶購 100 併發）

- 測試工具：k6（Docker 執行）
- 目標：`POST /flashsale/products/1/join` + `GET /flashsale/tickets/{ticketId}`
- 併發設定：`constant-vus`，**100 VUs**，duration **10s**
- 驗證重點：
  - 成功數量 **應等於庫存**，不可超賣
  - 搶購成功順序應符合 **FIFO 排隊順序**（以 enqueueSeq / successSeq 驗證）

### k6 結果摘要

| 指標 | 數值 |
|------|------|
| 總請求數（http_reqs） | 1655 |
| http_req_failed（網路/腳本失敗率） | 0.00% |
| http_req_duration p95 | 1403.50 ms |

> 備註：FIFO 驗證需搭配後端票券欄位 enqueueSeq/successSeq 或日誌輸出佐證。
