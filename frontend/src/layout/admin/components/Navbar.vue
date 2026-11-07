<template>
  <div class="admin-navbar">
    <div class="admin-navbar-left">
      <el-icon class="collapse-btn" @click="$emit('toggle')">
        <Fold v-if="!isCollapsed" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="admin-navbar-right">
      <el-tooltip content="刷新" placement="bottom">
        <el-icon class="action-icon" @click="refresh"><Refresh /></el-icon>
      </el-tooltip>
      <el-tooltip content="全屏" placement="bottom">
        <el-icon class="action-icon" @click="toggleFullscreen"><FullScreen /></el-icon>
      </el-tooltip>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="userStore.adminInfo?.avatar">
            {{ (userStore.adminInfo?.nickname || userStore.adminInfo?.username || '管').charAt(0) }}
          </el-avatar>
          <span class="username">{{ userStore.adminInfo?.nickname || userStore.adminInfo?.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { resetRouter } from '@/router/permission'

const props = defineProps<{ collapse?: boolean }>()
defineEmits<{ (e: 'toggle'): void }>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const isCollapsed = computed(() => props.collapse)

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((m) => m.meta?.title)
  return matched.map((m) => m.meta.title as string)
})

function refresh() {
  // 重新加载当前页面数据
  window.location.reload()
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    // 清权限态 + 移除动态路由，避免下一账号登录残留上一账号的全量菜单
    permissionStore.reset()
    resetRouter(router)
    router.push('/')
  } else if (command === 'profile') {
    router.push('/admin/perm/user')
  }
}
</script>

<style scoped lang="scss">
.admin-navbar {
  height: var(--navbar-height);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 1000;
}

.admin-navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #5a5e66;

  &:hover {
    color: #409eff;
  }
}

.admin-navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.action-icon {
  font-size: 18px;
  cursor: pointer;
  color: #5a5e66;

  &:hover {
    color: #409eff;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;

  .username {
    font-size: 14px;
    color: #303133;
  }
}
</style>
