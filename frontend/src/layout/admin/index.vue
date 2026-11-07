<template>
  <div class="layout-container">
    <Sidebar :collapse="isCollapse" />
    <div class="layout-main" :class="{ collapsed: isCollapse }">
      <Navbar @toggle="isCollapse = !isCollapse" />
      <TagsView />
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'

const isCollapse = ref(false)
const cachedViews = ref<string[]>([])
</script>

<style scoped lang="scss">
.layout-container {
  display: flex;
  height: 100%;
  width: 100%;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-left: var(--sidebar-width);
  transition: margin-left 0.28s;

  &.collapsed {
    margin-left: 64px;
  }
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--app-bg);
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
