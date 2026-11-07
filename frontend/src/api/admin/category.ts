import { request } from '@/utils/request'
import type { Category } from '@/types'

/** 分类树：后端 {list:[...]} */
export async function getCategoryTree() {
  const res = await request<Record<string, any>>({
    url: '/admin/category/tree',
    method: 'post',
    data: {}
  })
  return (res.list || []) as Category[]
}

/** 新增分类 */
export function addCategory(data: Partial<Category>) {
  return request({
    url: '/admin/category/save',
    method: 'post',
    data
  })
}

/** 编辑分类 */
export function updateCategory(data: Partial<Category>) {
  return request({
    url: '/admin/category/save',
    method: 'post',
    data
  })
}

/** 删除分类 */
export function deleteCategory(id: number) {
  return request({
    url: '/admin/category/delete',
    method: 'post',
    data: { id }
  })
}

/** 更新分类状态 */
export function updateCategoryStatus(data: { id: number; status: 0 | 1 }) {
  return request({
    url: '/admin/category/status',
    method: 'post',
    data
  })
}