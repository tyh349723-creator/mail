import { request } from '@/utils/request'

/**
 * 购物车（离线本地缓存 + 服务端）
 */
export function cartList(): Promise<any> {
  return request({ url: '/cart/list', method: 'post', data: {} })
}

export function cartAdd(d: Record<string, any>): Promise<any> {
  return request({ url: '/cart/add', method: 'post', data: d })
}

export function cartUpdate(d: Record<string, any>): Promise<any> {
  return request({ url: '/cart/update', method: 'post', data: d })
}

export function cartRemove(d: Record<string, any>): Promise<any> {
  return request({ url: '/cart/remove', method: 'post', data: d })
}

export function cartClear(): Promise<any> {
  return request({ url: '/cart/clear', method: 'post', data: {} })
}

export function cartMerge(items: Array<{ skuId: string; quantity: number }>): Promise<any> {
  return request({ url: '/cart/merge', method: 'post', data: { items } })
}

export function cartCount(): Promise<any> {
  return request({ url: '/cart/count', method: 'post', data: {} })
}