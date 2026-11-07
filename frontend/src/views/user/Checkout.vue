<template>
  <div class="page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">确认订单</span></div>

    <!-- 收货地址 -->
    <div class="card addr" @click="pickAddress">
      <template v-if="addr">
        <div>
          <div class="a1">{{ addr.consignee }} <em>{{ addr.phone }}</em></div>
          <div class="a2">{{ addr.full }}</div>
        </div>
        <span class="arrow">›</span>
      </template>
      <template v-else>
        <span class="add">＋ 请选择收货地址</span>
        <span class="arrow">›</span>
      </template>
    </div>

    <!-- 库存不足提示 -->
    <div v-if="stockShort" class="card stock-tip">部分商品库存不足，请调整购买数量后再提交订单</div>

    <!-- 商品/金额预览 -->
    <div class="card" v-for="g in groups" :key="g.merchantNo">
      <div class="mch-head"><span class="shop-name">🏪 {{ g.shopName }}</span><span class="gc">{{ g.count }} 件</span></div>
      <div v-for="it in g.items" :key="it.skuId" class="g-item">
        <img :src="it.image" />
        <div class="gi">
          <div>{{ it.title }} <span class="sp">{{ it.specInfo }}</span></div>
          <div class="row"><span class="money">¥{{ fmtMoney(it.price) }}</span><span>×{{ it.quantity }}</span></div>
        </div>
      </div>
      <div class="subtotal">小计：<span class="money">¥{{ fmtMoney(g.total) }}</span></div>
    </div>

    <!-- 支付方式（5.1.3：结算页选择渠道，收银台预选带入） -->
    <div class="card pay-card">
      <div class="label">选择支付方式</div>
      <div class="ch-list">
        <div
          v-for="c in channels"
          :key="c.wayCode"
          class="ch"
          :class="{ active: wayCode === c.wayCode }"
          @click="wayCode = c.wayCode"
        >
          <span class="ch-icon">{{ iconOf(c.wayCode) }}</span>
          <span class="ch-name">{{ c.channelName }}</span>
          <span v-if="wayCode === c.wayCode" class="ch-ok">✓</span>
        </div>
      </div>
    </div>

    <!-- 备注 -->
    <div class="card cell"><span class="label">订单备注</span><input v-model="remark" placeholder="选填，给商家留言" /></div>

    <div class="price-bar">
      <div>共 {{ totalCount }} 件，合计 <span class="money">¥{{ fmtMoney(total) }}</span></div>
      <button class="btn-primary" style="width: 130px; padding: 11px" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '提交订单' }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderPreview, orderCreate, addressList, payChannels } from '@/api'
import { fmtMoney } from '@/utils/const'
import { useCartStore } from '@/store/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const groups = ref<any[]>([])
const total = ref(0)
const totalCount = ref(0)
const addr = ref<any>(null)
const addrList = ref<any[]>([])
const remark = ref('')
const submitting = ref(false)
const channels = ref<any[]>([])
const wayCode = ref('')
const stockShort = ref(false)

const itemsParam = computed(() => {
  // 直接购买：从 query 带 skuId/quantity；购物车结算：取勾选项
  if (route.query.skuId) return [{ skuId: Number(route.query.skuId), quantity: Number(route.query.quantity || 1) }]
  return cart.server.filter((i) => i.checked && !i.invalid).map((i) => ({ skuId: i.skuId, quantity: i.quantity }))
})

onMounted(async () => {
  // 收货地址与支付方式独立加载，不因试算失败（如库存不足）而整页空白
  const [addrRes, chRes] = await Promise.all([
    addressList().catch(() => ({ list: [] })),
    payChannels().catch(() => ({ list: [] }))
  ])
  addrList.value = addrRes.list || []
  addr.value = addrList.value.find((a) => a.defaultFlag) || addrList.value[0] || null
  channels.value = chRes.list || []
  wayCode.value = route.query.payment || channels.value[0]?.wayCode || ''

  try {
    const prev = await orderPreview({ items: itemsParam.value })
    groups.value = prev.groups || []
    total.value = prev.total
    totalCount.value = prev.count
    stockShort.value = !!prev.stockShort
  } catch (e) {
    // 试算失败错误已由请求层统一提示，此处保留地址与支付方式供页面正常展示
  }
})

