import { request } from '@/utils/request'
import type { PayChannel } from '@/types'

/** 渠道编码 → 视图渠道类型（channelType 用于参数表单皮肤） */
const CHANNEL_TYPE_MAP: Record<string, string> = {
  WECHAT: 'wechat',
  WX: 'wechat',
  ALIPAY: 'alipay',
  UNIONPAY: 'unionpay'
}

/** 支付渠道列表：后端 {list: [{id,channelCode,channelName,baseRate,status,configJson}]} */
export async function getPayChannelList() {
  const res = await request<Record<string, any>>({
    url: '/admin/channel/list',
    method: 'post',
    data: {}
  })
  return (res.list || []).map((c: Record<string, any>) => ({
    id: c.id,
    channelCode: c.channelCode,
    channelName: c.channelName,
    channelType: CHANNEL_TYPE_MAP[c.channelCode] || 'wechat',
    baseRate: c.baseRate,
    status: c.status,
    configParams: parseConfig(c.configJson),
    configJson: c.configJson
  })) as PayChannel[]
}

/** 新增渠道（后端按 channelCode upsert） */
export function addPayChannel(data: Partial<PayChannel>) {
  return request({
    url: '/admin/channel/config',
    method: 'post',
    data: { channelCode: data.channelCode, channelName: data.channelName }
  })
}

/** 更新渠道 */
export function updatePayChannel(data: Partial<PayChannel>) {
  return request({
    url: '/admin/channel/config',
    method: 'post',
    data: { id: data.id, channelCode: data.channelCode, channelName: data.channelName, status: data.status }
  })
}

/** 启用/停用渠道 */
export function updatePayChannelStatus(data: { id: number; status: 0 | 1 }) {
  return request({
    url: '/admin/channel/status',
    method: 'post',
    data
  })
}

/** 保存渠道参数配置：configParams 对象 → configJson 字符串落库 */
export function savePayChannelConfig(data: { id: number; configParams: Record<string, string> }) {
  return request({
    url: '/admin/channel/config',
    method: 'post',
    data: { id: data.id, configJson: JSON.stringify(data.configParams) }
  })
}

function parseConfig(json?: string): Record<string, string> {
  if (!json) return {}
  try {
    return JSON.parse(json)
  } catch (e) {
    return {}
  }
}