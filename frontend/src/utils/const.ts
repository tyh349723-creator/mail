/**
 * 通用常量：业务状态枚举 → 中文文案、金额格式化、localStorage 键名。
 * 状态数字与后端 StatusConst / README 契约保持一致。
 */

/** 订单状态 */
export const ORDER_STATUS: Record<string, string> = {
  '-1': '待支付',
  '0': '待发货',
  '1': '支付失败',
  '2': '已退款',
  '3': '已关闭',
  '4': '待收货',
  '5': '已完成'
}

/** 支付状态 */
export const PAY_STATUS: Record<string, string> = {
  '-1': '待支付',
  '0': '支付成功',
  '1': '支付失败',
  '2': '已退款',
  '3': '已关闭'
}

/** 退款状态 */
export const REFUND_STATUS: Record<string, string> = { '0': '待处理', '1': '已处理', '2': '退款失败', '3': '已撤销' }

/** 商户入驻/店铺状态 */
export const MCH_STATUS: Record<string, string> = { '0': '待审核', '1': '正常', '2': '已驳回', '3': '已禁用' }

/** 提现状态 */
export const WITHDRAW_STATUS: Record<string, string> = { '0': '待审核', '1': '已打款', '2': '已驳回' }

/** 支付渠道中文名 */
export const CHANNEL_TEXT: Record<string, string> = { WECHAT: '微信支付', ALIPAY: '支付宝', UNIONPAY: '银联支付' }

/** 金额格式化（分/元均可，保留两位小数） */
export function fmtMoney(n: number | string | null | undefined): string {
  return Number(n ?? 0).toFixed(2)
}

/** 购物车离线本地缓存键：游客加购存 localStorage，登录后合并进服务端 */
export const LOCAL_CART_KEY = 'mall_cart:local'

/** 移动端模式键：user | merchant（新体系以 mall_role 为准，此键兼容保留） */
export const MODE_KEY = 'mall_mode'

/** 角色键：user | mch | admin */
export const ROLE_KEY = 'mall_role'