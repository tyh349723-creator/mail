import { request } from '@/utils/request'

/**
 * 支付（网关 /pay → 支付微服务）
 */
export function payChannels(): Promise<any> {
  return request({ url: '/pay/channels', method: 'post', data: {} })
}

export function payCreate(d: Record<string, any>): Promise<any> {
  return request({ url: '/pay/create', method: 'post', data: d })
}

export function payQuery(d: Record<string, any>): Promise<any> {
  return request({ url: '/pay/query', method: 'post', data: d })
}

/** 商户支付单列表（交易流水，5.2.2）：wayCode/status/pageNum/pageSize 筛选 */
export function payListMch(d: Record<string, any>): Promise<any> {
  return request({ url: '/pay/list/mch', method: 'post', data: d })
}