<template>
  <div class="sidebar" :class="{ collapse }">
    <div class="logo">
      <img src="/favicon.svg" alt="logo" class="logo-img" />
      <span v-if="!collapse" class="logo-title">Mall-X 管理后台</span>
    </div>

    <el-scrollbar class="sidebar-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :collapse-transition="false"
        router
        unique-opened
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <!-- 菜单纯路由驱动：单页（如 数据看板/订单查询/退款监管）与分组按路由注册顺序交错排列 -->
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
            <template #title>
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-else :index="item.path">
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

defineProps<{ collapse: boolean }>()

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
}

/**
 * 菜单纯路由驱动：/admin 命名空间下「有 title 且未隐藏」的路由，
 * 存在可见子路由 → 分组菜单；否则（含单页动态路由：订单查询/退款监管）→ 单页菜单。
 * 分组的孩子、隐藏重定向等一律不重复渲染；顺序 = 路由注册顺序。
 */
const menuItems = computed<MenuItem[]>(() => {
  const adminRoutes = router
    .getRoutes()
    .filter((r) => r.path.startsWith('/admin') && r.meta?.title && !r.meta?.hidden)

  const visibleChildren = (r: any) =>
    (r.children || []).filter((c: any) => c.meta?.title && !c.meta?.hidden)

  const isGroupOf = (r: any) =>
    adminRoutes.some((p) => p.path !== r.path && visibleChildren(p).length > 0 && r.path.startsWith(p.path + '/'))

  const ordered: MenuItem[] = []
  adminRoutes.forEach((r) => {
    const children = visibleChildren(r)
    if (children.length > 0) {
      ordered.push({
        path: r.path,
        title: r.meta?.title as string,
        icon: r.meta?.icon as string,
        children: children.map((c: any) => ({
          path: c.path.startsWith('/') ? c.path : `${r.path.replace(/\/$/, '')}/${c.path}`,
          title: c.meta?.title as string,
          icon: c.meta?.icon as string
        }))
      })
    } else if (!isGroupOf(r)) {
      ordered.push({ path: r.path, title: r.meta?.title as string, icon: r.meta?.icon as string })
    }
  })
  return ordered
})
</script>

<style scoped lang="scss">
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--sidebar-bg);
  transition: width 0.28s;
  z-index: 1001;
  display: flex;
  flex-direction: column;

  &.collapse {
    width: 64px;
  }
}

.logo {
  height: var(--navbar-height);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 12px;
  overflow: hidden;
  flex-shrink: 0;

  .logo-img {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
  }
  .logo-title {
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.sidebar-scroll {
  flex: 1;
}

:deep(.el-menu) {
  border-right: none;
}
:deep(.el-menu-item.is-active) {
  background-color: #1890ff !important;
  color: #fff !important;
}
</style>
