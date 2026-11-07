<template>
  <div class="app-container">
    <div class="search-bar">
      <span class="tip">配置项按 configKey 全局唯一，保存时自动创建或更新（upsert）</span>
      <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增参数</el-button>
      <el-button :icon="Refresh" @click="loadList">刷新</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="configKey" label="参数键" min-width="200">
          <template #default="{ row }">
            <div class="key">{{ row.configKey }}</div>
            <div class="sub">{{ keyCn(row.configKey) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="configValue" label="参数值" min-width="160">
          <template #default="{ row }">
            <el-tag v-if="!isPassword(row)" type="info" effect="plain">{{ row.configValue }}</el-tag>
            <span v-else class="pwd">••••••</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="editing?.id ? '编辑系统参数' : '新增系统参数'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="参数键" required>
          <el-input v-model="form.configKey" placeholder="如 order.timeout / payment.settle-day" :disabled="!!editing?.id" />
        </el-form-item>
        <el-form-item label="参数值" required>
          <el-input v-model="form.configValue" placeholder="参数值" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" placeholder="用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getSysConfigList, saveSysConfig } from '@/api/admin/system'
import type { SysConfig } from '@/types'

const loading = ref(false)
const list = ref<SysConfig[]>([])

const visible = ref(false)
const saving = ref(false)
const editing = ref<SysConfig | null>(null)
const form = reactive({ configKey: '', configValue: '', remark: '' })

/** 常见参数键的中文说明 */
function keyCn(key: string) {
  const map: Record<string, string> = {
    'order.timeout': '订单超时分钟数',
    'refund.large-threshold': '大额退款金额阈值',
    'platform.service-phone': '平台客服电话',
    'platform.notice': '平台公告',
    'payment.settle-day': '结算周期'
  }
  return map[key] || ''
}

function isPassword(row: SysConfig) {
  return /pass|secret|key|token/i.test(row.configKey)
}

async function loadList() {
  loading.value = true
  try {
    list.value = await getSysConfigList()
  } finally {
    loading.value = false
  }
}

function openEdit(row: SysConfig | null) {
  editing.value = row
  form.configKey = row?.configKey || ''
  form.configValue = row?.configValue || ''
  form.remark = row?.remark || ''
  visible.value = true
}

async function handleSave() {
  if (!form.configKey.trim() || !form.configValue.trim()) {
    ElMessage.warning('参数键和参数值不能为空')
    return
  }
  saving.value = true
  try {
    await saveSysConfig({ configKey: form.configKey.trim(), configValue: form.configValue.trim(), remark: form.remark })
    ElMessage.success('系统参数已保存')
    visible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.tip { color: #909399; font-size: 13px; margin-right: auto; }
.key { font-weight: 600; }
.sub { color: #909399; font-size: 12px; margin-top: 2px; }
.pwd { color: #909399; letter-spacing: 2px; }
</style>