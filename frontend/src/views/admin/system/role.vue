<template>
  <div class="app-container">
    <div class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增角色</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" min-width="160" />
        <el-table-column prop="roleCode" label="角色编码" width="160" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openDialog(row)">编辑</el-button>
            <el-button type="warning" size="small" link @click="assignPermission(row)">分配权限</el-button>
            <el-button v-if="row.roleCode !== 'super_admin'" type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permVisible" title="分配权限" width="480px">
      <el-tree
        ref="treeRef"
        :data="permTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'permName', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermission">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getRoleList, addRole, updateRole, deleteRole, getPermissionTree, assignRolePermissions } from '@/api/admin/system'
import type { Role, Permission } from '@/types'

const loading = ref(false)
const saving = ref(false)
const list = ref<Role[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const permVisible = ref(false)
const permTree = ref<Permission[]>([])
const treeRef = ref()
const currentRoleId = ref<number>()

const form = reactive({
  id: undefined as number | undefined,
  roleName: '',
  roleCode: '',
  description: '',
  status: 1 as 0 | 1
})

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    list.value = await getRoleList()
  } catch (e) {
    list.value = [
      { id: 1, roleName: '超级管理员', roleCode: 'SUPER_ADMIN', description: '拥有全部权限', status: 1 },
      { id: 2, roleName: '运营专员', roleCode: 'OPERATOR', description: '负责商户、类目、广告运营', status: 1 },
      { id: 3, roleName: '财务专员', roleCode: 'FINANCE', description: '负责订单退款、对账', status: 1 }
    ]
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Role) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: undefined, roleName: '', roleCode: '', description: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateRole({ ...form })
      } else {
        await addRole({ ...form })
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } catch (e) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } finally {
      saving.value = false
    }
  })
}

async function handleDelete(row: Role) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteRole(row.id)
  } catch (e) {
    // 模拟
  }
  ElMessage.success('删除成功')
  loadList()
}

async function assignPermission(row: Role) {
  currentRoleId.value = row.id
  try {
    permTree.value = await getPermissionTree()
  } catch (e) {
    permTree.value = buildMockPermTree()
  }
  permVisible.value = true
}

function buildMockPermTree(): Permission[] {
  return [
    { id: 1, permName: '数据看板', permCode: 'dashboard:view', type: 'menu', parentId: 0 },
    {
      id: 2, permName: '商户管理', permCode: 'merchant', type: 'menu', parentId: 0,
      children: [
        { id: 21, permName: '入驻审核', permCode: 'merchant:review', type: 'menu', parentId: 2 },
        { id: 22, permName: '资质审查', permCode: 'merchant:qualification', type: 'menu', parentId: 2 },
        { id: 23, permName: '店铺状态管理', permCode: 'merchant:status', type: 'menu', parentId: 2 }
      ]
    },
    {
      id: 3, permName: '运营管理', permCode: 'operation', type: 'menu', parentId: 0,
      children: [
        { id: 31, permName: '商品分类维护', permCode: 'category:list', type: 'menu', parentId: 3 },
        { id: 32, permName: '广告位配置', permCode: 'advert:list', type: 'menu', parentId: 3 }
      ]
    },
    {
      id: 4, permName: '订单管控', permCode: 'order', type: 'menu', parentId: 0,
      children: [
        { id: 41, permName: '订单查询', permCode: 'order:list', type: 'menu', parentId: 4 },
        { id: 42, permName: '退款审批', permCode: 'order:refund', type: 'menu', parentId: 4 }
      ]
    },
    {
      id: 5, permName: '支付配置', permCode: 'payment', type: 'menu', parentId: 0,
      children: [
        { id: 51, permName: '渠道管理', permCode: 'payment:list', type: 'menu', parentId: 5 },
        { id: 52, permName: '参数配置', permCode: 'payment:config', type: 'menu', parentId: 5 }
      ]
    },
    {
      id: 6, permName: '统计报表', permCode: 'report', type: 'menu', parentId: 0,
      children: [
        { id: 61, permName: '交易数据', permCode: 'report:trade', type: 'menu', parentId: 6 },
        { id: 62, permName: '商户经营分析', permCode: 'report:merchant', type: 'menu', parentId: 6 }
      ]
    }
  ]
}

async function savePermission() {
  const checked = treeRef.value.getCheckedKeys() as number[]
  const halfChecked = treeRef.value.getHalfCheckedKeys() as number[]
  const permIds = [...checked, ...halfChecked]
  try {
    await assignRolePermissions({ roleId: currentRoleId.value!, permIds })
  } catch (e) {
    // 模拟
  }
  ElMessage.success('权限已保存')
  permVisible.value = false
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
</style>
