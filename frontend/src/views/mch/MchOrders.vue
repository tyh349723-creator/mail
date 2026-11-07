<template>
  <div class="page">
    <div class="navbar"><span class="title">订单管理</span></div>

    <!-- 一级切换：订单 / 交易流水 / 退款记录 -->
    <div class="tabs main">
      <span v-for="t in mainTabs" :key="t.v" :class="{ active: tab === t.v }" @click="switchMain(t.v)">{{ t.label }}</span>
    </div>

    <!-- ==================== 订单 ==================== -->
    <template v-if="tab === 'orders'">
      <div class="tabs">
        <span v-for="t in tabs" :key="t.v" :class="{ active: status === t.v }" @click="switchTab(t.v)">{{ t.label }}</span>
      </div>

      <div v-for="o in list" :key="o.id" class="card order">
        <div class="o-head" @click="open = open === o.id ? '' : o.id">
          <span class="buyer">👤 {{ o.buyerName }} <em>{{ o.orderNo }}</em></span>
          <span class="state">{{ o.statusText }}</span>
        </div>
        <div v-for="it in o.items" :key="it.skuId" class="o-item">
          <img :src="it.image" />
          <div class="oi">
            <div class="t">{{ it.title }} <em>{{ it.specInfo }}</em></div>
            <div class="r"><span class="money">¥{{ fmtMoney(it.price) }}</span><span>×{{ it.quantity }}</span></div>
          </div>
        </div>
        <div class="o-foot">
          <span class="money">实付 ¥{{ fmtMoney(o.payAmount || o.totalAmount) }}</span>
          <span class="ft">{{ o.createTime }}</span>
        </div>
        <div v-if="open === o.id" class="o-detail">
          <div class="d-row"><span>买家</span><em>{{ o.consignee }} {{ o.phone }}</em></div>
          <div class="d-row"><span>地址</span><em>{{ o.address }}</em></div>
          <div class="d-row" v-if="o.remark"><span>备注</span><em>{{ o.remark }}</em></div>
          <div class="d-row" v-if="o.channel"><span>支付方式</span><em>{{ o.channel }}</em></div>
          <div class="d-row" v-if="o.payTime"><span>支付时间</span><em>{{ o.payTime }}</em></div>
        </div>
        <div class="o-actions" v-if="o.status === 0">
          <button class="mini primary" @click="ship(o)">发货</button>
        </div>
      </div>
      <div v-if="!list.length" class="empty">暂无订单</div>
    </template>

    <!-- ==================== 交易流水（5.2.2） ==================== -->
    <template v-else-if="tab === 'trades'">
      <div class="filter-row">
        <select v-model="tradeQuery.wayCode" class="sel">
          <option value="">全部渠道</option>
          <option v-for="(name, code) in channelText" :key="code" :value="code">{{ name }}</option>
        </select>
        <select v-model="tradeQuery.status" class="sel">
          <option value="-99">全部状态</option>
          <option v-for="(name, code) in payStatus" :key="code" :value="code">{{ name }}</option>
        </select>
        <button class="mini primary" @click="loadTrades">查询</button>
        <button class="mini" :disabled="!trades.length" @click="exportCsv">导出 CSV</button>
      </div>

      <div v-for="t in trades" :key="t.payOrderId" class="card rec">
        <div class="r1">
          <span class="tt">{{ t.channelName || t.wayCode }}</span>
          <em class="money">¥{{ fmtMoney(t.money) }}</em>
        </div>
        <div class="r2"><span>{{ t.orderNo }}</span><em class="st" :class="'s' + t.status">{{ t.statusText }}</em></div>
        <div class="r3">
          <span>手续费 ¥{{ fmtMoney(t.fee) }}</span>
          <span v-if="t.payTime">支付 {{ t.payTime }}</span>
        </div>
      </div>
      <div v-if="!trades.length" class="empty">暂无交易流水</div>
      <div v-if="trades.length" class="more" @click="loadAllTrades">加载更多（当前 {{ trades.length }} 条）</div>
    </template>

    <!-- ==================== 退款记录（5.2.3） ==================== -->
    <template v-else>
      <div class="tabs">
        <span v-for="t in refundTabs" :key="t.v" :class="{ active: refundStatus === t.v }" @click="switchRefund(t.v)">{{ t.label }}</span>
      </div>

      <div v-for="r in refunds" :key="r.id" class="card rec">
        <div class="r1">
          <span class="tt">{{ r.refundOrderId }}</span>
          <em class="r-money">- ¥{{ fmtMoney(r.money) }}</em>
        </div>
        <div class="r2"><span>原支付单 {{ r.orderNo }}</span><em class="wst"><b class="st" :class="'s' + r.status">{{ r.statusText }}</b></em></div>
        <div class="r3"><span>原因：{{ r.reason }}</span></div>
        <div class="r3" v-if="r.auditRemark"><span>处理意见：{{ r.auditRemark }}</span></div>
        <div class="r3 dim">
          <span>申请 {{ r.createTime }}</span>
          <span v-if="r.auditTime">处理 {{ r.auditTime }}</span>
        </div>
        <!-- 待处理退款：商户处理（同意/驳回） -->
        <div class="r-actions" v-if="r.status === 0">
          <button class="mini primary" @click="handleRefund(r, true)">同意退款</button>
          <button class="mini" @click="handleRefund(r, false)">驳回</button>
        </div>
      </div>
      <div v-if="!refunds.length" class="empty">暂无退款记录</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mchOrderList, mchOrderShip, mchRefundList, mchRefundHandle, payListMch } from '@/api'
