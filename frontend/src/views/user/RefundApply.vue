<template>
  <div class="page" v-if="order.orderNo">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">申请退款</span></div>

    <!-- 订单状态不可退款 -->
    <div v-if="!refundable" class="tips block">
      <div>当前订单状态（{{ order.statusText }}）不可申请退款</div>
    </div>

    <!-- 已有待处理退款 -->
    <div v-else-if="pending" class="tips">
      <div>⚠️ 该订单已提交退款申请，正在等待商户处理，请勿重复提交</div>
      <button class="mini primary" @click="$router.replace('/user/refund/' + order.orderNo)">查看退款进度 ›</button>
    </div>

    <!-- 商品卡 -->
    <div class="card shop-line">🏪 {{ order.shopName }}<span class="no">{{ order.merchantNo }}</span></div>
    <div class="card">
      <div v-for="it in order.items" :key="it.skuId" class="item">
        <img :src="it.image" />
        <div class="i">
          <div class="t">{{ it.title }} <em>{{ it.specInfo }}</em></div>
          <div class="r"><span class="money">¥{{ fmtMoney(it.price) }}</span><span class="q">×{{ it.quantity }}</span></div>
        </div>
      </div>
      <div class="order-no">订单编号：{{ order.orderNo }}</div>
    </div>

    <!-- 退款金额 -->
    <div class="card">
      <div class="card-title">退款金额</div>
      <div class="amount-row"><span>可退金额</span><em class="money big">¥{{ fmtMoney(amount) }}</em></div>
      <div class="amount-sub">实付金额（含运费），全额退回原支付渠道</div>
    </div>

    <!-- 退款原因 -->
    <div class="card">
      <div class="card-title">退款原因</div>
      <div class="reasons">
        <span v-for="r in reasons" :key="r" class="chip" :class="{ on: reason === r }" @click="pick(r)">{{ r }}</span>
      </div>
      <textarea v-if="reason === '其他'" v-model="otherReason" class="other" rows="2" maxlength="100" placeholder="请描述具体原因（100 字以内）"></textarea>
    </div>

    <div class="submit" v-if="refundable">
      <button class="primary" :disabled="!canSubmit" @click="submit">{{ !pending ? '提交退款申请' : '查看退款进度' }}</button>
      <div class="submit-tip">提交后由商户处理，同意后即原路退回</div>
    </div>
  </div>
  <div v-else class="loading">加载中…</div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderUserDetail, orderRefundApply, orderRefundList } from '@/api'
import { fmtMoney } from '@/utils/const'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo as string

const order = ref<any>({})
const refunds = ref([] as any[])
const reason = ref('七天无理由退换货')
const otherReason = ref('')

const reasons = ['七天无理由退换货', '商品质量问题', '发错、漏发商品', '与图片描述不符', '不想要了', '其他']

const refundable = computed(() => [0, 4, 5].includes(Number(order.value.status)))
const pending = computed(() => refunds.value.some((r: any) => Number(r.status) === 0))
const amount = computed(() => order.value.payAmount || order.value.totalAmount || 0)
const finalReason = computed(() => (reason.value === '其他' ? otherReason.value.trim() : reason.value))
const canSubmit = computed(() => refundable.value && !pending.value && !!finalReason.value)

onMounted(async () => {
  const [o, rf] = await Promise.all([
    orderUserDetail({ orderNo }),
    orderRefundList({ orderNo }).catch(() => ({ refunds: [] }))
  ])
  order.value = o.order || {}
  refunds.value = rf.refunds || []
})

function pick(r: string) {
  reason.value = r
  if (r !== '其他') otherReason.value = ''
}

async function submit() {
  try {
    await orderRefundApply({ orderNo, money: amount.value, reason: finalReason.value })
    router.replace('/user/refund/' + orderNo)
  } catch (e: any) {
    // 统一请求层已 toast，这里兜底处理重复提交等校验
    if (/已存在待处理的退款申请/.test(e.message || '')) {
      router.replace('/user/refund/' + orderNo)
    }
  }
}
</script>

<style scoped>
.navbar { display: flex; align-items: center; height: 44px; padding: 0 12px; background: #fff; }
.back { font-size: 22px; color: #333; width: 24px; }
.title { font-size: 16px; font-weight: 600; }
.tips { margin: 10px 12px; padding: 12px; background: #fff7e6; border: 1px solid #ffe7ba; border-radius: 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.tips.block { justify-content: flex-start; color: #666; }
.tips div { font-size: 13px; color: #985e00; }
.mini { padding: 6px 14px; border-radius: 14px; border: 1px solid var(--primary); color: var(--primary); font-size: 12px; background: #fff; flex-shrink: 0; }
.card { background: #fff; border-radius: 12px; margin: 10px 12px; padding: 6px 14px; }
.shop-line { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; font-size: 14px; font-weight: 600; }
.no { color: #ccc; font-weight: 400; font-size: 12px; }
.item { display: flex; gap: 10px; padding: 8px 0; }
.item img { width: 56px; height: 56px; border-radius: 8px; }
.i { flex: 1; font-size: 13px; color: #333; min-width: 0; }
.t { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t em { color: #999; font-size: 11px; font-style: normal; margin-left: 6px; }
.r { display: flex; justify-content: space-between; margin-top: 6px; }
.q { color: #999; }
.order-no { border-top: 1px dashed #eee; margin-top: 4px; padding: 8px 0; color: #bbb; font-size: 12px; }
.card-title { font-size: 14px; font-weight: 600; padding: 10px 0 8px; }
.amount-row { display: flex; justify-content: space-between; align-items: baseline; padding: 6px 0; }
.amount-row span { font-size: 13px; color: #666; }
.money.big { font-size: 20px; color: var(--primary); font-style: normal; }
.amount-sub { font-size: 12px; color: #bbb; padding-bottom: 10px; }
.reasons { display: flex; flex-wrap: wrap; gap: 10px; padding: 6px 0 12px; }
.chip { padding: 8px 14px; border-radius: 16px; border: 1px solid #e5e5e5; background: #fff; color: #666; font-size: 13px; }
.chip.on { border-color: var(--primary); color: var(--primary); background: rgba(63, 124, 255, 0.06); }
.other { width: 100%; box-sizing: border-box; border: 1px solid #e5e5e5; border-radius: 8px; padding: 8px 10px; font-size: 13px; margin-bottom: 10px; font-family: inherit; resize: none; }
.submit { padding: 14px 16px 30px; }
.submit .primary { width: 100%; height: 44px; border-radius: 22px; border: none; background: var(--primary); color: #fff; font-size: 15px; }
.submit .primary:disabled { opacity: 0.5; }
.submit-tip { text-align: center; color: #bbb; font-size: 12px; margin-top: 8px; }
</style>