<template>
  <div class="page">
    <div class="navbar">
      <span class="title">商品管理</span>
      <span class="right primary" @click="$router.push('/mch/product-edit')">＋发布</span>
    </div>

    <div class="tabs">
      <span v-for="t in tabs" :key="t.v" :class="{ active: status === t.v }" @click="switchTab(t.v)">{{ t.label }}</span>
    </div>

    <div v-for="p in list" :key="p.id" class="card prod">
      <img :src="p.mainImage" />
      <div class="pi">
        <div class="t">{{ p.title }}</div>
        <div class="info">库存 {{ p.stock }} · 销量 {{ p.sales }}<span class="st" :class="p.status === 1 ? 'on' : 'off'">{{ p.status === 1 ? '销售中' : '已下架' }}</span></div>
        <div class="money">¥{{ fmtMoney(p.price) }} <em>¥{{ fmtMoney(p.originalPrice) }}</em></div>
        <div class="acts">
          <template v-if="p.status === 1">
            <span @click="toggle(p, 0)">下架</span>
          </template>
          <template v-else>
            <span class="on" @click="toggle(p, 1)">上架</span>
          </template>
          <span @click="del(p)">删除</span>
          <span class="edit" @click="$router.push('/mch/product-edit/' + p.id)">编辑</span>
        </div>
      </div>
    </div>
    <div v-if="!list.length" class="empty">暂无商品</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mchProductList, mchProductStatus, mchProductDelete } from '@/api'
import { fmtMoney } from '@/utils/const'

const status = ref(-99)
const list = ref<any[]>([])
const tabs = [
  { v: -99, label: '全部' },
  { v: 1, label: '销售中' },
  { v: 0, label: '已下架' }
]

onMounted(load)
async function load() {
  const res = await mchProductList({ status: status.value, pageNum: 1, pageSize: 100 })
  list.value = res.records || []
}
function switchTab(v: any) {
  status.value = v
  load()
}
async function toggle(p: any, st: any) {
  await mchProductStatus({ id: p.id, status: st })
  load()
}
async function del(p: any) {
  if (!confirm('删除商品「' + p.title + '」？')) return
  await mchProductDelete({ id: p.id })
  load()
}
</script>

<style scoped>
.tabs { display: flex; background: #fff; padding: 10px 4px; }
.tabs span { flex: 1; text-align: center; font-size: 13px; color: #666; padding: 4px 0; }
.tabs span.active { color: var(--primary); font-weight: 600; border-bottom: 2px solid var(--primary); }
.prod { display: flex; gap: 10px; padding: 12px 14px; }
.prod img { width: 72px; height: 72px; border-radius: 10px; }
.pi { flex: 1; min-width: 0; }
.t { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.info { color: #999; font-size: 12px; margin-top: 4px; }
.st { margin-left: 6px; }
.st.on { color: #00b578; }
.st.off { color: #999; }
.money { margin-top: 4px; }
.money em { color: #bbb; font-size: 11px; font-style: normal; text-decoration: line-through; margin-left: 6px; }
.acts { margin-top: 6px; display: flex; gap: 12px; font-size: 12px; color: #666; }
.acts .on { color: #00b578; }
.acts .edit { color: var(--primary); }
</style>