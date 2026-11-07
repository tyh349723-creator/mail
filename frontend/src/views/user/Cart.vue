<template>
  <div class="page">
    <div class="navbar"><span class="title">购物车</span></div>

    <!-- 游客本地购物车 -->
    <div v-if="!user.isLogin && localItems.length" class="card">
      <div class="section-title">本地购物车（登录后自动合并）</div>
      <div v-for="it in localItems" :key="it.skuId" class="li">
        <span>{{ it.title }} <em v-if="it.spec">· {{ it.spec }}</em></span>
        <div>
          <span class="money">¥{{ fmtMoney(it.price) }}</span>
          <span class="qty">×{{ it.quantity }}</span>
          <button class="del" @click="cart.localRemove(it.skuId)">✕</button>
        </div>
      </div>
      <div class="login-tip" @click="$router.push('/')">去登录，与账号购物车合并 ›</div>
    </div>

    <div v-if="user.isLogin">
      <template v-if="items.length">
        <div class="card" v-for="it in items" :key="it.id">
          <div class="item">
            <span class="check" :class="{ on: it.checked }" @click="toggle(it)">✓</span>
            <img :src="it.image" @click="$router.push('/user/product/' + it.productId)" />
            <div class="info">
              <div class="ti">{{ it.title }}</div>
              <div class="sp">{{ it.specInfo }}</div>
              <div class="row">
                <span class="money">¥{{ fmtMoney(it.price) }}</span>
                <div class="stepper">
                  <button @click="change(it, -1)">−</button>
                  <span>{{ it.quantity }}</span>
                  <button @click="change(it, 1)">＋</button>
                </div>
              </div>
            </div>
          </div>
          <div v-if="it.invalid" class="invalid-tip">商品已下架</div>
        </div>
      </template>
      <div v-else class="empty">购物车是空的，去逛逛吧 🛍️</div>
    </div>
    <div v-else-if="!localItems.length" class="empty" style="padding-top: 120px">
      购物车是空的<br /><button class="btn-primary" style="margin: 16px auto 0; width: 180px" @click="$router.push('/user/home')">去逛逛</button>
    </div>

    <!-- 结算栏 -->
    <div v-if="user.isLogin && items.length" class="settle-bar">
      <span class="check-all" :class="{ on: allChecked }" @click="toggleAll">✓ 全选</span>
      <span class="clear" @click="clear">清空</span>
      <div class="settle-info">
        <div>合计：<span class="money">¥{{ fmtMoney(total) }}</span></div>
        <button class="btn-primary" style="width: 120px; padding: 10px" :disabled="!checkedItems.length" @click="checkout">结算({{ checkedCount }})</button>
      </div>
    </div>
    <Tabbar :active="1" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { cartUpdate, cartRemove, cartClear } from '@/api'
import { fmtMoney } from '@/utils/const'
import Tabbar from '@/components/mobile/Tabbar.vue'

const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const items = computed(() => cart.server)
const localItems = computed(() => {
  // 与商品快照无关，最简展示
  try {
    return JSON.parse(localStorage.getItem('mall_cart:local') || '[]')
  } catch (e) { return [] }
})
const checkedItems = computed(() => items.value.filter((i) => i.checked))
const checkedCount = computed(() => checkedItems.value.reduce((s, i) => s + i.quantity, 0))
const total = computed(() => checkedItems.value.reduce((s, i) => s + Number(i.price) * i.quantity, 0))
const allChecked = computed(() => items.value.length > 0 && items.value.every((i) => i.checked))

onMounted(() => cart.load())
watch(() => user.isLogin, (v) => { if (v) cart.syncAfterLogin() })

async function toggle(it: any) {
  await cartUpdate({ id: it.id, checked: !it.checked })
  it.checked = !it.checked
  cart.computeCount()
}
function toggleAll() {
  items.value.forEach((i) => (i.checked = !allChecked.value))
  cart.computeCount()
}
async function change(it: any, delta: any) {
  const q = it.quantity + delta
  if (q <= 0) return remove(it)
  // 增加数量时校验该 SKU 当前库存，达到上限后继续点击给出提示
  if (delta > 0 && it.availableStock != null && q > Number(it.availableStock)) {
    ElMessage.error(`商品「${it.title}」库存不足`)
    return
  }
  await cartUpdate({ id: it.id, quantity: q })
  it.quantity = q
  cart.computeCount()
}
async function remove(it: any) {
  await cartRemove({ id: it.id })
  cart.server = cart.server.filter((x: any) => x.id !== it.id)
  cart.computeCount()
}
function checkout() {
  router.push({ path: '/user/checkout', query: { fromCart: 1 } })
}

/** 清空购物车（5.1.2：无遗漏的购物车管理） */
async function clear() {
  if (!confirm('确认清空购物车？此操作不可撤销')) return
  await cartClear()
  cart.server = []
  cart.computeCount()
}
</script>

<style scoped>
.li, .item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; }
.li .qty { color: #999; margin-left: 8px; }
.del { color: #bbb; margin-left: 8px; }
.login-tip { padding: 10px 14px; color: var(--primary); font-size: 13px; border-top: 1px solid #f2f2f2; }
.item img { width: 70px; height: 70px; border-radius: 8px; }
.check { width: 20px; height: 20px; border-radius: 50%; border: 1px solid #ddd; color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; flex: none; }
.check.on { background: var(--primary); border-color: var(--primary); }
.info { flex: 1; min-width: 0; }
.ti { font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sp { color: #999; font-size: 11px; margin: 2px 0 8px; }
.row { display: flex; justify-content: space-between; align-items: center; }
.stepper { display: flex; align-items: center; gap: 8px; }
.stepper button { width: 22px; height: 22px; border-radius: 50%; background: #f2f3f5; }
.invalid-tip { color: #999; font-size: 12px; padding: 0 14px 10px 44px; }
.settle-bar { position: fixed; left: 0; right: 0; bottom: 52px; max-width: 480px; margin: 0 auto; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; border-top: 1px solid #eee; }
.check-all { font-size: 13px; color: #666; }
.check-all.on { color: var(--primary); }
.clear { font-size: 13px; color: #999; margin-left: 14px; }
.settle-info { display: flex; align-items: center; gap: 12px; }
</style>