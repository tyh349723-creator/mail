import { request } from '@/utils/request'

/**
 * 商家端：入驻、店铺、商品、订单
 */
export function mchApply(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/apply', method: 'post', data: d })
}

export function mchApplyStatus(): Promise<any> {
  return request({ url: '/mch/apply/status', method: 'post', data: {} })
}

export function mchShopInfo(): Promise<any> {
  return request({ url: '/mch/shop/info', method: 'post', data: {} })
}

export function mchShopUpdate(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/shop/update', method: 'post', data: d })
}

export function mchProductList(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/product/list', method: 'post', data: d })
}

export function mchProductCreate(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/product/create', method: 'post', data: d })
}

export function mchProductUpdate(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/product/update', method: 'post', data: d })
}

export function mchProductStatus(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/product/status', method: 'post', data: d })
}

/** 库存设置：直接设置某个 SKU 的库存量（实时落库） */
export function mchProductStock(d: { skuId: number | string; stock: number }): Promise<any> {
  return request({ url: '/mch/product/stock', method: 'post', data: d })
}

export function mchProductDelete(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/product/delete', method: 'post', data: d })
}

export function mchOrderList(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/order/list', method: 'post', data: d })
}

export function mchOrderDetail(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/order/detail', method: 'post', data: d })
}

export function mchOrderShip(d: Record<string, any>): Promise<any> {
  return request({ url: '/mch/order/ship', method: 'post', data: d })
}

/** 商户退款记录（5.2.3）：订单号/状态/分页 查询本店退款单与处理进度 */
export function mchRefundList(d: Record<string, any>): Promise<any> {
  return request({ url: '/refund/list/mch', method: 'post', data: d })
}

/** 商户处理退款（5.2.3）：同意退款 / 驳回，处理前须为待处理 */
export function mchRefundHandle(d: { id: number; approve: boolean; reason?: string }): Promise<any> {
  return request({ url: '/refund/handle/mch', method: 'post', data: d })
}