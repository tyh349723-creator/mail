# 多商家电商平台 Mall-X

多商家电商平台：覆盖商户入驻审核、商品管理、购物车、用户下单、渠道支付、订单履约、售后退款、商户钱包提现、交易数据统计可视化等电商全链路业务。

## 核心角色

- **平台管理员**：商户入驻审核、支付渠道配置、订单/退款管控、提现审核、数据看板
- **入驻商户**：店铺信息、商品上下架、订单发货、收款提现、经营统计
- **普通用户**：浏览商品、离线购物车、下单支付、订单管理、售后

## 技术架构

### 后端（backend/）

按《行业工程实践》实验指导书三模块布局（外部统一经网关口进入）：

| 服务 | 端口 | 说明 |
|------|------|------|
| mall-gateway | 10010 | API 网关：全局 MD5 验签、Redis+Lua 限流、JWT 鉴权与身份注入、跨域 |
| mall-mgr | 10011 | 业务微服务：商户/用户/商品/购物车/订单/管理端/平台统计 |
| mall-alipay | 10012 | 支付微服务：渠道配置/支付收银台/回调幂等/退款/钱包提现/商户统计 |

**本阶段「数据库先不用」**：全部业务数据存于 `mall-common` 提供的 `MemoryRepository<T>` 内存仓储（启动时加载种子数据），仓储已抽象，接入 MySQL/MyBatis 时仅替换实现、业务代码不变。Redis 可用即启用（Lua 限流、Lua 库存扣减、支付回调分布式锁、缓存），不可用自动降级为内存实现，保证无 Redis 也能启动演示。

技术栈：JDK 21 + Spring Boot 3.2 + Spring Cloud Gateway（Reactive）+ Spring WebFlux/Web MVC + Redis。参考指导书要点做了等价降级：`@Scheduled` 扫单代替 MQ 延迟关单、Redis Lua 固定窗口代替 Sentinel 限流、Redis 锁代替 Redisson 分布式锁（代码注释中留有切换点）。

### 前端（统一单工程 · 单端口）

`frontend/` 为**单一 Vue3 工程**，只有一个入口（角色选择门户），登录后按角色在同一 SPA 内跳转（无跨端口跳转、无 URL 传 token）：

| 项 | 说明 |
|------|------|
| 技术栈 | Vue3 + TypeScript + Vite + Pinia + Vue Router + Element Plus |
| 默认端口 | 3000（vite 代理 `/api` → `:10010`，保留 Expect 头剥离的 Netty 兼容补丁） |
| 路由命名空间 | `/` 角色门户 | `/user/**` 用户端 H5 | `/mch/**` 商家端 H5 | `/admin/**` 管理后台 |
| 登录态 | localStorage `mall_token` + `mall_role`（user / mch / admin） |

## 环境要求

- JDK 21 + Maven 3.6+
- Node.js 18+（npm）
- Redis（可选：缺失自动降级内存）
- MySQL（当前阶段无需）

## 快速启动

### 1. 后端（三个服务）

```bash
cd backend
mvn clean package -DskipTests
# 三个终端分别执行（或使用脚本后台启动）：
java -jar mall-gateway/target/mall-gateway-1.0.0.jar      # 10010
java -jar mall-mgr/target/mall-mgr-1.0.0.jar              # 10011
java -jar mall-alipay/target/mall-alipay-1.0.0.jar        # 10012
```

启动后内存自动装载种子数据：9 个商品分类、3 家商户、13 个商品、2 个用户、支付渠道 3 个等。

### 2. 前端（统一工程）

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173 （/api → 10010）
```

打开即进入**角色选择门户**，按角色登录后 SPA 内部跳转：

- **管理员** → `/admin/dashboard`：商户审核/资质/状态、订单/退款、渠道配置、数据看板/商户分析、系统管理（管理员/角色/权限）、运营配置（分类/广告）
- **用户** → `/user/home`：浏览 → 详情 → 离线购物车（游客本地缓存，登录后与服务端自动合并）→ 下单支付（模拟收银台自动回调）→ 订单管理 → 退款申请
- **商户** → `/mch/profile` 或 `/mch/apply-status`（按 `shopState` 分流）：入驻申请/资质、店铺信息、商品管理、订单发货、钱包提现、经营统计

## 统一角色入口（门户）

整个前端只有一个入口：打开 `http://localhost:5173/` 即**角色选择门户**。点选「用户 / 商户 / 管理员」在门户内完成对应角色的**登录或注册**（商户注册即提交入驻申请）：

- **管理员** → `/admin/dashboard`
- **用户** → `/user/home`；游客可直接浏览商品 / 加购（离线购物车），登录后自动合并
- **商户** → 已入驻进 `/mch/profile`；待审核 / 已驳回则进 `/mch/apply-status`

同一域内按 `mall_role` 权限路由：未登录访问受限页回门户，跨角色越权访问 → `/403`（跨端口跳转与 URL 传 token 的旧链路已移除）。

## 演示账号

| 端 | 账号 | 密码 | 说明 |
|----|------|------|------|
| 用户端 | user | 123456 | 普通用户（演示用户） |
| 用户端 | demo2 | 123456 | 普通用户 |
| 商家端 | mch_鲜果 | 123456 | 鲜果优选旗舰店（MC0001，已入驻） |
| 商家端 | mch_云客 | 123456 | 云客数码专营店（MC0002） |
| 商家端 | mch_温馨 | 123456 | 温馨服饰店（MC0003） |
| 管理端 | admin | 123456 | 超级管理员（全部权限） |
| 管理端 | operator | 123456 | 运营专员 |

