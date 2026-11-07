import type { Router, RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { useUserStore, roleHome, type Role } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { asyncRoutes } from './index'

/** 无需登录即可访问的公共页 */
const PUBLIC_PATHS = ['/', '/403', '/404']

/** 游客可浏览的移动端页面前缀（保持「离线浏览 + 离线购物车 → 登录合并」演示流程） */
const GUEST_BROWSE = ['/user/home', '/user/search', '/user/product/', '/user/cart']

function isGuestBrowse(path: string): boolean {
  return GUEST_BROWSE.some((p) => path.startsWith(p))
}

/**
 * 根据用户权限过滤动态路由（超级管理员放行全部）
 */
function filterAsyncRoutes(routes: RouteRecordRaw[], permissions: string[], roles: string[]): RouteRecordRaw[] {
  const isSuper = roles.some((r) => r.toLowerCase() === 'super_admin')
  const res: RouteRecordRaw[] = []

  routes.forEach((route) => {
    const tmp = { ...route }
    const perm = (tmp.meta?.permission as string) || ''
    const hasAccess = isSuper || !perm || permissions.includes(perm)

    if (hasAccess) {
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, permissions, roles)
      }
      res.push(tmp)
    }
  })
  return res
}

/**
 * 移除全部动态路由（再次登录 / 切换账号前调用，避免残留上一账号的全量菜单）。
 * 幂等：未挂载时直接跳过。
 */
export function resetRouter(router: Router) {
  asyncRoutes.forEach((r) => {
    const name = r.name as string
    if (name && router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
}

/**
 * 按当前账号权限挂载动态路由（登录后 / 页面刷新后各执行一次）。
 * 幂等：顶层路由已注册则跳过，防止重复 addRoute 告警。
 */
function mountAsyncRoutes(router: Router, permissions: string[], roles: string[]) {
  const accessRoutes = filterAsyncRoutes(asyncRoutes, permissions, roles)
  accessRoutes.forEach((r) => {
    if (!router.hasRoute(r.name as string)) {
      router.addRoute(r)
    }
  })
}

export function setupPermission(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const token = getToken()
    const userStore = useUserStore()

    // ---------- 未登录 ----------
    if (!token) {
      if (PUBLIC_PATHS.includes(to.path)) return next()
      // 游客浏览移动端商城（含离线购物车）
      if (to.path.startsWith('/user') && isGuestBrowse(to.path)) return next()
      return next('/')
    }

    // ---------- 已登录 ----------
    const role: Role = (userStore.role || 'user') as Role

    // 访问门户 → 回跳本角色首页
    if (to.path === '/' || to.path === '/login' || to.path === '/portal') {
      return next(roleHome(role, userStore.mchState))
    }

    if (role === 'admin') {
      if (to.path.startsWith('/user') || to.path.startsWith('/mch')) return next('/403')
      if (!to.path.startsWith('/admin')) return next(roleHome('admin'))

      // 动态路由未挂载则先挂载（登录响应 / localStorage 已有 adminInfo 时直接复用，免多余请求）
      const permissionStore = usePermissionStore()
      if (!permissionStore.routesLoaded) {
        try {
          let info = userStore.adminInfo
          if (!info) {
            info = await userStore.fetchUserInfo()
          }
          permissionStore.setRoles(info.roles || [])
          permissionStore.setPermissions(info.permissions || [])
          mountAsyncRoutes(router, info.permissions || [], info.roles || [])
          permissionStore.routesLoaded = true
          // 注：不能用 next({ ...to })。冷加载深层路由时初始 to 命中的是 catch-all(404)，
          // 展开该 location 对象会让 vue-router 重解析失效；改传纯 path 字符串重新导航。
          return next(to.path)
        } catch (e) {
          userStore.reset()
          permissionStore.reset()
          ElMessage.error('获取用户信息失败，请重新登录')
          return next('/')
        }
      }
      return next()
    }

    if (role === 'user') {
      if (to.path.startsWith('/admin') || to.path.startsWith('/mch')) return next('/403')
      if (to.path === '/user') return next('/user/home')
      return next()
    }

    if (role === 'mch') {
      if (to.path.startsWith('/admin') || to.path.startsWith('/user')) return next('/403')
      if (to.path === '/mch') return next(roleHome('mch', userStore.mchState))
      return next()
    }

    return next('/')
  })

  router.afterEach((to) => {
    const title = (to.meta?.title as string) || ''
    document.title = title ? `${title} - Mall-X 多商家电商平台` : 'Mall-X 多商家电商平台'
  })
}