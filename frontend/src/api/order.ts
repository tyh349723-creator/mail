import { request } from '@/utils/request'

/**
 * 订单（用户端：预下单、创建、列表、详情、取消、确认收货、退款）
 * 注：列表返回后端原始分页载荷 {records, total, current, size}，视图按 records 消费。
 */
export function orderPreview(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/preview', method: 'post', data: d })
}

export function orderCreate(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/create', method: 'post', data: d })
}

export function orderUserList(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/user/list', method: 'post', data: d })
}

export function orderUserDetail(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/user/detail', method: 'post', data: d })
}

export function orderCancel(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/cancel', method: 'post', data: d })
}

export function orderConfirm(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/confirm', method: 'post', data: d })
}

export function orderRefundApply(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/refund/apply', method: 'post', data: d })
}

export function orderRefundList(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/refund/list', method: 'post', data: d })
}

export function orderRefundCancel(d: Record<string, any>): Promise<any> {
  return request({ url: '/order/refund/cancel', method: 'post', data: d })
}