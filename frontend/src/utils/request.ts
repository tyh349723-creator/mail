import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken, clearAuth } from './auth'
import { signParams, generateNonce, getTimestamp } from './sign'

/**
 * 统一请求层（对齐新后端契约）：
 * - 所有业务接口 POST + JSON，timestamp/nonce/sign 放入请求体（body 签名）
 * - 统一响应 {id, message, ...业务字段}：id==="0" 成功；"401" 登录失效；其余业务错误 message 提示
 * - 分页字段 total/current/size/records，由各 api 适配为视图用的 {list,total}
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// 请求拦截器：附加 token + 生成请求体签名（null/undefined 参数不参与签名，避免与 JSON 序列化不一致）
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if ((config.method || 'get').toLowerCase() === 'post') {
      const data: Record<string, any> = {}
      if (config.data && typeof config.data === 'object') {
        for (const [k, v] of Object.entries(config.data as Record<string, any>)) {
          if (v !== undefined && v !== null) data[k] = v
        }
      }
      data.timestamp = getTimestamp()
      data.nonce = generateNonce()
      data.sign = signParams(data)
      config.data = data as any
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理（业务错误在 HTTP 200 中以 id 区分）
service.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res && String(res.id) === '0') {
      return res
    }

    // 登录失效
    if (res && String(res.id) === '401') {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '登录已失效'))
    }

    const msg = (res && res.message) || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      const msg = error.response?.data?.message || error.message || '网络异常，请稍后重试'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

let unauthorizedHandling = false
function handleUnauthorized(): void {
  if (unauthorizedHandling) return
  unauthorizedHandling = true
  ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      clearAuth()
      window.location.href = '/'
    })
    .finally(() => {
      unauthorizedHandling = false
    })
}

/** 通用请求方法 */
function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config) as unknown as Promise<T>
}

export default service
export { request }