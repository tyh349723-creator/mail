import { request } from '@/utils/request'
import type { StatMetric, ChartPoint } from '@/types'

/** 渠道编码 → 中文名 */
const CHANNEL_NAME: Record<string, string> = {
  WECHAT: '微信支付',
  WX: '微信支付',
  ALIPAY: '支付宝',
  UNIONPAY: '银联支付'
}

/** 日期范围 → 后端 begin/end 参数（未选日期时返回空对象） */
function rangeBody(dateRange?: string[]) {
  return dateRange && dateRange.length === 2 && dateRange[0] && dateRange[1]
    ? { begin: dateRange[0], end: dateRange[1] }
    : {}
}

/** 拉取平台统计（聚合 alipay：总额/笔数/渠道占比/趋势/商户数/今日订单/用户数） */
async function platform(params?: { dateRange?: string[]; type?: 'day' | 'week' | 'month' }) {
  return request<Record<string, any>>({
    url: '/admin/statis/platform',
    method: 'post',
    data: { ...rangeBody(params?.dateRange), type: params?.type }
  })
}

/** 看板核心指标：平台统计映射为指标卡（无历史对比数据，trend 置 0） */
export async function getDashboardMetrics(params?: { dateRange?: string[] }) {
  const res = await platform({ dateRange: params?.dateRange })
  return [
    { label: '交易总额', value: Number(res.totalMoney) || 0, trend: 0 },
    { label: '订单总量', value: Number(res.totalCount) || 0, trend: 0 },
    { label: '入驻商户数', value: Number(res.merchantCount) || 0, trend: 0 },
    { label: '今日订单', value: Number(res.orderToday) || 0, trend: 0 }
  ] as StatMetric[]
}

/** 交易趋势（折线图）：按区间聚合，未选日期时为近七日交易额 */
export async function getTradeTrend(params?: { type?: 'day' | 'week' | 'month'; dateRange?: string[] }) {
  const res = await platform({ dateRange: params?.dateRange, type: params?.type })
  return (res.sevenDayData || []).map((d: Record<string, any>) => ({
    name: d.date,
    value: Number(d.money) || 0
  })) as ChartPoint[]
}

/** 渠道销售分布（饼图）：后端提供渠道占比，按金额出图 */
export async function getCategorySalesPie(params?: { dateRange?: string[] }) {
  const res = await platform({ dateRange: params?.dateRange })
  return (res.channelRatio || []).map((c: Record<string, any>) => ({
    name: CHANNEL_NAME[c.wayCode] || c.wayCode,
    value: Number(c.money) || 0
  })) as ChartPoint[]
}

/** 商户经营排行（柱状图）：聚合各商户交易额 */
export async function getMerchantRanking(params?: { limit?: number; dateRange?: string[] }) {
  const res = await request<Record<string, any>>({
    url: '/admin/statis/merchants',
    method: 'post',
    data: { limit: params?.limit || 10, ...rangeBody(params?.dateRange) }
  })
  return (res.rank || []).map((m: Record<string, any>) => ({
    name: m.shopName,
    value: Number(m.salesAmount) || 0
  })) as ChartPoint[]
}

/** 商户经营分析列表：同源聚合数据，前端分页 */
export async function getMerchantAnalysis(params: { pageNum: number; pageSize: number; dateRange?: string[] }) {
  const res = await request<Record<string, any>>({
    url: '/admin/statis/merchants',
    method: 'post',
    data: { ...rangeBody(params.dateRange) }
  })
  const list = (res.list || []).map((m: Record<string, any>) => {
    const sales = Number(m.salesAmount) || 0
    const refund = Number(m.refundMoney) || 0
    return {
      ...m,
      refundRate: sales + refund > 0 ? ((refund / (sales + refund)) * 100).toFixed(1) : '0.0',
      avgScore: 5
    }
  })
  const pageNum = params.pageNum || 1
  const pageSize = params.pageSize || 10
  return {
    list: list.slice((pageNum - 1) * pageSize, pageNum * pageSize),
    total: list.length
  }
}