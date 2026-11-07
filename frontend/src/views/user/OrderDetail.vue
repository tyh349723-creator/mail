<template>
  <div class="page" v-if="order.orderNo">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">订单详情</span></div>

    <div class="status-banner" :class="'s' + order.status">
      <div class="s-title">{{ stateLabel }}</div>
      <div class="s-sub" v-if="order.status === -1">
        <template v-if="remainSec > 0">剩余 <b>{{ remainText }}</b> 自动取消</template>
        <template v-else>支付超时，订单即将自动取消</template>
      </div>
      <div class="s-sub" v-else-if="order.status === 3">订单{{ isTimeout ? '超时未支付，已自动取消' : '已取消' }}，库存已释放</div>
      <div class="s-sub" v-else-if="order.status === 4">商品已发出，请注意查收</div>
      <div class="s-sub" v-else-if="order.status === 2">退款已原路退至原支付渠道</div>
    </div>

    <!-- 倒计时区域：临近超时加强提醒 -->
    <div v-if="order.status === -1" class="countdown-card" :class="{ urgent: remainSec <= 120 }">
      <div class="cd-main" v-if="remainSec > 0">剩余 <b>{{ remainText }}</b> 自动取消</div>
      <div class="cd-main" v-else>支付超时</div>
      <div class="cd-sub">
        <template v-if="remainSec > 120">请在有效期内完成支付，超时订单将自动取消</template>
        <template v-else-if="remainSec > 0">支付时间即将结束，请尽快完成支付</template>
        <template v-else>订单即将自动取消</template>
      </div>
    </div>

    <div v-if="refundPending" class="refund-flag" @click="goRefund">
      <span>⏳ 退款申请处理中 · ¥{{ fmtMoney(order.payAmount || order.totalAmount) }}</span>
      <em>查看进度 ›</em>
    </div>

    <div class="card addr">
      <div class="a1">{{ order.consignee }} <em>{{ order.phone }}</em></div>
      <div class="a2">{{ order.address }}</div>
    </div>

    <div class="card shop-line">🏪 {{ order.shopName }}<span class="no">{{ order.merchantNo }}</span></div>
    <div class="card">
      <div v-for="it in order.items" :key="it.skuId" class="item">
        <img :src="it.image" />
        <div class="i">
          <div class="t">{{ it.title }} <em>{{ it.specInfo }}</em></div>
          <div class="r"><span class="money">¥{{ fmtMoney(it.price) }}</span><span>×{{ it.quantity }}</span></div>
        </div>
      </div>
    </div>

    <div class="card meta">
      <div class="m-row"><span>订单编号</span><em>{{ order.orderNo }}</em></div>
      <div class="m-row"><span>下单时间</span><em>{{ order.createTime }}</em></div>
      <div class="m-row" v-if="order.payTime"><span>支付时间</span><em>{{ order.payTime }}</em></div>
      <div class="m-row" v-if="order.channel"><span>支付方式</span><em>{{ order.channel }}</em></div>
      <div class="m-row" v-if="order.status === 3"><span>取消原因</span><em>{{ order.closeReason || '已取消' }}</em></div>
      <div class="m-row v" v-if="order.freight"><span>运费</span><em class="money">¥{{ fmtMoney(order.freight) }}</em></div>
      <div class="m-row v"><span>实付金额</span><em class="money big">¥{{ fmtMoney(order.payAmount || order.totalAmount) }}</em></div>
    </div>

    <div class="actions">
      <template v-if="order.status === -1">
        <button class="mini" @click="cancel">取消订单</button>
        <button class="mini primary" @click="$router.push('/user/pay/' + order.orderNo + '?money=' + (order.payAmount || order.totalAmount) + '&merchantNo=' + order.merchantNo)">去支付</button>
      </template>
      <template v-else-if="order.status === 4">
        <button class="mini" @click="confirmReceipt">确认收货</button>
        <button v-if="canApply" class="mini primary" @click="openRefund">申请退款</button>
      </template>
      <template v-else-if="order.status === 0 || order.status === 5">
        <button v-if="canApply" class="mini primary" @click="openRefund">申请退款</button>
      </template>
      <template v-else-if="order.status === 2">
        <button class="mini primary" @click="goRefund">查看退款详情</button>
      </template>
      <template v-else-if="order.status === 3">
        <button class="mini primary" @click="rebuy">重新购买</button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderUserDetail, orderCancel, orderConfirm, orderRefundList } from '@/api'
import { fmtMoney } from '@/utils/const'

const route = useRoute()
const router = useRouter()
const order = ref<any>({})
const refunds = ref([] as any[])

const refundPending = computed(() => refunds.value.some((r: any) => Number(r.status) === 0))
const canApply = computed(() => !refundPending.value)

