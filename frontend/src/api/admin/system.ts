import { request } from '@/utils/request'
import type { Role, Permission, AdminUserInfo, PageResult, PageQuery, SysConfig, OperLog, MonitorStatus } from '@/types'

function toPage<T>(res: Record<string, any>): PageResult<T> {
  return {
    list: res.records || [],
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10
  }
}

/** 管理员列表：后端 roles 为逗号分隔字符串 → 数组 */
export function getAdminList(params: PageQuery) {
  return request<Record<string, any>>({
    url: '/admin/system/user/list',
    method: 'post',
    data: params
  }).then((res) => ({
    list: (res.records || []).map((a: Record<string, any>) => ({
      ...a,
      roles: String(a.roles || '').split(',').filter(Boolean)
    })),
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10
  }) as PageResult<AdminUserInfo>)
}

/** 新增管理员 */
export function addAdmin(data: Partial<AdminUserInfo> & { password?: string }) {
  return request({
    url: '/admin/system/user/add',
    method: 'post',
    data: {
      username: data.username,
      password: data.password,
      nickname: data.nickname,
      roles: (data.roles || []).join(',')
    }
  })
}

/** 编辑管理员 */
export function updateAdmin(data: Partial<AdminUserInfo>) {
  return request({
    url: '/admin/system/user/update',
    method: 'post',
    data: { id: data.id, nickname: data.nickname, roles: (data.roles || []).join(',') }
  })
}

/** 重置密码 */
export function resetAdminPassword(data: { id: number; password: string }) {
  return request({
    url: '/admin/system/user/reset-password',
    method: 'post',
    data
  })
}

/** 角色列表 */
export async function getRoleList() {
  const res = await request<Record<string, any>>({
    url: '/admin/role/list',
    method: 'post',
    data: {}
  })
  return (res.list || []) as Role[]
}

/** 新增角色 */
export function addRole(data: Partial<Role>) {
  return request({
    url: '/admin/role/save',
    method: 'post',
    data
  })
}

/** 编辑角色 */
export function updateRole(data: Partial<Role>) {
  return request({
    url: '/admin/role/save',
    method: 'post',
    data
  })
}

/** 删除角色 */
export function deleteRole(id: number) {
  return request({
    url: '/admin/role/delete',
    method: 'post',
    data: { id }
  })
}

/** 权限树 */
export async function getPermissionTree() {
  const res = await request<Record<string, any>>({
    url: '/admin/permission/tree',
    method: 'post',
    data: {}
  })
  return (res.tree || []) as Permission[]
}

/** 角色分配权限：后端以 perms 逗号串存储，权限树 id → permCode 后写回角色 */
export async function assignRolePermissions(data: { roleId: number; permIds: number[] }) {
  const tree = await getPermissionTree()
  const codeById = new Map<number, string>()
  const walk = (nodes: Permission[]) => {
    nodes.forEach((n) => {
      codeById.set(n.id, n.permCode)
      if (n.children) walk(n.children)
    })
  }
  walk(tree)
  const perms = data.permIds
    .map((id) => codeById.get(id))
    .filter((c): c is string => !!c)
    .join(',')
  return request({
    url: '/admin/role/save',
    method: 'post',
    data: { id: data.roleId, perms }
  })
}

// ---------- 系统运维（5.3.7） ----------

/** 系统参数列表 */
export async function getSysConfigList() {
  const res = await request<Record<string, any>>({
    url: '/admin/system/config/list',
    method: 'post',
    data: {}
  })
  return (res.list || []) as SysConfig[]
}

/** 保存系统参数（configKey 唯一，upsert） */
export function saveSysConfig(data: { configKey: string; configValue: string; remark?: string }) {
  return request({
    url: '/admin/system/config/save',
    method: 'post',
    data
  })
}

/** 操作日志列表（keyword 匹配 操作人/动作/对象） */
export async function getOperLogList(params: PageQuery & { keyword?: string }) {
  const res = await request<Record<string, any>>({
    url: '/admin/system/log/list',
    method: 'post',
    data: params
  })
  return {
    list: (res.records || []) as OperLog[],
    total: res.total || 0,
    pageNum: res.current || 1,
    pageSize: res.size || 10
  } as PageResult<OperLog>
}

/** 接口运行状态监控（运行时长/JVM/线程/接口调用 Top） */
export async function getMonitorStatus() {
  const res = await request<Record<string, any>>({
    url: '/admin/monitor/status',
    method: 'post',
    data: {}
  })
  return res as MonitorStatus
}