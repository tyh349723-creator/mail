# Mall-X 统一前端（frontend/）

多商家电商平台 **前端单工程、单端口**：

- 唯一入口即**角色选择门户**（`/`，选「用户 / 商户 / 管理员」登录或注册），登录后在同一 SPA 内按角色跳转；
- 一个工程同时承载：移动端用户商城（H5）、移动端商家工作台（H5）、平台管理后台（PC）；
- 无跨端口跳转、无 URL 传 token（旧 `mall-vue` + `pc-admin` + 门户三端互跳架构已合并移除）。

## 快速开始

```bash
npm install
npm run dev     # http://localhost:5173（vite 代理 /api → http://localhost:10010）
npm run build   # 产物构建（hint：合理分包 manualChunks：element-plus / echarts / vue-vendor）
npm run lint    # vue-tsc --noEmit 类型检查（信息性，不阻塞 build）
```

> 端口占用时 vite 自动顺延（5174…）；如需固定端口：`npm run dev -- --port 5173 --strictPort`。

## 目录结构（毕设规范分层）

```
src/
  api/          # 业务接口按域拆分，@/api 统一出口（barrel）
    admin/      # 平台管理端（/admin/**）：merchant order payment report system category advert auth.ts 除外
    user.ts product.ts cart.ts order.ts pay.ts mch.ts wallet.ts portal.ts
  assets/       # 静态资源
  components/   # 通用组件（非页面级）
    mobile/     # 移动端 H5：Tabbar / ProductCard
    common/     # 管理端：Pagination / EChart
  layout/
    admin/      # 管理后台布局：Sidebar / Navbar / TagsView / index
  router/
    index.ts      # constantRoutes + asyncRoutes（admin 权限树，filterAsyncRoutes 挂载）
    permission.ts # 三角色守卫：未登录 → 门户；越权 → /403；已登录访问 / → 回跳 roleHome
  store/
    user.ts       # 统一登录态（user | mch | admin 三角色）：token/role/profile/mchState/adminInfo
    permission.ts # 管理端 roles/permissions（超管放行）
    cart.ts       # 购物车：游客本地(localStorage) + 登录后服务端，登录时自动合并
  styles/
    base.css      # 移动端 H5 基础（:root 变量 / .page / .tabbar / .navbar / .empty / .card）
    index.scss    # 管理端全局（布局变量 / .app-container / .table-card / 分页 / 统计卡片）
  utils/
    request.ts    # axios 封装：POST body 秒级签名、Bearer token、id="0" 判定、401 → 门户
    auth.ts       # 登录态存取 mall_token + mall_role + mall_admin_user（含旧 key 迁移）
    sign.ts       # sign = MD5(k=v& 参数串 + secretKey).toUpperCase()
    const.ts      # 状态枚举（订单/支付/退款/商户/提现）、fmtMoney、LOCAL_CART_KEY 等
    format.ts
  types/
    index.ts      # 通用类型：PageQuery / PageResult / AdminUserInfo / Permission / Order …
  views/
    portal/       # 角色选择门户（根路由 /）
    user/         # 用户端 H5（/user/**）
    mch/          # 商家端 H5（/mch/**）
    admin/        # 平台管理后台（/admin/**）
```

## 路由与权限规范

| 命名空间 | 角色 | 说明 |
|----------|------|------|
| `/` | 游客 | 角色选择门户（登录/注册） |
| `/user/**` | user | 移动端商城：home category search product/:id cart checkout pay/:orderNo orders order-detail/:orderNo profile address address-edit/:id? |
| `/mch/**` | mch | 商家端：apply apply-status shop products product-edit/:id? orders wallet stats profile |
| `/admin/**` | admin | 管理后台：dashboard 常驻 + merchant operation order payment report system 按权限动态挂载 |
| `/403` `/404` | - | 越权 / 未匹配 |

- 登录态：`mall_token`（JWT）+ `mall_role`（user|mch|admin）。
- 角色守卫（`router/permission.ts`）：
  - 未登录仅放行 `/` 及**游客可浏览页**（首页/分类/搜索/商品详情/购物车，保持「离线浏览 + 离线购物车 → 登录合并」演示）；
  - 已登录访问 `/` 回跳本角色首页（用户 `/user/home`、商户按 `mchState` 分流、管理员 `/admin/dashboard`）；
  - 跨角色越权 → `/403`；admin 首次进入时拉 `/admin/auth/info` 动态挂载权限路由。
- 页面间跳转统一 `router.push(...)` 绝对路径（含角色前缀），禁止 `location.hash` 路由。

## API 规范

- 统一 `request<T>({ url, method:'post', data })`；拦截器自动补 `timestamp/nonce/sign` 并附加 `Authorization: Bearer`。
- 统一响应 `{id, message, ...}`：`id==="0"` 成功；`"401"` 登录失效 → 弹窗后清登录态回门户；其余 `message` 以 ElMessage 提示。
- 分页字段 `total/current/size/records`（管理端 api 适配为 `{list,total,pageNum,pageSize}`；用户端订单列表按后端原始 `records` 消费）。
- 管理端接口统一经 `src/api/admin/` 引用（如 `import { getOrderList } from '@/api/admin/order'`），业务端经 `@/api` barrel 引用。

## 状态管理（Pinia）

- `store/user`（三角色合一）：`login / loginUser / loginMch`、`roleHome(role, mchState)`、`fetchUserInfo`、`logout`、`setMerchantInfo` …；兼容旧移动端视图的方法名（`setLogin/setMode`）保留。
- `store/permission`：admin 权限/角色；`hasPermission` 对 `SUPER_ADMIN` 放行。
- `store/cart`：`local`（游客离线）+ `server`（登录后）+ `syncAfterLogin` 合并 + `count` 角标。

## 样式规范

- 移动端 H5：`styles/base.css`（`:root` CSS 变量、`.page` 480px 居中限宽）。
- 管理端：`styles/index.scss`（`.app-container .search-bar` 等全局类，页面模板内相对布局）。
- **类名避让约定**：同名类若跨端语义不同，管理端类名加作用域前缀（如后台顶部栏 `.admin-navbar`、搜索栏 `.app-container .search-bar`），避免全局样式污染 H5。

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 用户 | user / demo2 | 123456 |
| 商户 | mch_鲜果 / mch_云客 / mch_温馨 | 123456 |
| 管理员 | admin（超管）/ operator | 123456 |