const now = ref(Date.now())
let ticker: any = null
/** 是否因超时未支付自动取消 */
const isTimeout = computed(() => String((order.value as any).closeReason || '') === '超时未支付')
/** 顶部状态文案 */
const stateLabel = computed(() => {
  const st = (order.value as any).status
  if (st === -1) return '待支付'
  if (st === 3) return isTimeout.value ? '超时未支付' : '已取消'
  return (order.value as any).statusText
})
/** 剩余支付秒数 */
const remainSec = computed(() => {
  const o = order.value as any
  const total = Number(o.timeoutSeconds || 0)
  if (o.status !== -1 || !total) return 0
  const left = Math.floor((Date.parse(String(o.createTime || '').replace(' ', 'T')) + total * 1000 - now.value) / 1000)
  return left > 0 ? left : 0
})
/** 倒计时格式：9:59 */
const remainText = computed(() => {
  const s = remainSec.value
  return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0')
})

onMounted(async () => {
  ticker = setInterval(() => (now.value = Date.now()), 1000)
  const [o, rf] = await Promise.all([
    orderUserDetail({ orderNo: route.params.orderNo }),
    orderRefundList({ orderNo: route.params.orderNo }).catch(() => ({ refunds: [] }))
  ])
  order.value = o.order || {}
  refunds.value = rf.refunds || []
})

onUnmounted(() => {
  if (ticker) clearInterval(ticker)
})

async function cancel() {
  if (!confirm('确定取消订单？')) return
  await orderCancel({ orderNo: order.value.orderNo })
  router.back()
}
async function confirmReceipt() {
  if (!window.confirm('确认已收到货？')) return
  await orderConfirm({ orderNo: order.value.orderNo })
  order.value.status = 5
  order.value.statusText = '已完成'
}
function openRefund() {
  router.push('/user/refund-apply/' + order.value.orderNo)
}
function goRefund() {
  router.push('/user/refund/' + order.value.orderNo)
}
/** 重新购买：跳转订单首件商品详情 */
function rebuy() {
  const pid = (order.value as any).items && (order.value as any).items[0] && (order.value as any).items[0].productId
  if (pid) router.push('/user/product/' + pid)
}
</script>

<style scoped>
.status-banner { background: linear-gradient(135deg, #ff8a5c, #ff3e3e); color: #fff; padding: 22px 18px; }
.status-banner.s0 { background: linear-gradient(135deg, #5cb8ff, #1677ff); }
.status-banner.s2 { background: linear-gradient(135deg, #ff8a5c, #ff3e3e); }
.status-banner.s4 { background: linear-gradient(135deg, #69d4a6, #00b578); }
.status-banner.s5 { background: linear-gradient(135deg, #8a8f9b, #5f6670); }
.refund-flag { display: flex; justify-content: space-between; align-items: center; margin: 10px 12px; padding: 10px 14px; background: #fff7e6; border: 1px solid #ffe7ba; border-radius: 10px; font-size: 13px; color: #985e00; }
.refund-flag em { font-style: normal; color: var(--primary); }
.s-title { font-size: 20px; font-weight: 700; }
.s-sub { font-size: 12px; opacity: .9; margin-top: 6px; }
.s-sub b { font-weight: 700; }
.countdown-card { margin: 10px 12px; padding: 12px 14px; background: #fff7e6; border: 1px solid #ffe7ba; border-radius: 10px; }
.countdown-card .cd-main { font-size: 14px; color: #985e00; font-weight: 600; }
.countdown-card .cd-main b { color: #ff3e3e; }
.countdown-card .cd-sub { font-size: 12px; color: #a9884f; margin-top: 4px; }
.countdown-card.urgent { background: #fff1f0; border-color: #ffccc7; }
.countdown-card.urgent .cd-main { color: #cf1322; }
.countdown-card.urgent .cd-sub { color: #d46b6b; }
.addr { padding: 14px 16px; }
.a1 { font-size: 15px; font-weight: 600; }
.a1 em { color: #999; font-weight: 400; margin-left: 8px; font-size: 13px; }
.a2 { color: #888; font-size: 13px; margin-top: 4px; }
.shop-line { padding: 12px 16px; font-size: 14px; font-weight: 600; display: flex; justify-content: space-between; }
.no { color: #ccc; font-weight: 400; font-size: 12px; }
.item { display: flex; gap: 10px; padding: 8px 14px; }
.item img { width: 56px; height: 56px; border-radius: 8px; }
.i { flex: 1; font-size: 13px; color: #333; min-width: 0; }
.t { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t em { color: #999; font-size: 11px; font-style: normal; margin-left: 6px; }
.r { display: flex; justify-content: space-between; margin-top: 6px; }
.meta { padding: 6px 16px; }
.m-row { display: flex; justify-content: space-between; padding: 7px 0; font-size: 13px; color: #666; }
.m-row em { font-style: normal; color: #333; }
.m-row.v { border-top: 1px dashed #eee; margin-top: 4px; padding-top: 10px; }
.money.big { font-size: 18px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; padding: 12px 16px 24px; }
.mini { padding: 8px 18px; border-radius: 16px; border: 1px solid #ddd; color: #666; font-size: 13px; background: #fff; }
.mini.primary { border-color: var(--primary); color: var(--primary); }
</style>