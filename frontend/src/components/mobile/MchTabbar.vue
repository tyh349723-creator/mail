<template>
  <nav class="tabbar">
    <router-link v-for="(t, i) in tabs" :key="t.path" :to="t.path" :class="{ active: active === i }">
      <img :src="t.icon" class="tab-icon" />
      <span class="tab-text">{{ t.label }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
// 导入图标
import shouyeIcon from '@/assets/shouye.png'
import dianpuIcon from '@/assets/dianpu.png'
import orderIcon from '@/assets/order.png'
import jingyingIcon from '@/assets/jingying.png'
import profileIcon from '@/assets/profile.png'

const route = useRoute()
/** 5 个 Tab 主页面 */
const tabs = [
  { path: '/mch/profile', icon: shouyeIcon, label: '工作台' },
  { path: '/mch/products', icon: dianpuIcon, label: '商品' },
  { path: '/mch/orders', icon: orderIcon, label: '订单' },
  { path: '/mch/stats', icon: jingyingIcon, label: '统计' },
  { path: '/mch/mine', icon: profileIcon, label: '我的' }
]
const active = computed(() => {
  const i = tabs.findIndex((t) => t.path === route.path)
  return i === -1 ? 0 : i
})
</script>

<style scoped>
.tabbar {
  display: flex;
  height: 50px;
  border-top: 1px solid #eee;
  background: #fff;
}
.tabbar a {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  color: #666;
}
.tabbar a.active {
  color: #409eff;
}
.tab-icon {
  width: 24px;
  height: 24px;
  margin-bottom: 4px;
}
.tab-text {
  font-size: 12px;
}
</style>
