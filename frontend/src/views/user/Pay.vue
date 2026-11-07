<template>
  <div class="page pay-page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">收银台</span></div>

    <div class="card pay-amount">
      <div class="lab">订单 {{ orderNo }}</div>
      <div class="amt"><span class="rmb">¥</span><span class="money">{{ fmtMoney(money) }}</span></div>
      <div v-if="timeoutMin" class="countdown">请在 <b>{{ timeoutMin }}</b> 分钟内完成支付</div>
    </div>

    <!-- 渠道选择 -->
    <div class="section-title">选择支付方式</div>
    <div class="card channel-list">
      <div
        v-for="c in channels"
        :key="c.wayCode"
        class="channel"
        :class="{ active: wayCode === c.wayCode }"
        @click="wayCode = c.wayCode"
      >
        <span class="ch-icon" :class="'ch-' + c.wayCode.toLowerCase()">{{ iconOf(c.wayCode) }}</span>
        <span class="ch-name">{{ c.channelName }}</span>
        <span class="check" :class="{ on: wayCode === c.wayCode }">✓</span>
      </div>
      <div v-if="!channels.length" class="channel-empty">暂无可用支付方式</div>
    </div>

    <!-- 分为多个商户订单时提示 -->
    <div v-if="(orderNos || []).length > 1" class="multi-tip">订单已按商家拆分（{{ orderNos.length }} 笔）</div>

    <div class="pay-footer">
      <button class="btn-primary" :disabled="!wayCode || paying" @click="pay">
        {{ paying ? '正在唤起支付...' : '立即支付 ¥' + fmtMoney(money) }}
      </button>
      <button class="btn-plain" @click="closeOrder">暂不支付，稍后再付</button>
      <p class="foot-tip">支付完成后会自动返回，可在订单详情查看支付状态</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { payChannels, payCreate, payQuery, orderUserDetail } from '@/api'
import { useCartStore } from '@/store/cart'
import { fmtMoney } from '@/utils/const'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const orderNo = route.params.orderNo
const money = ref(Number(route.query.money || 0))
// 支付超时秒数 → 分钟展示
const timeoutMin = computed(() => Math.round(Number(route.query.timeout || 0) / 60))
const merchantNo = ref(String(route.query.merchantNo || ''))
const channels = ref<Array<{ wayCode: string; channelName: string }>>([])
const wayCode = ref('')
const paying = ref(false)
const orderNos = ref([orderNo])
const payOrderId = ref('')

/** 移动端（含微信/支付宝内嵌浏览器）：同标签跳转更可靠，避免新窗口被拦截 */
const isMobile = () => /Android|iPhone|iPad|iPod|Mobile|HarmonyOS|MicroMessenger/i.test(navigator.userAgent)

onMounted(async () => {
  const res = await payChannels()
  channels.value = res.list || []
  // 结算页已选支付方式（query.payment）时直接预选，否则默认第一个渠道
  const preset = String(route.query.payment || '')
  if (preset && channels.value.some((c) => c.wayCode === preset)) wayCode.value = preset
  else if (channels.value.length) wayCode.value = channels.value[0].wayCode
})

/** 兜底：历史深链接/订单列表入口可能没带 merchantNo（或金额），从订单详情补全后再下单 */
async function ensurePayParams() {
  if (merchantNo.value && money.value) return
  const res = await orderUserDetail({ orderNo })
  const o = res.order || {}
  if (!merchantNo.value) merchantNo.value = o.merchantNo || ''
  if (!money.value) money.value = Number(o.payAmount || o.totalAmount || 0)
}

async function pay() {
  if (paying.value) return
  paying.value = true
  try {
    await ensurePayParams()
    const res = await payCreate({ orderNo, merchantNo: merchantNo.value, money: money.value, wayCode: wayCode.value })
    payOrderId.value = res.payOrderId
    // 移动端同标签跳转支付宝/模拟收银台（返回时由渠道回跳结果页）；桌面端新标签打开并轮询结果
    if (isMobile()) {
      location.href = res.payUrl
      return
    }
    const win = window.open(res.payUrl, '_blank')
    if (!win) {
      location.href = res.payUrl
      return
    }
    await waitPaid()
  } catch (e: any) {
    alert(e.message || '支付失败')
    paying.value = false
  }
}