import { fmtMoney } from '@/utils/const'

// ---------- 一级页签 ----------
const tab = ref('orders')
const mainTabs = [
  { v: 'orders', label: '订单' },
  { v: 'trades', label: '交易流水' },
  { v: 'refunds', label: '退款记录' }
]
function switchMain(t: string) {
  tab.value = t
  if (t === 'orders') load()
  else if (t === 'trades') loadTrades()
  else loadRefunds()
}

// ---------- 订单 ----------
const status = ref(-99)
const open = ref('')
const list = ref<any[]>([])
const tabs = [
  { v: -99, label: '全部' },
  { v: 0, label: '待发货' },
  { v: 4, label: '待收货' },
  { v: 5, label: '已完成' }
]
onMounted(load)
async function load() {
  const res = await mchOrderList({ orderState: status.value, pageNum: 1, pageSize: 100 })
  list.value = res.records || []
}
function switchTab(v: number) {
  status.value = v
  load()
}
async function ship(o: any) {
  const company = prompt('物流公司：', '顺丰速运')
  if (company === null) return
  const track = prompt('快递单号：', 'SF' + Date.now())
  if (track === null) return
  await mchOrderShip({ orderNo: o.orderNo })
  alert('已发货')
  load()
}

// ---------- 交易流水（5.2.2） ----------
const channelText = { WECHAT: '微信支付', ALIPAY: '支付宝', UNIONPAY: '银联支付' }
const payStatus: Record<string, string> = { '-1': '待支付', '0': '支付成功', '1': '支付失败', '2': '已退款', '3': '已关闭' }
const tradeQuery = ref({ wayCode: '', status: '-99', pageNum: 1 })
const trades = ref<any[]>([])
async function loadTrades() {
  tradeQuery.value.pageNum = 1
  fetchTrades()
}
async function fetchTrades() {
  const res = await payListMch({ ...tradeQuery.value, status: Number(tradeQuery.value.status), pageSize: 50 })
  trades.value = res.records || []
}
async function loadAllTrades() {
  const res = await payListMch({ ...tradeQuery.value, status: Number(tradeQuery.value.status), pageSize: 1000 })
  trades.value = res.records || []
}
function exportCsv() {
  const header = ['支付单号', '订单号', '渠道', '交易额', '手续费', '状态', '支付时间', '创建时间']
  const rows = trades.value.map((t: any) => [t.payOrderId, t.orderNo, t.channelName || t.wayCode, t.money, t.fee, t.statusText, t.payTime, t.createTime])
  const csv = '﻿' + [header, ...rows].map((r) => r.map((v) => `"${String(v ?? '')}"`).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `交易流水_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

// ---------- 退款记录（5.2.3） ----------
const refundStatus = ref(-99)
const refunds = ref<any[]>([])
const refundTabs = [
  { v: -99, label: '全部' },
  { v: 0, label: '待处理' },
  { v: 1, label: '已处理' },
  { v: 2, label: '退款失败' },
  { v: 3, label: '已撤销' }
]
async function loadRefunds() {
  const res = await mchRefundList({ status: refundStatus.value, pageNum: 1, pageSize: 100 })
  refunds.value = res.records || []
}
function switchRefund(v: number) {
  refundStatus.value = v
  loadRefunds()
}

/** 商户处理退款：同意 → 钱包扣回、订单自动置已退款；驳回 → 填写原因 */
async function handleRefund(r: any, approve: boolean) {
  if (approve) {
    if (!confirm(`确认同意退款单「${r.refundOrderId}」，金额 ¥${fmtMoney(r.money)}？同意后将从商户钱包扣回该笔金额。`)) return
    await mchRefundHandle({ id: r.id, approve: true })
    alert('已处理，退款完成')
  } else {
    const reason = prompt('驳回原因：', '')
    if (reason === null) return
    await mchRefundHandle({ id: r.id, approve: false, reason })
    alert('已驳回')
  }
  loadRefunds()
}
</script>

<style scoped>
.tabs { display: flex; background: #fff; padding: 10px 4px; }
.tabs.main { border-bottom: 1px solid #f2f2f2; }
.tabs span { flex: 1; text-align: center; font-size: 13px; color: #666; padding: 4px 0; }
.tabs span.active { color: var(--primary); font-weight: 600; border-bottom: 2px solid var(--primary); }
.filter-row { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: #fff; }
.sel { flex: 1; height: 30px; border: 1px solid #e5e5e5; border-radius: 6px; font-size: 12px; background: #fff; }
.order { margin-bottom: 10px; }
.o-head { display: flex; justify-content: space-between; padding: 12px 14px 6px; }
.buyer { font-size: 13px; font-weight: 600; }
.buyer em { color: #999; font-weight: 400; font-size: 11px; margin-left: 6px; }
.state { color: var(--primary); font-size: 13px; }
.o-item { display: flex; gap: 10px; padding: 6px 14px; }
.o-item img { width: 52px; height: 52px; border-radius: 8px; }
.oi { flex: 1; font-size: 13px; color: #333; min-width: 0; }
.t { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t em { color: #999; font-size: 11px; font-style: normal; margin-left: 6px; }
.r { display: flex; justify-content: space-between; margin-top: 4px; }
.o-foot { display: flex; justify-content: space-between; padding: 6px 14px; align-items: baseline; }
.ft { color: #bbb; font-size: 11px; }
.o-detail { margin: 0 14px 8px; background: #fafafa; border-radius: 8px; padding: 8px 10px; }
.d-row { display: flex; gap: 10px; padding: 4px 0; font-size: 12px; color: #666; }
.d-row span { color: #999; flex-shrink: 0; }
.d-row em { font-style: normal; }
.o-actions { display: flex; justify-content: flex-end; padding: 8px 14px; border-top: 1px solid #f2f2f2; }
.mini { padding: 7px 16px; border-radius: 15px; border: 1px solid #e5e5e5; background: #fff; color: #666; font-size: 12px; }
.mini.primary { background: var(--primary); border-color: var(--primary); color: #fff; }
.rec { padding: 12px 16px; margin-bottom: 10px; }
.r1 { display: flex; justify-content: space-between; align-items: center; }
.tt { font-size: 14px; font-weight: 600; }
.money { color: #3b7cff; font-weight: 700; }
.r-money { color: #ff3e3e; font-weight: 600; font-style: normal; }
.r2 { display: flex; justify-content: space-between; color: #999; font-size: 12px; margin-top: 5px; }
.r3 { display: flex; justify-content: space-between; color: #999; font-size: 12px; margin-top: 4px; }
.r3.dim { color: #bbb; }
.st { font-style: normal; font-weight: 600; }
.wst { font-style: normal; }
.r-actions { display: flex; justify-content: flex-end; gap: 10px; padding-top: 10px; border-top: 1px solid #f2f2f2; margin-top: 8px; }
.more { text-align: center; color: var(--primary); font-size: 12px; padding: 10px; }
</style>