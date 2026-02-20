# ===== Stage 1：Build（Node）=====
FROM node:20-alpine AS builder
WORKDIR /app

# 先只 copy package 檔，確保依賴 cache 可利用
COPY package.json package-lock.json ./
RUN npm ci

# copy source
COPY . .

# 使用 production env build
RUN npm run build

# ===== Stage 2：Run（Nginx）=====
FROM nginx:1.27-alpine

# 放入自訂 nginx 設定（處理 SPA fallback）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 靜態檔位置
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
