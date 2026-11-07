<template>
  <div class="page">
    <!-- 搜索栏 -->
    <div class="search-bar" @click="$router.push('/user/search')">
      <div class="search-input"><el-icon style="transform: translateY(3px);"><Search /></el-icon> 搜索商品</div>
      <span class="mode-btn" @click.stop="$router.push({ path: '/', query: { role: 'mch' } })">商家</span>
    </div>

    <!-- 轮播 -->
    <div
      v-if="home.banners?.length"
      class="banner"
      @mouseenter="stopAuto"
      @mouseleave="startAuto"
    >
      <div class="banner-track" :style="{ transform: `translateX(-${activeIndex * 100}%)` }">
        <div v-for="b in home.banners" :key="b.id" class="banner-item" @click="goLink(b.linkUrl)">
          <img :src="b.imageUrl" :alt="b.title" />
          <span class="banner-title">{{ b.title }}</span>
        </div>
      </div>
      <template v-if="home.banners.length > 1">
        <span class="banner-arrow left" @click.stop="prev">
          <el-icon><ArrowLeft /></el-icon>
        </span>
        <span class="banner-arrow right" @click.stop="next">
          <el-icon><ArrowRight /></el-icon>
        </span>
      </template>
      <div v-if="home.banners.length > 1" class="banner-dots">
        <span
          v-for="(b, i) in home.banners"
          :key="b.id"
          class="dot"
          :class="{ active: i === activeIndex }"
          @click.stop="goTo(i)"
        />
      </div>
    </div>

    <!-- 分类金刚区 -->
    <div class="cat-grid">
      <div v-for="c in home.categories" :key="c.id" class="cat-item" @click="$router.push({ path: '/user/search', query: { categoryId: c.id } })">
        <img :src="c.icon" class="cat-icon" alt="" />
        <span>{{ c.name }}</span>
      </div>
    </div>

    <!-- 推荐位 -->
    <div class="section-title">🔥 精品推荐</div>
    <div class="product-grid">
      <DefaultCard v-for="p in home.recommend" :key="p.id" :p="p" />
    </div>

    <!-- 热卖 TOP -->
    <div class="section-title">🏆 热卖榜单</div>
    <div class="hot-list">
      <div v-for="(p, i) in hotTop" :key="p.id" class="hot-item" @click="$router.push('/user/product/' + p.id)">
        <span class="hot-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
        <img :src="p.mainImage" />
        <div class="info">
          <div class="title">{{ p.title }}</div>
          <div class="price-line"><span class="money">¥{{ fmtMoney(p.price) }}</span><span class="sales">已售 {{ p.sales }}</span></div>
        </div>
      </div>
    </div>

    <!-- 猜你喜欢 -->
    <div class="section-title">💡 猜你喜欢</div>
    <div class="product-grid">
      <DefaultCard v-for="p in home.guessYouLike" :key="p.id" :p="p" />
    </div>
    <Tabbar :active="0" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { productHome } from '@/api'
import { fmtMoney } from '@/utils/const'
import Tabbar from '@/components/mobile/Tabbar.vue'
import DefaultCard from '@/components/mobile/ProductCard.vue'
// 首页金刚区用到的分类图标（从 src/assets 引入）
import foodIcon from '@/assets/category-food.png'
import fruitIcon from '@/assets/category-fruit.png'
import phoneIcon from '@/assets/category-phone.png'
import menIcon from '@/assets/category-men.png'
import applianceIcon from '@/assets/category-appliance.png'

const router = useRouter()
const home = ref<any>({})
const hotTop = computed(() => (home.value.hot || []).slice(0, 5))

// 轮播控制：每 3 秒自动换页
const activeIndex = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

function stopAuto() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function startAuto() {
  stopAuto()
  const len = home.value.banners?.length || 0
  if (len < 2) return
  timer = setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % len
  }, 3000)
}

function goTo(i: number) {
  activeIndex.value = i
  startAuto()
}

function prev() {
  const len = home.value.banners?.length || 0
  if (len < 2) return
  activeIndex.value = (activeIndex.value - 1 + len) % len
  startAuto()
}

function next() {
  const len = home.value.banners?.length || 0
  if (len < 2) return
  activeIndex.value = (activeIndex.value + 1) % len
  startAuto()
}

// 把首页这些分类的 emoji 图标替换为前端图片
const ICON_MAP: Record<string, string> = { '食品生鲜': foodIcon, '水果蔬菜': fruitIcon, '手机数码': phoneIcon, '男装': menIcon, '家用电器': applianceIcon }

onMounted(async () => {
  try {
    const res = await productHome()
    if (res.categories) {
      res.categories.forEach((c: any) => { if (ICON_MAP[c.name]) c.icon = ICON_MAP[c.name] })
    }
    home.value = res
    activeIndex.value = 0
    startAuto()
  } catch (e) {
    /* 服务未启动时静默 */
  }
})

onUnmounted(stopAuto)

function goLink(link: any) {
  if (!link || link === 'home') return
  if (link.startsWith('product:')) router.push('/user/product/' + link.split(':')[1])
  else if (link.startsWith('category:')) router.push('/user/search?categoryId=' + link.split(':')[1])
}
</script>

<style scoped lang="css">
.search-bar { display: flex; align-items: center; gap: 10px; padding: 12px 14px; background: #fff; position: sticky; top: 0; z-index: 20; }
.search-input { flex: 1; background: #f2f3f5; border-radius: 18px; padding: 9px 16px; color: #999; font-size: 13px; }
.mode-btn { color: var(--primary); font-size: 13px; }
.banner { position: relative; background: #fff; overflow: hidden; }
.banner-track { display: flex; transition: transform .4s ease; }
.banner-item { flex: 0 0 100%; position: relative; aspect-ratio: 5 / 2; }
.banner-item img { width: 100%; height: 100%; display: block; object-fit: contain; }
.banner-title { position: absolute; left: 12px; bottom: 12px; color: #fff; font-size: 15px; font-weight: 600; text-shadow: 0 1px 2px rgba(0,0,0,.3); }
.banner-arrow { position: absolute; top: 50%; transform: translateY(-50%); width: 28px; height: 28px; border-radius: 50%; background: rgba(0,0,0,.3); color: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: background .2s; z-index: 2; }
.banner-arrow:hover { background: rgba(0,0,0,.5); }
.banner-arrow.left { left: 8px; }
.banner-arrow.right { right: 8px; }
.banner-dots { position: absolute; right: 12px; bottom: 12px; display: flex; align-items: center; gap: 6px; }
.banner-dots .dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,.55); transition: width .2s, background .2s; }
.banner-dots .dot.active { width: 16px; border-radius: 3px; background: #fff; }
.cat-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; background: #fff; padding: 14px 8px; margin: 10px; border-radius: 12px; }
.cat-item { text-align: center; font-size: 12px; color: #333; }
.cat-icon { display: block; width: 44px; height: 44px; margin: 0 auto 4px; object-fit: contain; }
.product-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 0 10px; }
.hot-list { background: #fff; margin: 0 10px 10px; border-radius: 12px; overflow: hidden; }
.hot-item { display: flex; gap: 10px; padding: 10px 12px; align-items: center; }
.hot-item img { width: 56px; height: 56px; border-radius: 8px; }
.hot-no { width: 18px; color: #999; font-weight: 700; font-size: 14px; text-align: center; }
.hot-no.top { color: #ff3e3e; }
.info { flex: 1; min-width: 0; }
.title { font-size: 13px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.price-line { display: flex; justify-content: space-between; margin-top: 6px; align-items: baseline; }
.sales { color: #bbb; font-size: 11px; }
</style>