import { MODE_KEY, ROLE_KEY } from './const'

/**
 * 登录态统一存取（localStorage）。
 * 单页应用同一时刻只保留一个登录身份：三个角色共用同一 token 键 mall_token，
 * 以 mall_role（user | mch | admin）区分身份；移动端档案键沿用 mall_* 前缀以兼容历史数据。
 */
const TOKEN_KEY = 'mall_token'
const LEGACY_ADMIN_TOKEN_KEY = 'mall_admin_token' // 旧管理端 token，首次启动迁移
const REFRESH_TOKEN_KEY = 'mall_admin_refresh_token'
const USER_KEY = 'mall_admin_user'

export function getToken(): string {
  let t = localStorage.getItem(TOKEN_KEY) || ''
  // 兼容迁移：旧版 pc-admin 使用 mall_admin_token，首次启动并入 mall_token
  if (!t) {
    const legacy = localStorage.getItem(LEGACY_ADMIN_TOKEN_KEY)
    if (legacy) {
      t = legacy
      localStorage.setItem(TOKEN_KEY, t)
      localStorage.setItem(ROLE_KEY, 'admin')
    }
  }
  return t
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getRole(): string {
  return localStorage.getItem(ROLE_KEY) || ''
}

export function setRole(role: string): void {
  localStorage.setItem(ROLE_KEY, role)
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

/** 管理员附加信息（roles / permissions） */
export function getUser(): any {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function setUser(user: any): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/** 清空全部登录态 */
export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(MODE_KEY)
  localStorage.removeItem(LEGACY_ADMIN_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}