<template>
  <div class="tags-view">
    <el-scrollbar>
      <div class="tags-wrapper">
        <router-link
          v-for="tag in visitedTags"
          :key="tag.path"
          :to="tag.path"
          class="tag-item"
          :class="{ active: isActive(tag.path) }"
        >
          <span>{{ tag.title }}</span>
          <el-icon
            v-if="tag.path !== '/dashboard'"
            class="tag-close"
            @click.prevent.stop="closeTag(tag)"
          >
            <Close />
          </el-icon>
        </router-link>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

interface TagView {
  path: string
  title: string
}

const route = useRoute()
const visitedTags = ref<TagView[]>([{ path: '/dashboard', title: '数据看板' }])

watch(
  () => route.path,
  (path) => {
    if (route.meta?.title && !visitedTags.value.some((t) => t.path === path)) {
      visitedTags.value.push({ path, title: route.meta.title as string })
    }
  },
  { immediate: true }
)

function isActive(path: string): boolean {
  return route.path === path
}

function closeTag(tag: TagView) {
  const index = visitedTags.value.findIndex((t) => t.path === tag.path)
  if (index > -1) {
    visitedTags.value.splice(index, 1)
  }
}
</script>

<style scoped lang="scss">
.tags-view {
  height: 36px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
}

.tags-wrapper {
  display: flex;
  align-items: center;
  padding: 0 8px;
  gap: 6px;
  height: 36px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  line-height: 26px;
  padding: 0 10px;
  font-size: 12px;
  border: 1px solid #d8dce5;
  border-radius: 3px;
  color: #495060;
  background: #fff;
  cursor: pointer;
  white-space: nowrap;

  &.active {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
  }

  .tag-close {
    font-size: 12px;
    border-radius: 50%;

    &:hover {
      background: rgba(0, 0, 0, 0.15);
    }
  }
}
</style>
