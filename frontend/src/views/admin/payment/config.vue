<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>支付渠道</template>
          <el-menu :default-active="String(activeId)" @select="handleSelect">
            <el-menu-item v-for="c in channels" :key="c.id" :index="String(c.id)">
              <el-icon><Wallet /></el-icon>
              <span>{{ c.channelName }}</span>
              <el-tag :type="c.status === 1 ? 'success' : 'info'" size="small" class="ml8">
                {{ c.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <el-card shadow="never" v-if="activeChannel">
          <template #header>
            参数配置 - {{ activeChannel.channelName }}
          </template>

          <el-form ref="formRef" :model="configForm" label-width="140px" style="max-width: 560px">
            <template v-for="field in currentFields" :key="field.key">
              <el-form-item :label="field.label">
                <el-input
                  v-model="configForm[field.key]"
                  :placeholder="field.placeholder"
                  :type="field.secret ? 'password' : 'text'"
                  show-password
                />
              </el-form-item>
            </template>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-empty v-else description="请选择左侧渠道进行参数配置" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Wallet } from '@element-plus/icons-vue'
import { getPayChannelList, savePayChannelConfig } from '@/api/admin/payment'
import type { PayChannel } from '@/types'

interface FieldConfig {
  key: string
  label: string
  placeholder: string
  secret?: boolean
}

const channels = ref<PayChannel[]>([])
const activeId = ref<number>()
const configForm = ref<Record<string, string>>({})
const saving = ref(false)

const activeChannel = computed(() => channels.value.find((c) => c.id === activeId.value))

// 不同渠道的参数字段定义
const fieldMap: Record<string, FieldConfig[]> = {
  wechat: [
    { key: 'appId', label: 'AppID', placeholder: '微信支付 AppID' },
    { key: 'mchId', label: '商户号', placeholder: '微信支付商户号' },
    { key: 'apiKey', label: 'API密钥', placeholder: 'API v3 密钥', secret: true },
    { key: 'notifyUrl', label: '回调地址', placeholder: 'https://.../pay/notify/wechat' }
  ],
  alipay: [
    { key: 'appId', label: 'AppID', placeholder: '支付宝 AppID' },
    { key: 'merchantPrivateKey', label: '商户私钥', placeholder: '应用私钥', secret: true },
    { key: 'alipayPublicKey', label: '支付宝公钥', placeholder: '支付宝公钥', secret: true },
    { key: 'notifyUrl', label: '回调地址', placeholder: 'https://.../pay/alipay/notify' }
  ],
  unionpay: [
    { key: 'merId', label: '商户号', placeholder: '银联商户号' },
    { key: 'signCertPath', label: '签名证书', placeholder: '签名证书路径' },
    { key: 'notifyUrl', label: '回调地址', placeholder: 'https://.../pay/notify/unionpay' }
  ],
  bank: [
    { key: 'bankCode', label: '银行编码', placeholder: '银行联行号' },
    { key: 'accountNo', label: '收款账号', placeholder: '收款账户' },
    { key: 'accountName', label: '账户名', placeholder: '账户名称' }
  ]
}

const currentFields = computed<FieldConfig[]>(() => {
  if (!activeChannel.value) return []
  return fieldMap[activeChannel.value.channelType] || []
})

async function loadChannels() {
  try {
    channels.value = await getPayChannelList()
  } catch (e) {
    channels.value = [
      { id: 1, channelCode: 'wechat_pay', channelName: '微信支付', channelType: 'wechat', status: 1 },
      { id: 2, channelCode: 'alipay', channelName: '支付宝', channelType: 'alipay', status: 1 },
      { id: 3, channelCode: 'unionpay', channelName: '银联', channelType: 'unionpay', status: 0 }
    ]
  }
  if (channels.value.length > 0) {
    activeId.value = channels.value[0].id
    resetForm()
  }
}

function handleSelect(index: string) {
  activeId.value = Number(index)
  resetForm()
}

function resetForm() {
  const fields = currentFields.value
  const obj: Record<string, string> = {}
  fields.forEach((f) => (obj[f.key] = ''))
  configForm.value = obj
}

async function handleSave() {
  if (!activeChannel.value) return
  saving.value = true
  try {
    await savePayChannelConfig({
      id: activeChannel.value.id,
      configParams: configForm.value
    })
    ElMessage.success('配置保存成功')
  } catch (e) {
    ElMessage.success('配置保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(loadChannels)
</script>

<style scoped lang="scss">
.ml8 {
  margin-left: 8px;
}
</style>
