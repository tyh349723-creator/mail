import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginAdminApi, getAdminInfo as fetchAdminInfoApi } from '@/api/auth'
import { userLogin, mchLogin } from '@/api/portal'
import { MODE_KEY } from '@/utils/const'
import { getToken, setToken, setRefreshToken, getUser as getAdminUser, setUser as setAdminUser, clearAuth } from '@/utils/auth'
import type { AdminUserInfo } from '@/types'

export type Role = 'user' | 'mch' | 'admin'

/** 各角色登录后的默认落点 */
export function roleHome(role: Role, mchState: number = -1): string {
  if (role === 'admin') return '/admin/dashboard'
  if (role === 'mch') return mchState === 1 ? '/mch/profile' : '/mch/apply-status'
  return '/user/home'
}

/**
 * 统一登录态 store（三角色共用）：
 * - 单一 token + role，portal 登录后按角色写入档案并跳转对应工作台；
 * - 保留移动端旧调用的 setLogin / setMode / setMerchantInfo 方法名，兼容历史视图。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const role = ref<Role>((getRoleValue() as Role) || 'user')

  // 移动端用户档案
  const userNum = ref(localStorage.getItem('mall_usernum') || '')
  const nickname = ref(localStorage.getItem('mall_nickname') || '')
  const avatar = ref(localStorage.getItem('mall_avatar') || '')

  // 商户档案（入驻状态：-1 未入驻 0 待审核 1 正常 2 已驳回 3 已禁用）
  const mchState = ref<number>(Number(localStorage.getItem('mall_mchstate') ?? -1))
  const shopName = ref(localStorage.getItem('mall_shopname') || '')

  // 管理端档案（roles / permissions）
  const adminInfo = ref<AdminUserInfo | null>(getAdminUser())

  // 移动端模式镜像（user | merchant），主身份以 role 为准
  const mode = ref(localStorage.getItem(MODE_KEY) || 'merchant')

  const isLogin = computed(() => !!token.value)
  /** 当前单据身份对应的编号（userNum / shopNum） */
  const num = computed(() => userNum.value)

  function getRoleValue(): string {
    return localStorage.getItem('mall_role') || ''
  }

  function setRole(r: Role) {
    role.value = r
    localStorage.setItem('mall_role', r)
    const m = r === 'user' ? 'user' : 'merchant'
    mode.value = m
    localStorage.setItem(MODE_KEY, m)
  }

  /** 用户端登录（portal 调用） */
  async function loginUser(username: string, password: string) {
    const data = await userLogin({ username, password })
    token.value = data.token
    setToken(data.token)
    userNum.value = data.userNum || ''
    nickname.value = data.nickname || ''
    localStorage.setItem('mall_usernum', data.userNum || '')
    localStorage.setItem('mall_nickname', data.nickname || '')
    setRole('user')
    return data
  }

  /** 商户端登录（portal 调用） */
  async function loginMch(username: string, password: string) {
    const data = await mchLogin({ username, password })
    token.value = data.token
    setToken(data.token)
    shopName.value = data.shopName || ''
    mchState.value = Number(data.shopState ?? -1)
    localStorage.setItem('mall_shopname', data.shopName || '')
    localStorage.setItem('mall_mchstate', String(data.shopState ?? -1))
    setRole('mch')
    return data
  }

  /** 管理端登录（portal 调用） */
  async function login(username: string, password: string) {
    const data = await loginAdminApi({ username, password })
    token.value = data.token
    setToken(data.token)
    if (data.refreshToken) setRefreshToken(data.refreshToken)
    if (data.userInfo) {
      adminInfo.value = data.userInfo
      setAdminUser(data.userInfo)
    }
    setRole('admin')
    return data
  }

  /** 兼容移动端旧调用：setLogin(token, { userNum, nickname } | { shopNum, nickname }) */
  function setLogin(_token: string, meta: Record<string, any> = {}) {
    token.value = _token
    setToken(_token)
    if (meta.userNum) {
      userNum.value = String(meta.userNum)
      localStorage.setItem('mall_usernum', String(meta.userNum))
    }
    if (meta.shopNum) {
      userNum.value = String(meta.shopNum)
      localStorage.setItem('mall_usernum', String(meta.shopNum))
    }
    if (meta.nickname) {
      nickname.value = String(meta.nickname)
      localStorage.setItem('mall_nickname', String(meta.nickname))
    }
    setRole(meta.shopNum ? 'mch' : 'user')
  }

  /** 兼容移动端旧调用：setMode('user' | 'merchant') */
  function setMode(m: string) {
    setRole(m === 'merchant' ? 'mch' : 'user')
  }

  function setMerchantInfo(name: string, state: number) {
    shopName.value = name
    mchState.value = state
    localStorage.setItem('mall_shopname', name || '')
    localStorage.setItem('mall_mchstate', String(state ?? -1))
  }

  /** 管理端：拉取用户信息与权限（守卫用） */
  async function fetchUserInfo() {
    const info = await fetchAdminInfoApi()
    adminInfo.value = info
    setAdminUser(info)
    return info
  }

  /** 退出登录（后端无独立退出接口，前端清理即可） */
  async function logout() {
    reset()
  }

  function reset() {
    token.value = ''
    role.value = 'user'
    userNum.value = ''
    nickname.value = ''
    avatar.value = ''
    mchState.value = -1
    shopName.value = ''
    adminInfo.value = null
    localStorage.removeItem('mall_usernum')
    localStorage.removeItem('mall_nickname')
    localStorage.removeItem('mall_avatar')
    localStorage.removeItem('mall_mchstate')
    localStorage.removeItem('mall_shopname')
    clearAuth()
  }

  return {
    token,
    role,
    mode,
    isLogin,
    num,
    userNum,
    nickname,
    avatar,
    mchState,
    shopName,
    adminInfo,
    setRole,
    setLogin,
    setMode,
    setMerchantInfo,
    loginUser,
    loginMch,
    login,
    fetchUserInfo,
    logout,
    reset
  }
})