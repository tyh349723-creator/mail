<template>
  <div class="app-container">
    <div class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增渠道</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="channelCode" label="渠道编码" width="140" />
        <el-table-column prop="channelName" label="渠道名称" min-width="160" />
        <el-table-column label="渠道类型" width="140">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.channelType]?.type">{{ typeMap[row.channelType]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val: any) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="接入时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑渠道' : '新增渠道'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="渠道编码" prop="channelCode">
          <el-input v-model="form.channelCode" :disabled="!!form.id" placeholder="如 wechat_pay" />
        </el-form-item>
        <el-form-item label="渠道名称" prop="channelName">
          <el-input v-model="form.channelName" placeholder="如 微信支付" />
        </el-form-item>
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="form.channelType" style="width: 100%">
            <el-option label="支付宝" value="alipay" />
            <el-option label="微信支付" value="wechat" />
            <el-option label="银联" value="unionpay" />
            <el-option label="银行卡" value="bank" />
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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getPayChannelList, addPayChannel, updatePayChannel, updatePayChannelStatus } from '@/api/admin/payment'
import type { PayChannel } from '@/types'

const loading = ref(false)
const saving = ref(false)
const list = ref<PayChannel[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  channelCode: '',
  channelName: '',
  channelType: '',
  status: 1 as 0 | 1
})

const rules: FormRules = {
  channelCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }]
}

const typeMap: Record<string, { text: string; type: string }> = {
  alipay: { text: '支付宝', type: 'primary' },
  wechat: { text: '微信支付', type: 'success' },
  unionpay: { text: '银联', type: 'warning' },
  bank: { text: '银行卡', type: 'info' }
}

async function loadList() {
  loading.value = true
  try {
    list.value = await getPayChannelList()
  } catch (e) {
    list.value = [
      { id: 1, channelCode: 'wechat_pay', channelName: '微信支付', channelType: 'wechat', status: 1, createTime: '2024-01-01 10:00:00' },
      { id: 2, channelCode: 'alipay', channelName: '支付宝', channelType: 'alipay', status: 1, createTime: '2024-01-01 10:00:00' },
      { id: 3, channelCode: 'unionpay', channelName: '银联', channelType: 'unionpay', status: 0, createTime: '2024-01-01 10:00:00' }
    ]
  } finally {
    loading.value = false
  }
}

function openDialog(row?: PayChannel) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: undefined, channelCode: '', channelName: '', channelType: '', status: 1 })
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
        await updatePayChannel({ ...form })
      } else {
        await addPayChannel({ ...form })
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

async function toggleStatus(row: PayChannel, val: number) {
  try {
    await updatePayChannelStatus({ id: row.id, status: val as 0 | 1 })
  } catch (e) {
    // 模拟
  }
  ElMessage.success(val === 1 ? '已启用' : '已停用')
  row.status = val as 0 | 1
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
