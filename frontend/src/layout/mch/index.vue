<template>
  <div class="mch-layout">
    <!-- 移动端「三明治」：内容区 + 底部 5 Tab（仅 Tab 主页面显示） -->
    <div class="content" :class="{ 'has-tab': showTab }">
      <router-view v-slot="{ Component }">
        <transition name="fade-transform" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <MchTabbar v-if="showTab" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MchTabbar from '@/components/mobile/MchTabbar.vue'

const route = useRoute()

/** 底部 Tab 覆盖的主页面；子页（发布商品/钱包/店铺/入驻等）走 navbar 返回，不显示 Tab */
const TAB_PATHS = ['/mch/profile', '/mch/products', '/mch/orders', '/mch/stats', '/mch/mine']
const showTab = computed(() => TAB_PATHS.includes(route.path))
</script>

<style scoped>
.mch-layout {
  min-height: 100vh;
  background: var(--bg);
}

.content {
  /* 由各页面的 .page（max-width 480px 居中）接管宽度，随手机屏适配 */
}

/* 有底部 Tab 时给内容区留出 Tabbar 高度的占位，避免列表底被遮 */
.has-tab {
  padding-bottom: calc(56px + env(safe-area-inset-bottom));
}

/* 切页过渡：只用纵向位移 + 淡入，避免 translateX 在移动端造成横向溢出闪动 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.25s ease;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>