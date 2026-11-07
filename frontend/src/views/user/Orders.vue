<template>
  <div class="page">
    <div class="navbar">
      <span class="back" @click="$router.push('/user/profile')">‹</span>
      <span class="title">我的订单</span>
    </div>
    <!-- 状态页签 -->
    <div class="tabs">
      <span v-for="t in tabs" :key="t.v" :class="{ active: status === t.v }" @click="switchTab(t.v)">{{ t.label }}</span>
    </div>
    <!-- 订单列表 -->
    <div v-for="o in list" :key="o.id" class="card order" @click="$router.push('/user/order-detail/' + o.orderNo)">
      <div class="o-head">
        <span class="shop">🏪 {{ o.shopName }}</span>
        <span class="state" :class="{ warn: o.status === -1 }">{{ stateLabel(o) }}</span>
      </div>
      <div v-for="it in o.items" :key="it.skuId" class="o-item">
        <img :src="it.image" />
        <div class="oi">
          <div class="t">{{ it.title }} <em>{{ it.specInfo }}</em></div>
          <div class="r"><span class="money">¥{{ fmtMoney(it.price) }}</span><span class="q">×{{ it.quantity }}</span></div>
        </div>
      </div>
      <div class="o-foot">
        <span class="money">¥{{ fmtMoney(o.payAmount || o.totalAmount) }}</span>
        <span class="ft">{{ o.createTime }}</span>
      </div>
      <!-- 待支付：倒计时进行中 -->
      <div v-if="o.status === -1" class="o-tip countdown" :class="{ urgent: remainSec(o) <= 120 }">
        <span v-if="remainSec(o) > 0">剩余 <b>{{ remainText(o) }}</b> 自动取消</span>
        <span v-else>支付超时，订单即将自动取消</span>
      </div>
      <!-- 超时关闭后：取消原因 -->
      <div v-else-if="o.status === 3" class="o-tip closed">
        <span>{{ o.closeReason === '超时未支付' ? '超时未支付，已自动取消' : (o.closeReason || '已取消') }}，库存已释放</span>
      </div>
      <div class="o-actions">
        <template v-if="o.status === -1">
          <button class="mini" @click.stop="cancel(o)">取消订单</button>
          <button class="mini primary" @click.stop="goPay(o)">去支付</button>
        </template>
        <template v-else-if="o.status === 0">
          <button class="mini" @click.stop="$router.push({ path: '/user/checkout', query: {} })" style="display:none"></button>
        </template>
        <template v-else-if="o.status === 4">
          <button class="mini" @click.stop="$router.push('/user/order-detail/' + o.orderNo)">查看物流</button>
          <button class="mini primary" @click.stop="confirmReceipt(o)">确认收货</button>
        </template>
        <template v-else-if="o.status === 0 || o.status === 5">
          <button class="mini primary" @click.stop="$router.push('/user/order-detail/' + o.orderNo)">订单详情</button>
        </template>
        <template v-else-if="o.status === 2">
          <button class="mini primary" @click.stop="$router.push('/user/refund/' + o.orderNo)">查看退款</button>
        </template>
        <template v-else-if="o.status === 3">
          <button class="mini primary" @click.stop="rebuy(o)">重新购买</button>
        </template>
      </div>
    </div>
    <div v-if="!list.length" class="empty">暂无订单</div>
    <Tabbar :active="2" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderUserList, orderCancel, orderConfirm } from '@/api'
import { fmtMoney } from '@/utils/const'
import Tabbar from '@/components/mobile/Tabbar.vue'

const router = useRouter()
const status = ref(-99)
const list = ref<any[]>([])
const now = ref(Date.now())
let ticker: any = null

const tabs = [
  { v: -99, label: '全部' },
  { v: -1, label: '待支付' },
  { v: 0, label: '待发货' },
  { v: 4, label: '待收货' },
  { v: 5, label: '已完成' },
  { v: 2, label: '已退款' },
  { v: 3, label: '已关闭' }
]