> 登录/注册后即可体验；管理端可在「系统管理-角色」中调整权限。

## 接口规范

- 核心业务接口统一 **POST + JSON**（含登录注册）
- 请求体携带 `timestamp`（秒）、`nonce`（随机串）与业务参数，并计算签名字段 `sign`
- 签名算法：`sign = MD5( 排序后 k=v& 参数串 + secretKey ).toUpperCase()`，复杂值（数组/对象）JSON 序列化
- `secretKey = mall-x-secret-2026`；网关放行前缀的内部回调可使用特殊通道
- 统一响应：`{ "id": "0", "message": "成功", ...业务字段 }`，`id="0"` 成功；401 未登录、403 验签失败、429 限流（"系统繁忙，请稍后再试"）
- 分页响应字段：`total / current / size / records`
- 用 JWT（HMAC-SHA256）鉴权：登录后请求头携带 `Authorization: Bearer <token>`

主要接口：`/mch/register` `/mch/login` `/mch/audit(admin)` `/pay/create` `/pay/callback`(渠道验签) `/pay/local-callback/{payOrderId}`(模拟渠道) `/pay/query` `/refund/create` `/refund/query` `/admin/mch/audit` `/admin/channel/config` `/admin/mch/rate` `/statis/platform` `/statis/mch`（详见解锁后各 controller）。

### 指导书 5.2 / 5.3 新增功能接口

| 模块 | 接口 | 说明 |
| --- | --- | --- |
| 商户·交易流水（5.2.2） | `POST /pay/list/mch` | 本商户支付流水分页/渠道/状态筛选，返回 `fee` 手续费 |
| 商户·流水导出 | 前端 CSV | 交易流水一页内按查询条件导出 BOM+UTF8 CSV |
| 商户·退款记录（5.2.3） | `POST /refund/list/mch` | 本商户退款单（审核状态/原因/金额/平台意见） |
| 商户·手续费明细（5.2.4） | `POST /wallet/records` | 支持 `tradeType` 筛选，返回 `moneyTotal/feeTotal` 汇总（营收−手续费=实收） |
| 商户·统计时间范围（5.2.5） | `POST /statis/mch` | 支持 `begin/end`（yyyy-MM-dd）日期范围筛选 |
| 管理·差异化费率（5.3.2） | `POST /admin/mch/rate/list`、`/admin/mch/rate` | 按渠道配置商户费率（rate=mchRate/基础费率） |
| 管理·异常单处理（5.3.3） | `POST /admin/order/handle` | 支付失败/待支付异常单人工关单并回补库存 |
| 管理·退款监管（5.3.4） | `POST /admin/refund/list`、`/admin/refund/audit` | 大额筛选 `amountMin/amountMax`；返回平台退款率 `refundRate/refundMoney/refundCount/totalMoney` |
| 管理·系统参数（5.3.7） | `POST /admin/system/config/list`、`/admin/system/config/save` | 系统参数 upsert（订单超时/大额阈值/客服电话等） |
| 管理·操作日志（5.3.7） | `POST /admin/system/log/list` | keyword 筛选（操作人/动作/对象） |
| 管理·接口监控（5.3.7） | `POST /admin/monitor/status` | 运行时长/启动时间/JVM 堆/线程/接口调用 Top10 |

## 重点考核点落地

- **Redis+Lua 原子限流与库存扣减**（不可用时内存降级）
- **支付回调幂等**：分布式锁 + 状态机 + 重复回调忽略
- **订单超时自动关闭**：`@Scheduled` 每秒扫单，逾期关闭并回补库存（注释保留 RocketMQ 延迟消息接入点）
- **渠道费率与商户差异化费率**（`/admin/mch/rate`）
- **手续费与钱包提现资金管理** + 提现审核打款（资金流水含手续费汇总）
- **商户经营统计**：渠道占比 / 近 7 日趋势 / 自定义时间范围（`/statis/mch?begin&end`）
- **平台退款监管**：大额退款审核（≥阈值筛选）+ 平台退款率统计（退款金额/交易总额）
- **单据生命周期闭环**：异常（支付失败/待支付超期）订单人工关单并回补库存（`/admin/order/handle`）
- **系统运维三件套**：系统参数配置 / 操作日志检索 / 接口运行状态监控（JVM/线程/调用 Top）
- **RBAC 角色权限**（超管/运营专员 + 权限码树 + 操作日志）

## 目录结构

```
backend/
  mall-common/   共享：Result/SignUtil/状态枚举/MemoryRepository 仓储抽象/RedisTools/JWT/异常
  mall-gateway/  网关 10010：验签/限流/鉴权/路由
  mall-mgr/      业务微服务 10011
  mall-alipay/   支付微服务 10012
frontend/      统一前端单工程（单端口 3000；入口即角色门户，/user /mch /admin 三命名空间）
```

## 已知边界（本阶段）

- 数据存于内存，重启后恢复种子数据（演示完整）；接入 MySQL 即可持久化（仓储已抽象）
- 支付渠道为模拟实现：收银台页面 + 同源异步回调触发支付成功；生产替换为真实渠道回调（验签）
- Redis 缺失时限流/锁/库存降级为进程内实现