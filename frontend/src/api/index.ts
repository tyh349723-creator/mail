/**
 * API 统一出口（barrel）。
 * 按业务域拆分为模块（user/product/cart/order/pay/mch/wallet/portal），此处集中再导出，
 * 视图层可整体 `import { xxx } from '@/api'`，也可按需 `import { xxx } from '@/api/order'`。
 * 管理端接口见 @/api/admin/，单独引入。
 */
export * from './portal'
export * from './user'
export * from './product'
export * from './cart'
export * from './order'
export * from './pay'
export * from './mch'
export * from './wallet'