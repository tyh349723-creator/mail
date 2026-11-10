# 前端镜像：Node 构建静态资源，Nginx 托管并反代 /api 到网关
# syntax=docker/dockerfile:1
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
# 验签密钥由 docker compose --build-arg 从 .env 注入，不写入仓库；
# Vite 会优先读取 process.env 中已存在的 VITE_ 变量，覆盖 .env.production 内的占位值
ARG VITE_SIGN_SECRET
ENV VITE_SIGN_SECRET=$VITE_SIGN_SECRET
RUN npm run build

FROM nginx:1.27-alpine
ENV TZ=Asia/Shanghai
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
