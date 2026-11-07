<template>
  <div class="page">
    <div class="navbar">
      <span class="back" @click="$router.back()">‹</span>
      <input v-model="keyword" class="search-input" placeholder="搜索商品" @confirm="search" @keyup.enter="search" />
      <span class="search-btn" @click="search">搜索</span>
    </div>
    <!-- 排序 -->
    <div class="sort-bar">
      <span v-for="s in sorts" :key="s.key" :class="{ active: sort === s.key }" @click="changeSort(s.key)">{{ s.label }}</span>
    </div>
    <!-- 商品 -->
    <div class="product-grid">
      <ProductCard v-for="p in list" :key="p.id" :p="p" />
    </div>
    <div v-if="!list.length" class="empty">没有找到相关商品</div>
    <div class="load-more" @click="loadMore">加载更多</div>
    <Tabbar :active="-1" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { productList } from '@/api'
import ProductCard from '@/components/mobile/ProductCard.vue'
import Tabbar from '@/components/mobile/Tabbar.vue'

const route = useRoute()
const keyword = ref(route.query.keyword || '')
const categoryId = ref(Number(route.query.categoryId || 0))
const sort = ref('default')
const list = ref<any[]>([])
const page = ref(1)
const size = 10
const total = ref(0)
const sorts = [
  { key: 'default', label: '综合' },
  { key: 'sales', label: '销量' },
  { key: 'price_asc', label: '价格↑' },
  { key: 'price_desc', label: '价格↓' },
  { key: 'new', label: '新品' }
]

onMounted(load)

async function load() {
  page.value = 1
  const res = await productList({ keyword: keyword.value, categoryId: categoryId.value, sort: sort.value, pageNum: page.value, pageSize: size })
  list.value = res.records || []
  total.value = res.total || 0
}
function search() {
  load()
}
function changeSort(k: any) {
  sort.value = k
  load()
}
async function loadMore() {
  if (list.value.length >= total.value) return
  page.value++
  const res = await productList({ keyword: keyword.value, categoryId: categoryId.value, sort: sort.value, pageNum: page.value, pageSize: size })
  list.value = list.value.concat(res.records || [])
}
</script>

<style scoped>
.search-bar { background: #fff; }
.navbar input { flex: 1; }
.search-input { border: none; background: #f2f3f5; border-radius: 18px; padding: 9px 16px; font-size: 13px; }
.search-btn { color: var(--primary); font-size: 14px; }
.sort-bar { display: flex; background: #fff; padding: 10px 0; }
.sort-bar span { flex: 1; text-align: center; font-size: 13px; color: #666; }
.sort-bar span.active { color: var(--primary); font-weight: 600; }
.product-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 10px; }
.load-more { text-align: center; color: #999; font-size: 12px; padding: 14px; }
</style>