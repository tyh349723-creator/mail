import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AdminLayout from '@/layout/admin/index.vue'

/**
 * 静态路由（无需权限/公共）
 * 入口即门户：/ （角色选择 · 登录/注册），登录后按角色进入 /user | /mch | /admin 命名空间。
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    // 统一角色门户（用户/商户/管理员）——无需登录即可访问
    path: '/',
    name: 'Portal',
    component: () => import('@/views/portal/index.vue'),
    meta: { title: '角色选择 · 登录 / 注册', hidden: true }
  },
  // 旧入口兼容：老链接/书签统一回到门户
  { path: '/login', redirect: '/' },
  { path: '/portal', redirect: '/' },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/admin/error/403.vue'),
    meta: { title: '403', hidden: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/admin/error/404.vue'),
    meta: { title: '404', hidden: true }
  },
  // 历史路由重定向：旧版 pc-admin 路径 → 新菜单落点（不占侧边栏）
  { path: '/admin/merchant/status', redirect: '/admin/merchant/list', meta: { hidden: true } },
  { path: '/admin/order/list', redirect: '/admin/order', meta: { hidden: true } },
  { path: '/admin/order/refund', redirect: '/admin/refund', meta: { hidden: true } },
  { path: '/admin/system/user', redirect: '/admin/perm/user', meta: { hidden: true } },
  { path: '/admin/system/role', redirect: '/admin/perm/role', meta: { hidden: true } },
  { path: '/admin/system/permission', redirect: '/admin/perm/permission', meta: { hidden: true } },
  { path: '/admin/system/settings', redirect: '/admin/ops/settings', meta: { hidden: true } },
  { path: '/admin/system/log', redirect: '/admin/ops/log', meta: { hidden: true } },
  { path: '/admin/system/monitor', redirect: '/admin/ops/monitor', meta: { hidden: true } },

  // ---------- 移动端 · 用户端 ----------
  {
    path: '/user',
    redirect: '/user/home',
    children: [
      { path: 'home', name: 'UserHome', component: () => import('@/views/user/Index.vue'), meta: { title: '首页' } },
      { path: 'search', name: 'UserSearch', component: () => import('@/views/user/Search.vue'), meta: { title: '搜索' } },
      { path: 'product/:id', name: 'UserProductDetail', component: () => import('@/views/user/ProductDetail.vue'), meta: { title: '商品详情' } },
      { path: 'cart', name: 'UserCart', component: () => import('@/views/user/Cart.vue'), meta: { title: '购物车' } },
      { path: 'checkout', name: 'UserCheckout', component: () => import('@/views/user/Checkout.vue'), meta: { title: '确认订单' } },
      { path: 'pay/:orderNo', name: 'UserPay', component: () => import('@/views/user/Pay.vue'), meta: { title: '收银台' } },
      { path: 'orders', name: 'UserOrders', component: () => import('@/views/user/Orders.vue'), meta: { title: '我的订单' } },
      { path: 'order-detail/:orderNo', name: 'UserOrderDetail', component: () => import('@/views/user/OrderDetail.vue'), meta: { title: '订单详情' } },
      { path: 'refund-apply/:orderNo', name: 'UserRefundApply', component: () => import('@/views/user/RefundApply.vue'), meta: { title: '申请退款' } },
      { path: 'refund/:orderNo', name: 'UserRefundDetail', component: () => import('@/views/user/RefundDetail.vue'), meta: { title: '退款详情' } },
      { path: 'profile', name: 'UserProfile', component: () => import('@/views/user/Profile.vue'), meta: { title: '我的' } },
      { path: 'address', name: 'UserAddress', component: () => import('@/views/user/Address.vue'), meta: { title: '收货地址' } },
      { path: 'address-edit/:id?', name: 'UserAddressEdit', component: () => import('@/views/user/AddressEdit.vue'), meta: { title: '编辑地址' } }
    ]
  },

  // ---------- 商家端（左侧栏控制台，内容区保持手机宽度预览） ----------
  {
    path: '/mch',
    component: () => import('@/layout/mch/index.vue'),
    redirect: '/mch/profile',
    children: [
      { path: 'apply', name: 'MchApply', component: () => import('@/views/mch/MchApply.vue'), meta: { title: '入驻申请' } },
      { path: 'apply-status', name: 'MchApplyStatus', component: () => import('@/views/mch/MchApplyStatus.vue'), meta: { title: '入驻状态' } },
      { path: 'shop', name: 'MchShop', component: () => import('@/views/mch/MchShop.vue'), meta: { title: '店铺信息' } },
      { path: 'products', name: 'MchProducts', component: () => import('@/views/mch/MchProducts.vue'), meta: { title: '商品管理' } },
      { path: 'product-edit/:id?', name: 'MchProductEdit', component: () => import('@/views/mch/MchProductEdit.vue'), meta: { title: '编辑商品' } },
      { path: 'orders', name: 'MchOrders', component: () => import('@/views/mch/MchOrders.vue'), meta: { title: '订单管理' } },
      { path: 'wallet', name: 'MchWallet', component: () => import('@/views/mch/MchWallet.vue'), meta: { title: '钱包提现' } },
      { path: 'stats', name: 'MchStats', component: () => import('@/views/mch/MchStats.vue'), meta: { title: '经营统计' } },
      { path: 'profile', name: 'MchProfile', component: () => import('@/views/mch/MchProfile.vue'), meta: { title: '商家工作台' } },
      { path: 'mine', name: 'MchMine', component: () => import('@/views/mch/MchMine.vue'), meta: { title: '我的' } }
    ]
  },

  // ---------- 平台管理端（常驻：数据看板） ----------
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis', affix: true }
      }
    ]
  }
]

/**
 * 动态路由（管理员按权限挂载，路径统一挂在 /admin 下）
 * 结构对应左侧 7 大功能模块：
 *   商户管控 → 订单查询 → 退款监管 → 支付渠道 → 平台统计 → 权限管理 → 系统运维 → 运营管理
 * 单页菜单（订单查询 / 退款监管）：父级承载 title/icon/permission，子路由 path 为空串挂载页面组件。
 */
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/merchant',
    name: 'MerchantRoutes',
    component: AdminLayout,
    redirect: '/admin/merchant/list',
    meta: { title: '商户管控', icon: 'Shop', permission: 'merchant:list' },
    children: [
      { path: 'list', name: 'MerchantList', component: () => import('@/views/admin/merchant/status.vue'), meta: { title: '商户列表', icon: 'Shop', permission: 'merchant:status' } },
      { path: 'review', name: 'MerchantReview', component: () => import('@/views/admin/merchant/review.vue'), meta: { title: '入驻审核', icon: 'Checked', permission: 'merchant:review' } },
      { path: 'qualification', name: 'MerchantQualification', component: () => import('@/views/admin/merchant/qualification.vue'), meta: { title: '资质审查', icon: 'DocumentChecked', permission: 'merchant:qualification' } }
    ]
  },
  {
    // 单页菜单：订单查询（多条件筛选 + 异常订单人工关单）
    path: '/admin/order',
    name: 'OrderQueryRoutes',
    component: AdminLayout,
    meta: { title: '订单查询', icon: 'Tickets', permission: 'order:list' },
    children: [{ path: '', component: () => import('@/views/admin/order/list.vue') }]
  },
  {
    // 单页菜单：退款监管（全平台退款记录 + 大额审核 + 退款率统计）
    path: '/admin/refund',
    name: 'RefundMonitorRoutes',
    component: AdminLayout,
    meta: { title: '退款监管', icon: 'RefreshLeft', permission: 'order:refund' },
    children: [{ path: '', component: () => import('@/views/admin/order/refund.vue') }]
  },
  {
    path: '/admin/payment',
    name: 'PaymentRoutes',
    component: AdminLayout,
    redirect: '/admin/payment/channel',
    meta: { title: '支付渠道', icon: 'Wallet', permission: 'payment:list' },
    children: [
      { path: 'channel', name: 'PayChannel', component: () => import('@/views/admin/payment/channel.vue'), meta: { title: '渠道管理', icon: 'CreditCard', permission: 'payment:list' } },
      { path: 'config', name: 'PayConfig', component: () => import('@/views/admin/payment/config.vue'), meta: { title: '渠道密钥', icon: 'Setting', permission: 'payment:config' } }
    ]
  },
  {
    path: '/admin/report',
    name: 'ReportRoutes',
    component: AdminLayout,
    redirect: '/admin/report/trade',
    meta: { title: '平台统计', icon: 'TrendCharts', permission: 'report:view' },
    children: [
      { path: 'trade', name: 'TradeReport', component: () => import('@/views/admin/report/trade.vue'), meta: { title: '交易数据可视化', icon: 'DataLine', permission: 'report:trade' } },
      { path: 'merchant', name: 'MerchantReport', component: () => import('@/views/admin/report/merchant.vue'), meta: { title: '商户经营分析', icon: 'Histogram', permission: 'report:merchant' } }
    ]
  },
  {
    // 单页菜单：用户管理（用户端账号检索、详情、启停、重置密码）
    path: '/admin/user',
    name: 'UserManageRoutes',
    component: AdminLayout,
    meta: { title: '用户管理', icon: 'User', permission: 'user:list' },
    children: [{ path: '', component: () => import('@/views/admin/user/index.vue') }]
  },
  {
    // 系统权限管理：管理员账号 / 角色权限分配 / 操作用户管理
    path: '/admin/perm',
    name: 'PermissionRoutes',
    component: AdminLayout,
    redirect: '/admin/perm/user',
    meta: { title: '权限管理', icon: 'UserFilled', permission: 'system:user' },
    children: [
      { path: 'user', name: 'SystemUser', component: () => import('@/views/admin/system/user.vue'), meta: { title: '管理员管理', icon: 'User', permission: 'system:user' } },
      { path: 'role', name: 'SystemRole', component: () => import('@/views/admin/system/role.vue'), meta: { title: '角色管理', icon: 'Avatar', permission: 'system:role' } },
      { path: 'permission', name: 'SystemPermission', component: () => import('@/views/admin/system/permission.vue'), meta: { title: '权限管理', icon: 'Key', permission: 'system:permission' } }
    ]
  },
  {
    // 系统运维：系统参数 / 操作日志 / 接口监控
    path: '/admin/ops',
    name: 'OpsRoutes',
    component: AdminLayout,
    redirect: '/admin/ops/settings',
    meta: { title: '系统运维', icon: 'Setting', permission: 'system:config' },
    children: [
      { path: 'settings', name: 'SystemSettings', component: () => import('@/views/admin/system/settings.vue'), meta: { title: '系统参数', icon: 'Tools', permission: 'system:config' } },
      { path: 'log', name: 'SystemLog', component: () => import('@/views/admin/system/log.vue'), meta: { title: '操作日志', icon: 'Documentation', permission: 'system:log' } },
      { path: 'monitor', name: 'SystemMonitor', component: () => import('@/views/admin/system/monitor.vue'), meta: { title: '接口监控', icon: 'Monitor', permission: 'system:monitor' } }
    ]
  },
  {
    path: '/admin/operation',
    name: 'OperationRoutes',
    component: AdminLayout,
    redirect: '/admin/operation/category',
    meta: { title: '运营管理', icon: 'Operation', permission: 'category:list' },
    children: [
      { path: 'category', name: 'CategoryManage', component: () => import('@/views/admin/category/index.vue'), meta: { title: '商品分类维护', icon: 'Menu', permission: 'category:list' } },
      { path: 'advert', name: 'AdvertManage', component: () => import('@/views/admin/advert/index.vue'), meta: { title: '广告位配置', icon: 'Picture', permission: 'advert:list' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

export default router