onMounted(() => {
  load()
  ticker = setInterval(() => (now.value = Date.now()), 1000)
})
onUnmounted(() => {
  if (ticker) clearInterval(ticker)
})

/** 时间字符串（yyyy-MM-dd HH:mm:ss）转毫秒时间戳 */
function toTs(s: string): number {
  return Date.parse(String(s || '').replace(' ', 'T')) || 0
}
/** 剩余支付秒数（倒计时进行中） */
function remainSec(o: any): number {
  const total = Number(o.timeoutSeconds || 0)
  if (o.status !== -1 || !total) return 0
  const left = Math.floor((toTs(o.createTime) + total * 1000 - now.value) / 1000)
  return left > 0 ? left : 0
}
/** 倒计时格式：9:59 */
function remainText(o: any): string {
  const s = remainSec(o)
  return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0')
}
const CLOSED_LABEL: Record<string, string> = { 超时未支付: '超时未支付', 用户取消: '已取消', 平台关闭: '已关闭' }
/** 状态标签：待支付统一显示「待支付」，关闭单按原因区分 */
function stateLabel(o: any): string {
  if (o.status === -1) return '待支付'
  if (o.status === 3) return CLOSED_LABEL[o.closeReason] || '已关闭'
  return o.statusText
}
function goPay(o: any) {
  router.push('/user/pay/' + o.orderNo + '?money=' + (o.payAmount || o.totalAmount) + '&merchantNo=' + o.merchantNo)
}
function rebuy(o: any) {
  const pid = o.items && o.items[0] && o.items[0].productId
  if (pid) router.push('/user/product/' + pid)
}
async function load() {
  const res = await orderUserList({ status: status.value, pageNum: 1, pageSize: 20 })
  list.value = res.records || []
}
function switchTab(v: any) {
  status.value = v
  load()
}
async function cancel(o: any) {
  if (!confirm('确定取消该订单？')) return
  await orderCancel({ orderNo: o.orderNo })
  load()
}
async function confirmReceipt(o: any) {
  if (!window.confirm('确认已收到货？')) return
  await orderConfirm({ orderNo: o.orderNo })
  load()
}
</script>

<style scoped>
.tabs { display: flex; background: #fff; overflow-x: auto; padding: 10px 4px; gap: 4px; }
.tabs span { flex: 1; text-align: center; font-size: 13px; color: #666; white-space: nowrap; padding: 4px 0; }
.tabs span.active { color: var(--primary); font-weight: 600; border-bottom: 2px solid var(--primary); }
.order { margin-bottom: 10px; }
.o-head { display: flex; justify-content: space-between; padding: 12px 14px 6px; }
.shop { font-size: 13px; font-weight: 600; }
.state { color: var(--primary); font-size: 13px; }
.o-item { display: flex; gap: 10px; padding: 6px 14px; }
.o-item img { width: 56px; height: 56px; border-radius: 8px; }
.oi { flex: 1; font-size: 13px; color: #333; min-width: 0; }
.t { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t em { color: #999; font-size: 11px; font-style: normal; margin-left: 6px; }
.r { display: flex; justify-content: space-between; margin-top: 6px; }
.q { color: #999; }
.o-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 6px 14px; align-items: baseline; }
.ft { color: #bbb; font-size: 11px; }
.o-actions { display: flex; justify-content: flex-end; gap: 8px; padding: 8px 14px; border-top: 1px solid #f2f2f2; }
.mini { padding: 6px 14px; border-radius: 14px; border: 1px solid #ddd; color: #666; font-size: 12px; background: #fff; }
.mini.primary { border-color: var(--primary); color: var(--primary); }
.state.warn { color: #ff3e3e; }
.o-tip { padding: 0 14px 8px; font-size: 12px; color: #ff7a00; }
.o-tip.countdown b { color: #ff3e3e; font-weight: 600; }
.o-tip.closed { color: #999; }
</style>