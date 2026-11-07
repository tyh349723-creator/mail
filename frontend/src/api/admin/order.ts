import { request } from '@/utils/request'
import type { Order, RefundOrder, PageResult, PageQuery } from '@/types'

/** 分页记录 → 视图 PageResult（后端 records → list） */
function toPage<T>(res: Record<string, any>): PageResult<T> {
  return {
    list: res.records || [],
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10
  }
}

/** 订单列表（仅看板/运营侧；平台订单表单向商户聚合并摊派金额，字段与 Order 对齐） */
export function getOrderList(params: PageQuery & { orderNo?: string; shopName?: string; status?: number }) {
  return request<PageResult<Order>>({
    url: '/admin/order/list',
    method: 'post',
    data: params
  }).then(toPage<Order>)
}

/** 订单详情（后端 Result 内含 order 视图字段） */
export function getOrderDetail(orderNo: string) {
  return request<Record<string, any>>({
    url: '/admin/order/detail',
    method: 'post',
    data: { orderNo }
  }).then((res) => (res?.order || res) as Order)
}

/** 退款申请列表（大额审核：amountMin 筛选；返回含平台退款率汇总） */
export async function getRefundList(params: PageQuery & { orderNo?: string; status?: number; amountMin?: number }) {
  const res = await request<Record<string, any>>({
    url: '/admin/refund/list',
    method: 'post',
    data: params
  })
  return {
    list: (res.records || []) as RefundOrder[],
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10,
    refundCount: res.refundCount || 0,
    refundMoney: res.refundMoney || 0,
    totalMoney: res.totalMoney || 0,
    refundRate: res.refundRate || 0
  }
}

/** 异常订单处理（支付失败/超时未支付人工关单） */
export function orderHandle(data: { orderNo: string; action?: string }) {
  return request({
    url: '/admin/order/handle',
    method: 'post',
    data: { orderNo: data.orderNo, action: data.action || 'close' }
  })
}

/** 退款审批（同意 / 驳回）；后端已批准态固定 id 字段，驳回原因字段为 rejectReason */
export function auditRefund(data: { id: number; approve: boolean; reason?: string }) {
  return request({
    url: '/admin/refund/audit',
    method: 'post',
    data: { id: data.id, approve: data.approve, rejectReason: data.reason || '' }
  })
}