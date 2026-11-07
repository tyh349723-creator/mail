import MD5 from 'crypto-js/md5'

/**
 * 接口签名（对齐《行业工程实践》指导书 & 后端 SignUtil）：
 * sign = MD5( 排序后的 k=v& 参数串 + secretKey ).toUpperCase()
 * timestamp / nonce 随业务参数一起放入请求体参与签名（防重放）；复杂值（数组/对象）JSON 序列化。
 */
const SECRET = import.meta.env.VITE_SIGN_SECRET || ''

/** 对参数对象生成签名（不含 sign 自身；undefined/null 字段已由调用方剔除） */
export function signParams(params: Record<string, any>): string {
  const keys = Object.keys(params).sort()
  const parts = keys.map((k) => `${k}=${stringifyValue(params[k])}`)
  return MD5(parts.join('&') + SECRET).toString().toUpperCase()
}

/** 值与签名拼接时的文本化：字符串原样、布尔/数字直接、复合类型 JSON */
function stringifyValue(v: any): string {
  if (v === null || v === undefined) return ''
  switch (typeof v) {
    case 'string':
      return v
    case 'boolean':
      return v ? 'true' : 'false'
    case 'number':
      return String(v)
    default:
      return JSON.stringify(v)
  }
}

/** 生成随机字符串（10 位，与移动端一致） */
export function generateNonce(length = 10): string {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

/** 获取当前时间戳（秒） */
export function getTimestamp(): string {
  return String(Math.floor(Date.now() / 1000))
}