/** 轮询支付结果（收银台支付成功后页面会自动刷新状态） */
function waitPaid() {
  return new Promise<void>((resolve) => {
    const timer = setInterval(async () => {
      try {
        const res = await payQuery({ payOrderId: payOrderId.value })
        if (res.payOrder?.status === 0) {
          clearInterval(timer)
          paying.value = false
          alert('支付成功！')
          await cart.load()
          router.replace('/user/order-detail/' + orderNo)
          resolve()
        }
      } catch (e) { /* 继续轮询 */ }
    }, 2000)
    setTimeout(() => { clearInterval(timer); paying.value = false; resolve() }, 120000)
  })
}

function closeOrder() {
  cart.load()
  router.replace('/user/orders')
}

function iconOf(code: any) {
  return ({ WECHAT: '微', ALIPAY: '支', UNIONPAY: '银' } as Record<string, string>)[code] || '付'
}
</script>

<style scoped>
.pay-page { padding-bottom: 132px; }

/* 金额区 */
.pay-amount { text-align: center; padding: 24px 16px 18px; }
.lab { color: #999; font-size: 13px; word-break: break-all; }
.amt { margin: 8px 0 4px; color: #111; display: flex; align-items: baseline; justify-content: center; gap: 2px; }
.rmb { font-size: 20px; font-weight: 600; }
.money { font-size: 34px; font-weight: 700; line-height: 1.1; }
.countdown { color: #999; font-size: 12px; }
.countdown b { color: var(--primary); font-weight: 600; }

/* 渠道列表 */
.channel-list { padding: 2px 0; }
.channel {
  display: flex; align-items: center; gap: 12px;
  padding: 15px 16px; border-bottom: 1px solid #f4f4f4;
  font-size: 15px; min-height: 56px; box-sizing: border-box;
  transition: background .15s ease;
}
.channel:last-child { border-bottom: 0; }
.channel.active { background: #fff8f8; }
.ch-name { flex: 1; color: #222; }
.ch-icon {
  width: 34px; height: 34px; border-radius: 9px; flex-shrink: 0;
  background: #2593ff; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 16px;
}
.ch-icon.ch-wechat { background: #07c160; }
.ch-icon.ch-alipay { background: #1677ff; }
.ch-icon.ch-unionpay { background: #e4393c; }
.check {
  width: 20px; height: 20px; border-radius: 50%; flex-shrink: 0;
  border: 1px solid #ddd; color: transparent; font-size: 12px;
  display: flex; align-items: center; justify-content: center;
}
.check.on { border-color: var(--primary); background: var(--primary); color: #fff; }
.channel-empty { text-align: center; color: #bbb; font-size: 13px; padding: 22px 0; }

.multi-tip { text-align: center; color: #999; font-size: 12px; padding: 10px 16px 0; }

/* 底部操作区：固定并适配安全区 */
.pay-footer {
  position: fixed; left: 50%; bottom: 0; transform: translateX(-50%);
  width: 100%; max-width: 480px; box-sizing: border-box;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: #fff; box-shadow: 0 -4px 16px rgba(0, 0, 0, .06);
}
.pay-footer .btn-primary,
.pay-footer .btn-plain { padding: 13px; font-size: 16px; }
.pay-footer .btn-plain { margin-top: 8px; padding: 11px; font-size: 14px; }
.foot-tip { margin: 8px 0 0; text-align: center; color: #b5b5b5; font-size: 11px; }

/* 小屏适配 */
@media (max-width: 360px) {
  .money { font-size: 30px; }
  .channel { padding: 13px 12px; font-size: 14px; min-height: 52px; }
  .pay-footer { padding-left: 12px; padding-right: 12px; }
}
/* 横屏/大屏时保持内容居中不拉伸 */
@media (min-width: 481px) {
  .pay-page { min-height: 100vh; }
}
</style>
