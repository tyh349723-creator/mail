<template>
  <div class="p-card" @click="$router.push('/user/product/' + p.id)">
    <img :src="p.mainImage" :alt="p.title" />
    <div class="p-title">{{ p.title }}</div>
    <div class="p-sub">{{ p.subTitle }}</div>
    <div class="p-line">
      <span class="money">¥{{ fmtMoney(p.price) }}</span>
      <span class="sales">已售{{ p.sales }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { fmtMoney } from '@/utils/const'
defineProps({ p: { type: Object, required: true } })
</script>

<style scoped>
.p-card { background: #fff; border-radius: 10px; overflow: hidden; }
.p-card img { width: 100%; height: 160px; object-fit: cover; }
.p-title { font-size: 13px; color: #333; padding: 8px 8px 2px; line-height: 1.3; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.p-sub { font-size: 11px; color: #999; padding: 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.p-line { display: flex; justify-content: space-between; align-items: center; padding: 6px 8px 10px; }
.sales { color: #bbb; font-size: 11px; }
</style>