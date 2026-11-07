import { request } from '@/utils/request'
import type { PageResult, MallUserAdmin, MallUserDetail } from '@/types'

function normalizeUser(raw: Record<string, any>): MallUserAdmin {
  return {
    ...raw,
    status: Number(raw.status) === 0 ? 0 : 1,
    orderCount: Number(raw.orderCount || 0),
    paidCount: Number(raw.paidCount || 0),
    totalSpent: Number(raw.totalSpent || 0)
  } as MallUserAdmin
}

/** 用户列表（实时查询 mall_user，附带订单统计） */
export async function getUserList(params: {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number
}) {
  const res = await request<Record<string, any>>({
    url: '/admin/user/list',
    method: 'post',
    data: params
  })
  return {
    list: (res.records || []).map(normalizeUser),
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10
  } as PageResult<MallUserAdmin>
}

/** 用户详情（基本信息 + 收货地址 + 最近订单） */
export async function getUserDetail(data: { id: number }) {
  const res = await request<Record<string, any>>({
    url: '/admin/user/detail',
    method: 'post',
    data
  })
  return {
    user: normalizeUser(res.user || {}),
    addresses: res.addresses || [],
    recentOrders: res.recentOrders || []
  } as MallUserDetail
}

/** 启用 / 禁用用户 */
export function updateUserStatus(data: { id: number; status: 0 | 1 }) {
  return request({
    url: '/admin/user/status',
    method: 'post',
    data
  })
}

/** 重置用户密码 */
export function resetUserPassword(data: { id: number; password: string }) {
  return request({
    url: '/admin/user/reset-password',
    method: 'post',
    data
  })
}
