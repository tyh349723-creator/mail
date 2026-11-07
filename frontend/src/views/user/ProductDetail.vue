<template>
  <div class="page" v-if="product.productId">
    <!-- 顶部导航：返回上一页（无历史时兜底回首页） -->
    <div class="navbar"><span class="back" @click="goBack">‹</span><span class="title">商品详情</span></div>

    <!-- 主图 -->
    <div class="hero"><img :src="product.mainImage" /></div>
    <div class="card info-card">
      <div class="price-line">
        <span class="money big">¥{{ fmtMoney(product.price) }}</span>
        <span class="orig">¥{{ fmtMoney(product.originalPrice) }}</span>
        <span class="sales">已售 {{ product.sales }}</span>
      </div>
      <div class="title">{{ product.title }}</div>
      <div class="sub">{{ product.subTitle }}</div>
    </div>
    <!-- 店铺 -->
    <div class="card shop-row" @click="shop">
      <div>
        <div class="shop-name">{{ product.shopName }}</div>
        <div class="shop-no">商家编号 {{ product.merchantNo }}</div>
      </div>
      <span class="arrow">进店 ›</span>
    </div>
    <!-- 规格选择 -->
    <div class="card">
      <div class="section-title">选择规格</div>
      <div class="sku-list">
        <div v-for="s in product.skus" :key="s.id" class="sku-item" :class="{ active: skuId === s.id }" @click="selectSku(s.id)">
          {{ s.specValue }}
        </div>
      </div>
      <div class="qty-row">
        <span>数量</span>
        <div class="stepper">
          <button @click="qty > 1 && qty--">−</button>
          <input v-model.number="qty" type="number" min="1" :max="availableStock" />
          <button @click="incQty">＋</button>
        </div>
      </div>
    </div>
    <!-- 图文详情 -->
    <div class="card"><div class="section-title">商品详情</div><div class="detail" v-html="product.detail"></div></div>

    <!-- 底部操作 -->
    <div class="action-bar">
      <button class="abtn" :disabled="!skuId" @click="addCart">🛒 加入购物车</button>
      <button class="abtn buy" :disabled="!skuId" @click="buyNow">立即购买</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { productDetail } from '@/api'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { fmtMoney } from '@/utils/const'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const product = ref<any>({})
const skuId = ref<any>(null)
const qty = ref(1)

const availableStock = computed(() => {
  const s = (product.value.skus || []).find((x: any) => x.id === skuId.value)
  return s ? s.stock : 999
})

onMounted(async () => {
  product.value = await productDetail(String(route.params.id))
  if (product.value.skus?.length) skuId.value = product.value.skus[0].id
})

function selectSku(id: any) {
  skuId.value = id
}

function incQty() {
  if (qty.value >= availableStock.value) {
    ElMessage.error(`商品「${product.value.title}」库存不足`)
    return
  }
  qty.value++
}

/**
 * 安全回退：正常导航 → 返回上一页；
 * 直接输入 URL / 刷新详情页（无历史记录）→ 兜底回首页。
 */
function goBack() {
  const h = window.history.state as { back?: unknown } | null
  if (h?.back != null) router.back()
  else router.push('/user/home')
}

async function addCart() {
  if (!user.isLogin) {
    // 游客：离线本地购物车
    cart.localAdd(skuId.value, qty.value)
    alert('已加入本地购物车（登录后自动合并）')
    return
  }
  const { cartAdd } = await import('@/api')
  const res = await cartAdd({ skuId: skuId.value, quantity: qty.value })
  cart.setCount(res.cartCount)
  alert('已加入购物车')
}

async function buyNow() {
  if (!user.isLogin) {
    const ok = confirm('请先登录，是否前往登录？')
    if (ok) router.push({ path: '/', query: { role: 'user' } })
    return
  }
  // 直接下单：items = [{skuId, quantity}]
  router.push({ path: '/user/checkout', query: { skuId: skuId.value, quantity: qty.value } })
}

function shop() {
  router.push({ path: '/user/search', query: { merchantNo: product.value.merchantNo } })
}
</script>

<style scoped>
.hero img { width: 100%; height: 260px; object-fit: cover; }
.info-card { padding: 12px 14px; }
.price-line { display: flex; align-items: baseline; gap: 8px; }
.money.big { font-size: 24px; }
.orig { color: #bbb; text-decoration: line-through; font-size: 12px; }
.sales { margin-left: auto; color: #999; font-size: 12px; }
.title { font-size: 16px; font-weight: 600; color: #222; margin-top: 8px; line-height: 1.35; }
.sub { color: #999; font-size: 12px; margin-top: 4px; }
.shop-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; }
.shop-name { font-size: 14px; font-weight: 600; }
.shop-no { color: #999; font-size: 12px; margin-top: 2px; }
.arrow { color: #ccc; font-size: 13px; }
.sku-list { display: flex; flex-wrap: wrap; gap: 10px; padding: 0 14px 8px; }
.sku-item { padding: 8px 14px; border: 1px solid #e5e5e5; border-radius: 16px; font-size: 13px; color: #666; }
.sku-item.active { border-color: var(--primary); color: var(--primary); background: #fff5f5; }
.qty-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px 16px; font-size: 14px; }
.stepper { display: flex; align-items: center; gap: 8px; }
.stepper button { width: 26px; height: 26px; border-radius: 50%; background: #f2f3f5; font-size: 15px; }
.stepper input { width: 40px; text-align: center; border: 1px solid #eee; border-radius: 6px; height: 26px; }
.detail { padding: 0 14px 14px; color: #555; font-size: 13px; }
.action-bar { position: fixed; left: 0; right: 0; bottom: 0; max-width: 480px; margin: 0 auto; display: flex; gap: 10px; padding: 10px 14px calc(10px + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #eee; }
.abtn { flex: 1; padding: 12px; border-radius: 22px; background: #fff; border: 1px solid var(--primary); color: var(--primary); font-size: 14px; }
.abtn:disabled { opacity: .5; }
.abtn.buy { background: var(--primary); color: #fff; border: none; }
</style>