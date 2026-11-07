import { request } from '@/utils/request'

/**
 * 商品 / 分类（用户端首页、分类、搜索、详情）
 */
export function productHome(): Promise<any> {
  return request({ url: '/product/home', method: 'post', data: {} })
}

export function productList(d: Record<string, any>): Promise<any> {
  return request({ url: '/product/list', method: 'post', data: d })
}

export function productDetail(id: string): Promise<any> {
  return request({ url: '/product/detail', method: 'post', data: { id } })
}

export function categoryTree(): Promise<any> {
  return request({ url: '/product/category/tree', method: 'post', data: {} })
}