function iconOf(code: any) {
  return ({ WECHAT: '微', ALIPAY: '支', UNIONPAY: '银' } as Record<string, string>)[code] || '付'
}

async function pickAddress() {
  if (!addrList.value.length) {
    router.push('/user/address-edit')
    return
  }
  // 简版：交互使用 prompt 序号选择（真实项目应为底部弹层）
  const names = addrList.value.map((a, i) => `${i + 1}.${a.consignee} ${a.phone}`).join('\n')
  const pick = prompt('选择收货地址（输入序号）：\n' + names)
  const idx = Number(pick) - 1
  if (addrList.value[idx]) addr.value = addrList.value[idx]
}

async function submit() {
  if (!addr.value) { alert('请选择收货地址'); return }
  if (!itemsParam.value.length) { alert('没有可结算的商品'); return }
  submitting.value = true
  try {
    const res = await orderCreate({
      items: itemsParam.value,
      addressId: addr.value.id,
      remark: remark.value,
      cartIds: route.query.skuId ? undefined : cart.server.filter((i) => i.checked).map((i) => i.id)
    })
    const orderNo = (res.orderNos || [])[0]
    const merchantNo = (res.merchantNos || [])[0] || ''
    // 清理本地购物车游标
    if (!route.query.skuId) await cart.load()
    router.replace(
      '/user/pay/' + orderNo + '?money=' + res.money + '&merchantNo=' + merchantNo + '&timeout=' + res.timeoutSeconds +
      (wayCode.value ? '&payment=' + wayCode.value : '')
    )
  } catch (e: any) {
    alert(e.message || '下单失败')
    submitting.value = false
  }
}
</script>

<style scoped>
.addr { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; }
.a1 { font-size: 15px; font-weight: 600; }
.a1 em { font-style: normal; color: #999; font-weight: 400; margin-left: 8px; font-size: 13px; }
.a2 { color: #888; font-size: 13px; margin-top: 4px; }
.add { color: var(--primary); font-size: 14px; }
.stock-tip { color: #e4393c; font-size: 13px; padding: 12px 14px; }
.arrow { color: #ccc; }
.mch-head { display: flex; justify-content: space-between; padding: 12px 14px 4px; }
.shop-name { font-size: 14px; font-weight: 600; }
.gc { color: #999; font-size: 12px; }
.g-item { display: flex; gap: 10px; padding: 8px 14px; }
.g-item img { width: 60px; height: 60px; border-radius: 8px; }
.gi { flex: 1; font-size: 13px; color: #333; }
.sp { color: #999; font-size: 11px; }
.row { display: flex; justify-content: space-between; margin-top: 6px; }
.subtotal { text-align: right; padding: 6px 14px 12px; font-size: 13px; color: #333; }
.pay-card { padding: 12px 14px; }
.pay-card .label { font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.ch-list { display: flex; gap: 10px; }
.ch { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px 0; border: 1px solid #e8e8e8; border-radius: 10px; font-size: 13px; color: #666; }
.ch.active { border-color: var(--primary); color: var(--primary); background: #f5f8ff; }
.ch-icon { width: 24px; height: 24px; border-radius: 6px; background: #2593ff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; }
.ch:nth-child(2) .ch-icon { background: #1677ff; }
.ch:nth-child(3) .ch-icon { background: #e4393c; }
.ch-ok { color: var(--primary); font-weight: 700; }
.price-bar { position: fixed; left: 0; right: 0; bottom: 0; max-width: 480px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 10px 14px calc(10px + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #eee; }
</style>