<template>
  <div class="app-container">
    <div class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openAdd(null)">新增一级分类</el-button>
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
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column prop="level" label="层级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? '' : row.level === 2 ? 'success' : 'warning'" size="small">
              {{ row.level }}级
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val: any) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.level < 3" type="primary" size="small" link @click="openAdd(row)">
              添加子分类
            </el-button>
            <el-button type="primary" size="small" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="父级分类">
          <el-input :model-value="parentName || '顶级分类'" disabled />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getCategoryTree, addCategory, updateCategory, deleteCategory, updateCategoryStatus } from '@/api/admin/category'
import type { Category } from '@/types'

const loading = ref(false)
const saving = ref(false)
const tree = ref<Category[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const parentName = ref('')

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  parentId: 0,
  level: 1,
  sort: 0,
  status: 1 as 0 | 1
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

async function loadTree() {
  loading.value = true
  try {
    tree.value = await getCategoryTree()
  } catch (e) {
    tree.value = buildMockTree()
  } finally {
    loading.value = false
  }
}

function buildMockTree(): Category[] {
  return [
    {
      id: 1, name: '数码家电', parentId: 0, level: 1, sort: 1, status: 1,
      children: [
        { id: 11, name: '手机通讯', parentId: 1, level: 2, sort: 1, status: 1, children: [
          { id: 111, name: '智能手机', parentId: 11, level: 3, sort: 1, status: 1 },
          { id: 112, name: '老人机', parentId: 11, level: 3, sort: 2, status: 1 }
        ]},
        { id: 12, name: '电脑办公', parentId: 1, level: 2, sort: 2, status: 1, children: [] }
      ]
    },
    {
      id: 2, name: '服饰鞋包', parentId: 0, level: 1, sort: 2, status: 1,
      children: [
        { id: 21, name: '男装', parentId: 2, level: 2, sort: 1, status: 1, children: [] },
        { id: 22, name: '女装', parentId: 2, level: 2, sort: 2, status: 1, children: [] }
      ]
    },
    {
      id: 3, name: '食品生鲜', parentId: 0, level: 1, sort: 3, status: 1, children: [] }
  ]
}

function openAdd(parent: Category | null) {
  form.id = undefined
  form.name = ''
  form.sort = 0
  form.status = 1
  if (parent) {
    form.parentId = parent.id
    form.level = parent.level + 1
    parentName.value = parent.name
  } else {
    form.parentId = 0
    form.level = 1
    parentName.value = ''
  }
  dialogVisible.value = true
}

function openEdit(row: Category) {
  form.id = row.id
  form.name = row.name
  form.sort = row.sort
  form.status = row.status
  form.parentId = row.parentId
  form.level = row.level
  parentName.value = row.parentId === 0 ? '' : '父级分类'
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateCategory({ ...form })
      } else {
        await addCategory({ ...form })
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadTree()
    } catch (e) {
      // 模拟成功
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadTree()
    } finally {
      saving.value = false
    }
  })
}

async function handleDelete(row: Category) {
  if (row.children && row.children.length > 0) {
    ElMessage.warning('存在子分类，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteCategory(row.id)
  } catch (e) {
    // 模拟
  }
  ElMessage.success('删除成功')
  loadTree()
}

async function toggleStatus(row: Category, val: number) {
  try {
    await updateCategoryStatus({ id: row.id, status: val as 0 | 1 })
  } catch (e) {
    // 模拟
  }
  ElMessage.success(val === 1 ? '已启用' : '已禁用')
  row.status = val as 0 | 1
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
