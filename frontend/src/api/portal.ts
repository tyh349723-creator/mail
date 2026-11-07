import { request } from '@/utils/request'

/**
 * 统一登录注册门户的三方角色认证接口。
 * 三个角色的登录/注册接口都挂在网关（10010）下统一定义了 body 签名契约，
 * 因此门户页复用 pc-admin 的统一请求层即可直接调用（vite proxy /api → 网关）。
 * 校验/错误提示由 request.ts 拦截器统一处理（id==="0" 成功，否则 ElMessage 提示）。
 */

export interface UserLoginResult {
  token: string
  userNum: string
  nickname: string
}

export interface MchLoginResult {
  token: string
  shopNum: string
  shopName: string
  /** 商户状态：0 待审核 1 正常 2 已驳回 3 已禁用 */
  shopState: number
}

// ---------- 用户端 ----------

export const userLogin = (data: { username: string; password: string }) =>
  request<UserLoginResult>({ url: '/user/login', method: 'post', data })

export const userRegister = (data: {
  username: string
  password: string
  nickname?: string
  phone?: string
}) => request({ url: '/user/register', method: 'post', data })

// ---------- 商户端 ----------

export const mchLogin = (data: { username: string; password: string }) =>
  request<MchLoginResult>({ url: '/mch/login', method: 'post', data })

/** 商户注册 = 提交入驻资料（后端 /mch/register 创建商户并置【待审核】，管理员审核通过后方可经营；字段与门户注册表单对齐） */
export const mchRegister = (data: {
  username: string
  password: string
  shopName: string
  shopPhone: string
  shopAddress: string
  contactName?: string
}) => request({ url: '/mch/register', method: 'post', data })