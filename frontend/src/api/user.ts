import { request } from '@/utils/request'

/**
 * 用户端：个人资料 / 收货地址
 */
export function updateProfile(d: Record<string, any>): Promise<any> {
  return request({ url: '/user/profile/update', method: 'post', data: d })
}

export function addressList(): Promise<any> {
  return request({ url: '/user/address/list', method: 'post', data: {} })
}

export function addressCreate(d: Record<string, any>): Promise<any> {
  return request({ url: '/user/address/create', method: 'post', data: d })
}

export function addressUpdate(d: Record<string, any>): Promise<any> {
  return request({ url: '/user/address/update', method: 'post', data: d })
}

export function addressDelete(d: Record<string, any>): Promise<any> {
  return request({ url: '/user/address/delete', method: 'post', data: d })
}