<template>
  <div class="page" v-if="loaded">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">退款详情</span></div>

    <!-- 无退款记录 -->
    <template v-if="!refund">
      <div class="card empty">
        <div class="e-icon">🧾</div>
        <div class="e-t">该订单暂无退款记录</div>
        <button v-if="refundable" class="primary" @click="$router.push('/user/refund-apply/' + orderNo)">申请退款</button>
      </div>
    </template>

    <template v-else>
      <!-- 状态横幅 -->
      <div class="banner" :class="'s' + refund.status">
        <div class="b-status">{{ refund.statusText }}</div>
        <div class="b-money">退款金额 ¥{{ fmtMoney(refund.money) }}</div>
        <div class="b-sub">{{ statusSub }}</div>
      </div>

      <!-- 进度时间线 -->
      <div class="card">
        <div class="card-title">退款进度</div>
        <div class="timeline">
          <div v-for="(s, i) in steps" :key="i" class="step" :class="s.state">
            <div class="rail">
              <i>{{ s.icon }}</i>
              <b v-if="i < steps.length - 1" class="line"></b>
            </div>
            <div class="body">
              <div class="st">{{ s.title }}</div>
              <div class="tm" v-if="s.time">{{ s.time }}</div>
              <div class="rm" v-if="s.remark">{{ s.remark }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 退款信息 -->
      <div class="card">
        <div class="card-title">退款信息</div>
        <div class="meta">
          <div class="m-row"><span>退款单号</span><em>{{ refund.refundOrderId }}</em></div>
          <div class="m-row"><span>原订单号</span><em>{{ refund.orderNo }}</em></div>
          <div class="m-row"><span>退款金额</span><em class="money">¥{{ fmtMoney(refund.money) }}</em></div>
          <div class="m-row"><span>退款原因</span><em>{{ refund.reason }}</em></div>
          <div class="m-row"><span>申请时间</span><em>{{ refund.createTime }}</em></div>
          <div class="m-row" v-if="refund.auditTime"><span>处理时间</span><em>{{ refund.auditTime }}</em></div>
        </div>
        <div class="opinion" v-if="refund.auditRemark">
          <div class="op-label">处理意见</div>
          <div class="op-text">{{ refund.auditRemark }}</div>
        </div>
      </div>

      <!-- 商品概要 -->
      <div class="card shop-card">
        <div class="shop-line">🏪 {{ order.shopName || '' }}</div>
        <div v-for="it in order.items || []" :key="it.skuId" class="item">
          <img :src="it.image" />
          <div class="i">
            <div class="t">{{ it.title }} <em>{{ it.specInfo }}</em></div>
            <div class="r"><span class="money">¥{{ fmtMoney(it.price) }}</span><span class="q">×{{ it.quantity }}</span></div>
          </div>
        </div>
      </div>

      <!-- 退款说明 -->
      <div class="card notice">
        <div class="card-title">退款说明</div>
        <p>· 退款申请提交后由商户处理，同意后款项将原路退回至您的支付账户。</p>
        <p>· 申请未通过后如需再次申请，可重新发起退款。</p>
        <p>· 商户处理前可随时撤销退款申请，撤销后订单状态恢复原样。</p>
      </div>
    </template>

    <!-- 撤销申请 -->
    <div v-if="refund && Number(refund.status) === 0" class="footer">
      <button class="cancel" @click="cancelApply">撤销退款申请</button>
    </div>
  </div>
  <div v-else class="loading">加载中…</div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderUserDetail, orderRefundList, orderRefundCancel } from '@/api'
import { fmtMoney, REFUND_STATUS } from '@/utils/const'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo as string

const loaded = ref(false)
const order = ref<any>({})
const refunds = ref([] as any[])

const refund = computed(() => refunds.value[0] || null)
const refundable = computed(() => [0, 4, 5].includes(Number(order.value.status)))

const statusSub = computed(() => {
  const st = Number(refund.value?.status)
  if (st === 1) return '退款已完成，款项退回至您的支付账户'
  if (st === 2) return '申请未通过，可重新发起申请'
  if (st === 0) return '商户正在处理，请耐心等待'
  if (st === 3) return '申请已由您主动撤销'
  return ''
})

const steps = computed(() => {
  const r = refund.value
  if (!r) return []
  const st = Number(r.status)
  return [
    { title: '退款申请已提交', time: r.createTime, state: 'done', icon: '✓', remark: '' },
    {
      title: '商户处理',
      time: st === 0 ? '' : r.auditTime || '',
      state: st === 0 ? 'doing' : 'done',
      icon: st === 0 ? '●' : '✓',
      remark: st === 0 ? '商户处理中，同意后自动原路退回' : '商户已处理'
    },
    {
      title: st === 1 ? '退款到账' : st === 2 ? '退款被驳回' : st === 3 ? '申请已撤销' : '等待处理结果',
      time: st !== 0 ? r.auditTime || '' : '',
      state: st === 1 ? 'done' : st === 2 ? 'fail' : st === 3 ? 'cancel' : 'todo',
      icon: st === 1 ? '✓' : st === 2 ? '✕' : st === 3 ? '—' : '○',
      remark: st === 2 ? r.auditRemark || '申请未通过' : ''
    }
  ]
})

