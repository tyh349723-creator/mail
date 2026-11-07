import { request } from '@/utils/request'

/**
 * 商家资金（钱包余额、流水、提现）
 */
export function walletBalance(): Promise<any> {
  return request({ url: '/wallet/balance', method: 'post', data: {} })
}

export function walletRecords(d: Record<string, any>): Promise<any> {
  return request({ url: '/wallet/records', method: 'post', data: d })
}

export function walletWithdrawApply(d: Record<string, any>): Promise<any> {
  return request({ url: '/wallet/withdraw/apply', method: 'post', data: d })
}

export function walletWithdrawList(d: Record<string, any>): Promise<any> {
  return request({ url: '/wallet/withdraw/list', method: 'post', data: d })
}

/** 商户经营统计（5.2.5）：可选 begin/end（yyyy-MM-dd）自定义时间范围，不传为全部 */
export function statisMch(d: Record<string, any> = {}): Promise<any> {
  return request({ url: '/statis/mch', method: 'post', data: d })
}