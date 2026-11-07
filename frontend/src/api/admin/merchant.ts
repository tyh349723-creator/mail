import { request } from '@/utils/request'
import type { Merchant, Qualification, PageResult, PageQuery, MerchantRateItem } from '@/types'

/** 分页记录 → 视图 PageResult（后端 records → list） */
function toPage<T>(res: Record<string, any>): PageResult<T> {
  return {
    list: res.records || [],
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10
  }
}

/** 商户入驻申请列表 */
export function getMerchantList(params: PageQuery & { status?: number; keyword?: string }) {
  return request<PageResult<Merchant>>({
    url: '/admin/mch/list',
    method: 'post',
    data: params
  }).then(toPage<Merchant>)
}

/** 商户详情：后端 {merchant, qualifications[]} → 合并为一个对象（视图按旧形状消费） */
export function getMerchantDetail(id: number) {
  return request<Record<string, any>>({
    url: '/admin/mch/detail',
    method: 'post',
    data: { id }
  }).then((res) => ({ ...(res.merchant || {}), qualifications: res.qualifications || [] }))
}

/** 入驻审核（通过/驳回） */
export function auditMerchant(data: { id: number; approve: boolean; rejectReason?: string }) {
  return request({
    url: '/admin/mch/audit',
    method: 'post',
    data
  })
}

/** 资质审查 */
export function auditQualification(data: { id: number; approve: boolean; reason?: string }) {
  return request({
    url: '/admin/mch/qualification/audit',
    method: 'post',
    data
  })
}

/** 店铺状态管理（启用 1 / 禁用 3） */
export function updateShopStatus(data: { id: number; status: number }) {
  return request({
    url: '/admin/mch/status',
    method: 'post',
    data
  })
}

/** 商户资质文件列表 */
export function getQualifications(merchantId: number) {
  return request<Record<string, any>>({
    url: '/admin/mch/detail',
    method: 'post',
    data: { id: merchantId }
  }).then((res) => (res.qualifications || []) as Qualification[])
}

/** 商户差异化费率列表（3 渠道：基础费率 + 当前费率） */
export async function getMerchantRateList(merchantNo: string) {
  const res = await request<Record<string, any>>({
    url: '/admin/mch/rate/list',
    method: 'post',
    data: { merchantNo }
  })
  return (res.list || []) as MerchantRateItem[]
}

/** 保存商户某渠道差异化费率 */
export function updateMerchantRate(data: { shopNum: string; wayCode: string; mchRate: string | number }) {
  return request({
    url: '/admin/mch/rate',
    method: 'post',
    data: { shopNum: data.shopNum, wayCode: data.wayCode, mchRate: data.mchRate }
  })
}