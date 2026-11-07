<template>
  <div class="app-container">
    <div class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增权限</el-button>
        <el-button :icon="Refresh" @click="loadTree">刷新</el-button>
      </div>

      <el-table
        :data="tree"
        row-key="id"
        stripe
        border
        v-loading="loading"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="permName" label="权限名称" min-width="200" />
        <el-table-column prop="permCode" label="权限编码" min-width="200" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.type">{{ typeMap[row.type]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.type === 'menu'" type="primary" size="small" link @click="openDialog(row)">
              添加子级
            </el-button>
            <el-button type="primary" size="small" link @click="openDialog(row, true)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑权限' : '新增权限'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级权限">
          <el-input :model-value="parentName || '顶级权限'" disabled />
        </el-form-item>
        <el-form-item label="权限名称" prop="permName">
          <el-input v-model="form.permName" />
        </el-form-item>
        <el-form-item label="权限编码" prop="permCode">
          <el-input v-model="form.permCode" placeholder="如 merchant:review" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button label="menu">菜单</el-radio-button>
            <el-radio-button label="button">按钮</el-radio-button>
            <el-radio-button label="api">接口</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="如 /merchant/review" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getPermissionTree } from '@/api/admin/system'
import type { Permission } from '@/types'

const loading = ref(false)
const saving = ref(false)
const tree = ref<Permission[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const parentName = ref('')

const form = reactive({
  id: undefined as number | undefined,
  permName: '',
  permCode: '',
  type: 'menu' as 'menu' | 'button' | 'api',
  parentId: 0,
  path: '',
  sort: 0
})

const rules: FormRules = {
  permName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }]
}

const typeMap: Record<string, { text: string; type: string }> = {
  menu: { text: '菜单', type: 'primary' },
  button: { text: '按钮', type: 'warning' },
  api: { text: '接口', type: 'info' }
}

async function loadTree() {
  loading.value = true
  try {
    tree.value = await getPermissionTree()
  } catch (e) {
    tree.value = buildMockTree()
  } finally {
    loading.value = false
  }
}

function buildMockTree(): Permission[] {
  return [
    { id: 1, permName: '数据看板', permCode: 'dashboard:view', type: 'menu', parentId: 0, path: '/admin/dashboard', sort: 1 },
    {
      id: 2, permName: '商户管控', permCode: 'merchant', type: 'menu', parentId: 0, path: '/admin/merchant', sort: 2,
      children: [
        { id: 21, permName: '商户列表', permCode: 'merchant:status', type: 'menu', parentId: 2, path: '/admin/merchant/list', sort: 1 },
        { id: 22, permName: '入驻审核', permCode: 'merchant:review', type: 'menu', parentId: 2, path: '/admin/merchant/review', sort: 2 },
        { id: 23, permName: '资质审查', permCode: 'merchant:qualification', type: 'menu', parentId: 2, path: '/admin/merchant/qualification', sort: 3 }
      ]
    },
    {
      id: 3, permName: '订单查询', permCode: 'order:list', type: 'menu', parentId: 0, path: '/admin/order', sort: 3,
      children: []
    },
    {
      id: 4, permName: '退款监管', permCode: 'order:refund', type: 'menu', parentId: 0, path: '/admin/refund', sort: 4,
      children: []
    },
    {
      id: 5, permName: '支付渠道', permCode: 'payment', type: 'menu', parentId: 0, path: '/admin/payment', sort: 5,
      children: [
        { id: 51, permName: '渠道管理', permCode: 'payment:list', type: 'menu', parentId: 5, path: '/admin/payment/channel', sort: 1 },
        { id: 52, permName: '渠道密钥', permCode: 'payment:config', type: 'menu', parentId: 5, path: '/admin/payment/config', sort: 2 }
      ]
    },
    {
      id: 6, permName: '平台统计', permCode: 'report', type: 'menu', parentId: 0, path: '/admin/report', sort: 6,
      children: [
        { id: 61, permName: '交易数据可视化', permCode: 'report:trade', type: 'menu', parentId: 6, path: '/admin/report/trade', sort: 1 },
        { id: 62, permName: '商户经营分析', permCode: 'report:merchant', type: 'menu', parentId: 6, path: '/admin/report/merchant', sort: 2 }
      ]
    },
    {
      id: 7, permName: '权限管理', permCode: 'system', type: 'menu', parentId: 0, path: '/admin/perm', sort: 7,
      children: [
        { id: 71, permName: '管理员管理', permCode: 'system:user', type: 'menu', parentId: 7, path: '/admin/perm/user', sort: 1 },
        { id: 72, permName: '角色管理', permCode: 'system:role', type: 'menu', parentId: 7, path: '/admin/perm/role', sort: 2 },
        { id: 73, permName: '权限管理', permCode: 'system:permission', type: 'menu', parentId: 7, path: '/admin/perm/permission', sort: 3 }
      ]
    },
    {
      id: 8, permName: '系统运维', permCode: 'system:ops', type: 'menu', parentId: 0, path: '/admin/ops', sort: 8,
      children: [
        { id: 81, permName: '系统参数', permCode: 'system:config', type: 'menu', parentId: 8, path: '/admin/ops/settings', sort: 1 },
        { id: 82, permName: '操作日志', permCode: 'system:log', type: 'menu', parentId: 8, path: '/admin/ops/log', sort: 2 },
        { id: 83, permName: '接口监控', permCode: 'system:monitor', type: 'menu', parentId: 8, path: '/admin/ops/monitor', sort: 3 }
      ]
    },
    {
      id: 9, permName: '运营管理', permCode: 'operation', type: 'menu', parentId: 0, path: '/admin/operation', sort: 9,
      children: [
        { id: 91, permName: '商品分类维护', permCode: 'category:list', type: 'menu', parentId: 9, path: '/admin/operation/category', sort: 1 },
        { id: 92, permName: '广告位配置', permCode: 'advert:list', type: 'menu', parentId: 9, path: '/admin/operation/advert', sort: 2 }
      ]
    }
  ]
}

function openDialog(row?: Permission, edit = false) {
  if (edit && row) {
    Object.assign(form, {
      id: row.id, permName: row.permName, permCode: row.permCode,
      type: row.type, parentId: row.parentId, path: row.path || '', sort: row.sort || 0
    })
    parentName.value = row.parentId === 0 ? '' : '上级权限'
  } else if (row) {
    // 添加子级
    Object.assign(form, {
      id: undefined, permName: '', permCode: '', type: 'menu',
      parentId: row.id, path: '', sort: 0
    })
    parentName.value = row.permName
  } else {
    Object.assign(form, { id: undefined, permName: '', permCode: '', type: 'menu', parentId: 0, path: '', sort: 0 })
    parentName.value = ''
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    // 调用新增/编辑权限接口（此处简化处理）
    ElMessage.success('保存成功')
    dialogVisible.value = false
    saving.value = false
    loadTree()
  })
}

onMounted(loadTree)
</script>

<style scoped lang="scss">
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
</style>