onMounted(load)
async function load() {
  const [o, rf] = await Promise.all([
    orderUserDetail({ orderNo }),
    orderRefundList({ orderNo }).catch(() => ({ refunds: [] }))
  ])
  order.value = o.order || {}
  refunds.value = rf.refunds || []
  loaded.value = true
}

async function cancelApply() {
  if (!confirm('确定撤销退款申请吗？撤销后订单状态将恢复原样。')) return
  await orderRefundCancel({ orderNo })
  await load()
}
</script>

<style scoped>
.navbar { display: flex; align-items: center; height: 44px; padding: 0 12px; background: #fff; }
.back { font-size: 22px; color: #333; width: 24px; }
.title { font-size: 16px; font-weight: 600; }
.banner { color: #fff; padding: 20px 18px 18px; }
.banner.s0 { background: linear-gradient(135deg, #ffb84d, #ff8a00); }
.banner.s1 { background: linear-gradient(135deg, #69d4a6, #00b578); }
.banner.s2 { background: linear-gradient(135deg, #ff8a5c, #ff3e3e); }
.banner.s3 { background: linear-gradient(135deg, #a8adb8, #6b7280); }
.b-status { font-size: 20px; font-weight: 700; }
.b-money { font-size: 28px; font-weight: 800; margin-top: 8px; }
.b-sub { font-size: 12px; opacity: .9; margin-top: 6px; }
.card { background: #fff; border-radius: 12px; margin: 10px 12px; padding: 4px 14px 12px; }
.card-title { font-size: 14px; font-weight: 600; padding: 12px 0 8px; }
/* 时间线 */
.timeline { padding: 2px 0 8px; }
.step { display: flex; gap: 12px; }
.rail { width: 22px; display: flex; flex-direction: column; align-items: center; }
.rail i { width: 20px; height: 20px; border-radius: 50%; font-style: normal; font-size: 12px; line-height: 20px; text-align: center; background: #e5e5e5; color: #fff; flex-shrink: 0; margin-top: 2px; }
.rail .line { width: 2px; flex: 1; background: #e5e5e5; margin: 4px 0; min-height: 26px; }
.step.done .rail i { background: #00b578; }
.step.doing .rail i { background: #ff8a00; box-shadow: 0 0 0 4px rgba(255, 138, 0, .15); }
.step.fail .rail i { background: #ff3e3e; }
.step.cancel .rail i { background: #9aa0ac; }
.step.done .line { background: #00b578; }
.body { flex: 1; padding-bottom: 18px; }
.st { font-size: 14px; font-weight: 600; color: #333; }
.step.doing .st { color: #ff8a00; }
.step.todo .st { color: #bbb; }
.step.fail .st { color: #ff3e3e; }
.step.cancel .st { color: #9aa0ac; }
.tm { font-size: 12px; color: #999; margin-top: 3px; }
.rm { font-size: 12px; color: #ff3e3e; margin-top: 3px; background: #fff1f0; border-radius: 6px; padding: 6px 8px; }
/* 退款信息 */
.meta { font-size: 13px; }
.m-row { display: flex; justify-content: space-between; padding: 6px 0; color: #666; }
.m-row span { color: #999; flex-shrink: 0; }
.m-row em { font-style: normal; color: #333; text-align: right; word-break: break-all; }
.money { color: var(--primary); font-weight: 700; }
.opinion { border-top: 1px dashed #eee; margin-top: 4px; padding: 10px 0 4px; }
.op-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.op-text { font-size: 13px; color: #ff3e3e; }
/* 商品概要 */
.shop-line { font-size: 13px; font-weight: 600; padding: 12px 0 6px; }
.item { display: flex; gap: 10px; padding: 6px 0; }
.item img { width: 52px; height: 52px; border-radius: 8px; }
.i { flex: 1; font-size: 13px; color: #333; min-width: 0; }
.t { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t em { color: #999; font-size: 11px; font-style: normal; margin-left: 6px; }
.r { display: flex; justify-content: space-between; margin-top: 4px; }
.q { color: #999; }
/* 说明 */
.notice p { font-size: 12px; color: #999; line-height: 1.8; margin: 0 0 6px; }
/* 空态 / 撤销 */
.empty { text-align: center; padding: 40px 0; }
.e-icon { font-size: 40px; }
.e-t { color: #999; font-size: 14px; margin: 10px 0 18px; }
.primary { padding: 9px 26px; border-radius: 18px; border: none; background: var(--primary); color: #fff; font-size: 13px; }
.footer { padding: 6px 16px 30px; }
.cancel { width: 100%; height: 44px; border-radius: 22px; border: 1px solid #ff3e3e; background: #fff; color: #ff3e3e; font-size: 15px; }
.loading, .loading-msg { text-align: center; color: #999; padding: 60px 0; }
</style>