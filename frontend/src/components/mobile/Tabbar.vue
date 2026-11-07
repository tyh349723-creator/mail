<template>
  <nav class="tabbar">
    <router-link to="/user/home" :class="{ active: active === 0 }"><span class="ic"><img src="@/assets/shouye.png" /></span>首页</router-link>
    <router-link to="/user/cart" :class="{ active: active === 1 }"><span class="ic"><img src="@/assets/a1.png" /></span>购物车<em v-if="cartCount">{{ cartCount }}</em></router-link>
    <router-link to="/user/orders" :class="{ active: active === 2 }"><span class="ic"><img src="@/assets/dingdang.png" /></span>订单</router-link>
    <router-link to="/user/profile" :class="{ active: active === 3 }"><span class="ic"><img src="@/assets/bug.png" /></span>我的</router-link>
  </nav>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useCartStore } from '@/store/cart'

defineProps({ active: { type: Number, default: 0 } })
const cart = useCartStore()
const cartCount = computed(() => cart.count)
onMounted(() => cart.load())
</script>

<style scoped>
.tabbar em {
  position: absolute; transform: translate(6px, -8px); background: var(--price);
  color: #fff; font-size: 10px; font-style: normal; border-radius: 8px; padding: 0 4px; min-width: 16px; line-height: 16px;
}
</style>