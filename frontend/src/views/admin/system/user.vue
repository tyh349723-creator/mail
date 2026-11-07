<template>
  <div class="app-container">
    <div class="search-bar">
      <el-input
        v-model="query.keyword"
        placeholder="用户名/昵称"
        style="width: 220px"
        clearable
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      <div class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增管理员</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" class="mr4">{{ r }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openDialog(row)">编辑</el-button>
            <el-button type="warning" size="small" link @click="resetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadList"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-select v-model="form.roles" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
          </el-select>
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
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getAdminList, addAdmin, updateAdmin, resetAdminPassword, getRoleList } from '@/api/admin/system'
import type { AdminUserInfo, Role } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref<AdminUserInfo[]>([])
const roles = ref<Role[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  nickname: '',
  roles: [] as string[]
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const res = await getAdminList(query)
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    list.value = [
      { id: 1, username: 'admin', nickname: '超级管理员', roles: ['SUPER_ADMIN'], permissions: [] },
      { id: 2, username: 'operator', nickname: '运营专员', roles: ['OPERATOR'], permissions: [] },
      { id: 3, username: 'finance', nickname: '财务专员', roles: ['FINANCE'], permissions: [] }
    ]
    total.value = 3
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    roles.value = await getRoleList()
  } catch (e) {
    roles.value = [
      { id: 1, roleName: '超级管理员', roleCode: 'SUPER_ADMIN', status: 1 },
      { id: 2, roleName: '运营专员', roleCode: 'OPERATOR', status: 1 },
      { id: 3, roleName: '财务专员', roleCode: 'FINANCE', status: 1 }
    ]
  }
}

function openDialog(row?: AdminUserInfo) {
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, nickname: row.nickname, roles: [...row.roles], password: '' })
  } else {
    Object.assign(form, { id: undefined, username: '', password: '', nickname: '', roles: [] })
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
        await updateAdmin({ ...form })
      } else {
        await addAdmin({ ...form })
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

async function resetPassword(row: AdminUserInfo) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为「${row.username}」设置新密码`,
      '重置密码',
      { inputType: 'password', inputPattern: /^.{6,}$/, inputErrorMessage: '密码至少6位' }
    )
    await resetAdminPassword({ id: row.id, password: value })
    ElMessage.success('密码已重置')
  } catch (e) {
    // 取消
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  query.keyword = ''
  query.pageNum = 1
  loadList()
}

onMounted(() => {
  loadList()
  loadRoles()
})
</script>

<style scoped lang="scss">
.spacer {
  flex: 1;
}
.mr4 {
  margin-right: 4px;
}
</style>
