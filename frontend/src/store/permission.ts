import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUser } from '@/utils/auth'

export const usePermissionStore = defineStore('permission', () => {
  const roles = ref<string[]>(getUser()?.roles || [])
  const permissions = ref<string[]>(getUser()?.permissions || [])
  /** 管理端动态路由是否已挂载（守卫用）；登录/刷新后需按当前账号权限挂载一次 */
  const routesLoaded = ref(false)

  function setRoles(r: string[]) {
    roles.value = r
  }

  function setPermissions(p: string[]) {
    permissions.value = p
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function hasPermission(perm: string): boolean {
    // 超级管理员拥有全部权限
    if (roles.value.includes('SUPER_ADMIN')) return true
    return permissions.value.includes(perm)
  }

  function reset() {
    roles.value = []
    permissions.value = []
    routesLoaded.value = false
  }

  return { roles, permissions, routesLoaded, setRoles, setPermissions, hasRole, hasPermission, reset }
})
