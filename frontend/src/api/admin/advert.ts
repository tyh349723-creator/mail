import { request } from '@/utils/request'
import type { Advert, AdvertSlot, PageResult, PageQuery } from '@/types'

/** 广告位列表 */
export async function getAdvertSlots() {
  const res = await request<Record<string, any>>({
    url: '/admin/advert/slots',
    method: 'post',
    data: {}
  })
  return (res.list || []) as AdvertSlot[]
}

/** 新增广告位 */
export function addAdvertSlot(data: Partial<AdvertSlot>) {
  return request({
    url: '/admin/advert/slot/save',
    method: 'post',
    data
  })
}

/** 广告列表 */
export function getAdvertList(params: PageQuery & { slotId?: number; status?: number }) {
  return request<Record<string, any>>({
    url: '/admin/advert/list',
    method: 'post',
    data: params
  }).then((res) => ({
    list: res.records || [],
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10
  }) as PageResult<Advert>)
}

/** 新增广告 */
export function addAdvert(data: Partial<Advert>) {
  return request({
    url: '/admin/advert/save',
    method: 'post',
    data
  })
}

/** 编辑广告 */
export function updateAdvert(data: Partial<Advert>) {
  return request({
    url: '/admin/advert/save',
    method: 'post',
    data
  })
}

/** 删除广告 */
export function deleteAdvert(id: number) {
  return request({
    url: '/admin/advert/delete',
    method: 'post',
    data: { id }
  })
}

/** 上下线广告 */
export function updateAdvertStatus(data: { id: number; status: 0 | 1 }) {
  return request({
    url: '/admin/advert/status',
    method: 'post',
    data
  })
}