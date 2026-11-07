import { request } from '@/utils/request'
import type { LoginResult, AdminUserInfo } from '@/types'

/** 登录（POST /admin/login）：返回 {token, nickname, roles[], permissions[]} */
export async function login(data: { username: string; password: string }) {
  const res = await request<Record<string, any>>({
    url: '/admin/login',
    method: 'post',
    data
  })
  const result: LoginResult = {
    token: res.token,
    userInfo: {
      id: 0,
      username: data.username,
      nickname: res.nickname || data.username,
      roles: res.roles || [],
      permissions: res.permissions || []
    }
  }
  return result
}

/** 获取当前管理员信息（含权限） */
export async function getAdminInfo() {
  const res = await request<AdminUserInfo & { username: string; adminId?: number }>({
    url: '/admin/auth/info',
    method: 'post',
    data: {}
  })
  return {
    id: res.adminId ?? 0,
    username: res.username,
    nickname: res.nickname,
    roles: res.roles || [],
    permissions: res.permissions || []
  } as AdminUserInfo
}

