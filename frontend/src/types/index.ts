// 通用类型定义

/** 分页请求参数 */
export interface PageQuery {
  pageNum: number
  pageSize: number
  [key: string]: any
}

/** 分页响应 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 统一响应结构 */
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/** 登录响应 */
export interface LoginResult {
  token: string
  refreshToken?: string
  expiresIn?: number
  userInfo: AdminUserInfo
}

/** 管理员用户信息 */
export interface AdminUserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

/** 角色 */
export interface Role {
  id: number
  roleName: string
  roleCode: string
  description?: string
  status: 0 | 1
  createTime?: string
}

/** 权限 */
export interface Permission {
  id: number
  permName: string
  permCode: string
  type: 'menu' | 'button' | 'api'
  parentId: number
  path?: string
  icon?: string
  sort?: number
  children?: Permission[]
}

/** 商户申请/入驻 */
export interface Merchant {
  id: number
  merchantNo: string
  shopName: string
  contactName: string
  contactPhone?: string
  shopPhone?: string
  shopAddress?: string
  categoryId: number
  categoryName: string
  status: number // 0待审核 1已通过 2已驳回 3已关闭
  rejectReason?: string
  qualifications: Qualification[]
  createTime: string
  auditTime?: string
  auditor?: string
}

/** 资质文件 */
export interface Qualification {
  id: number
  type: string // business_license 营业执照 / id_card 法人身份证 / bank 银行账户
  name: string
  url: string
  auditStatus?: number
}

/** 商品分类 */
export interface Category {
  id: number
  name: string
  parentId: number
  level: number
  sort: number
  icon?: string
  status: 0 | 1
  children?: Category[]
}

/** 广告位 */
export interface AdvertSlot {
  id: number
  slotName: string
  slotCode: string
  position: string // 首页轮播/分类页/详情页
  status: 0 | 1
}

/** 广告 */
export interface Advert {
  id: number
  slotId: number
  slotName: string
  title: string
  imageUrl: string
  linkUrl: string
  sort: number
  startTime: string
  endTime: string
  status: 0 | 1
}

/** 订单 */
export interface Order {
  id: number
  orderNo: string
  merchantId: number
  shopName: string
  buyerName: string
  totalAmount: number
  payAmount: number
  status: number // 0待支付 1已支付 2待发货 3已发货 4已完成 5已取消 6退款中 7已退款
  statusText?: string
  payChannel?: string
  createTime: string
  payTime?: string
}

/** 平台退款单（admin/refund/list，字段与后端 Refund.view / MchRefund 对齐：0 待处理 1 已处理 2 退款失败 3 已撤销） */
export interface RefundOrder {
  id: number
  refundOrderId: string
  orderNo: string
  merchantNo: string
  money: number
  reason: string
  status: number
  statusText: string
  auditRemark: string
  createTime: string
  auditTime: string
}

/** 支付渠道 */
export interface PayChannel {
  id: number
  channelCode: string
  channelName: string
  channelType: string // alipay / wechat / unionpay / bank
  status: 0 | 1
  icon?: string
  configParams?: Record<string, string>
  createTime?: string
}

/** 统计指标 */
export interface StatMetric {
  label: string
  value: number
  unit?: string
  trend: number
}

/** 图表数据点 */
export interface ChartPoint {
  name: string
  value: number
}

/** 商户差异化费率项（3 渠道 × 基础费率/当前费率） */
export interface MerchantRateItem {
  wayCode: string
  channelName: string
  baseRate: number
  mchRate: number
}

/** 商户交易流水（pay/list/mch） */
export interface PayTrade {
  id: number
  payOrderId: string
  orderNo: string
  merchantNo: string
  wayCode: string
  channelName: string
  money: number
  fee: number
  status: number
  statusText: string
  payTime: string
  createTime: string
}

/** 商户退款记录（refund/list/mch，字段与后端 Refund.view 对齐） */
export interface MchRefund {
  id: number
  refundOrderId: string
  orderNo: string
  merchantNo: string
  money: number
  reason: string
  status: number // 0 待处理 1 已处理 2 退款失败 3 已撤销
  statusText: string
  auditRemark: string
  createTime: string
  auditTime: string
}

/** 系统参数 */
export interface SysConfig {
  id: number
  configKey: string
  configValue: string
  remark: string
}

/** 操作日志 */
export interface OperLog {
  id: number
  adminName: string
  action: string
  target: string
  detail: string
  ip: string
  createTime: string
}

/** 接口运行状态监控 */
export interface MonitorStatus {
  uptimeSeconds: number
  startTime: string
  threads: number
  totalCalls: number
  jvm: { heapUsedMb: number; heapTotalMb: number; heapMaxMb: number }
  top: Array<{ path: string; count: number }>
}

/** 用户端账号（后台用户管理） */
export interface MallUserAdmin {
  id: number
  userNum: string
  username: string
  nickname: string
  avatar: string
  phone: string
  status: 0 | 1
  createTime: string
  orderCount: number
  paidCount: number
  totalSpent: number
  lastOrderTime: string
}

/** 用户收货地址（后台用户详情） */
export interface MallUserAddress {
  id: number
  consignee: string
  phone: string
  full: string
  defaultFlag: boolean
}

/** 用户最近订单（后台用户详情） */
export interface UserRecentOrder {
  id: number
  orderNo: string
  shopName: string
  payAmount: number
  status: number
  statusText: string
  createTime: string
}

/** 用户详情 */
export interface MallUserDetail {
  user: MallUserAdmin
  addresses: MallUserAddress[]
  recentOrders: UserRecentOrder[]
}
