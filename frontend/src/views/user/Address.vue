<template>
  <div class="page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">收货地址</span><span class="right" @click="$router.push('/user/address-edit')">＋新增</span></div>

    <div v-for="a in list" :key="a.id" class="card addr">
      <div class="a1">{{ a.consignee }} <em>{{ a.phone }}</em>
        <span v-if="a.defaultFlag" class="tag">默认</span>
      </div>
      <div class="a2">{{ a.full }}</div>
      <div class="a-actions">
        <span @click="remove(a)">删除</span>
        <span @click="edit(a)">编辑 ›</span>
      </div>
    </div>
    <div v-if="!list.length" class="empty">暂无收货地址</div>

    <div class="btn-wrap"><button class="btn-primary" @click="$router.push('/user/address-edit')">新增地址</button></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { addressList, addressDelete } from '@/api'

const list = ref<any[]>([])
const router = useRouter()
onMounted(load)
async function load() {
  const res = await addressList()
  list.value = res.list || []
}
function edit(a: any) {
  localStorage.setItem('mall_addr_edit', JSON.stringify(a))
  const { id, ...rest } = a
  localStorage.setItem('mall_addr_edit', JSON.stringify(rest))
  router.push('/user/address-edit/' + a.id)
}
async function remove(a: any) {
  if (!confirm('删除该地址？')) return
  await addressDelete({ id: a.id })
  load()
}
</script>

<style scoped>
.addr { padding: 14px 16px; }
.a1 { font-size: 15px; font-weight: 600; }
.a1 em { color: #999; font-weight: 400; margin-left: 8px; }
.a2 { color: #888; font-size: 13px; margin: 4px 0 8px; }
.tag { background: var(--primary); color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 6px; margin-left: 8px; font-weight: 400; }
.a-actions { display: flex; justify-content: flex-end; gap: 18px; color: #666; font-size: 13px; }
</style>