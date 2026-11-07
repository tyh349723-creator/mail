<template>
  <div class="app-container">
    <div class="search-bar">
      <el-input
        v-model="query.keyword"
        placeholder="商户编号/店铺名称"
        style="width: 220px"
        clearable
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="店铺状态" style="width: 150px" clearable>
        <el-option label="营业中" :value="1" />
        <el-option label="已关闭" :value="3" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="merchantNo" label="商户编号" width="140" />
        <el-table-column prop="shopName" label="店铺名称" min-width="160" />
        <el-table-column prop="contactName" label="负责人" width="100" />
        <el-table-column prop="shopPhone" label="联系电话" width="130" />
        <el-table-column prop="categoryName" label="经营类目" width="120" />
        <el-table-column label="店铺状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '营业中' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openRate(row)">费率配置</el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              size="small"
              link
              @click="handleToggle(row)"
            >
              关闭店铺
            </el-button>
            <el-button v-else type="success" size="small" link @click="handleToggle(row)">
              恢复营业
            </el-button>
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

    <!-- 差异化费率配置弹窗（5.3.2：按渠道配置商户费率） -->
    <el-dialog v-model="rateVisible" :title="`费率配置 · ${rateMerchant?.shopName || ''}`" width="560px">
      <el-alert
        title="差异化费率按渠道独立配置，留空保存则恢复为渠道基础费率"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-table :data="rateList" border size="small">
        <el-table-column prop="channelName" label="支付渠道" width="150" />
        <el-table-column label="基础费率" width="130">
          <template #default="{ row }">
            <span>{{ (Number(row.baseRate) * 100).toFixed(2) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="商户费率（%）">
          <template #default="{ row }">
            <el-input-number
              v-model="row.mchRatePct"
              :min="0"
              :max="10"
              :step="0.05"
              :precision="4"
              size="small"
              controls-position="right"
              style="width: 140px"
            />
          </template>
        </el-table-column>
        <el-table-column label="当前费率" width="110">
          <template #default="{ row }">
            <span>{{ (Number(row.mchRate) * 100).toFixed(2) }}%</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="rateVisible = false">取消</el-button>
        <el-button type="primary" :loading="rateSaving" @click="saveRates">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getMerchantList, updateShopStatus, getMerchantRateList, updateMerchantRate } from '@/api/admin/merchant'
import type { Merchant, MerchantRateItem } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const loading = ref(false)
const list = ref<Merchant[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })

async function loadList() {
  loading.value = true
  try {
    const res = await getMerchantList(query)
    list.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

async function handleToggle(row: Merchant) {
  const targetStatus = row.status === 1 ? 3 : 1
  const action = targetStatus === 3 ? '关闭' : '恢复'
  try {
    await ElMessageBox.confirm(`确认${action}店铺「${row.shopName}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await updateShopStatus({ id: row.id, status: targetStatus })
  ElMessage.success(`${action}成功`)
  loadList()
}

// ---------- 差异化费率配置 ----------
const rateVisible = ref(false)
const rateSaving = ref(false)
const rateMerchant = ref<Merchant | null>(null)
const rateList = ref<(MerchantRateItem & { mchRatePct: number })[]>([])

async function openRate(row: Merchant) {
  rateMerchant.value = row
  rateVisible.value = true
  const list = await getMerchantRateList(row.merchantNo)
  rateList.value = list.map((r) => ({ ...r, mchRatePct: Number(((Number(r.mchRate) || 0) * 100).toFixed(4)) }))
}

async function saveRates() {
  if (!rateMerchant.value) return
  rateSaving.value = true
  try {
    for (const r of rateList.value) {
      // 修改了才提交；费率以百分数录入，后端按小数存储
      await updateMerchantRate({
        shopNum: rateMerchant.value.merchantNo,
        wayCode: r.wayCode,
        mchRate: (Number(r.mchRatePct) / 100).toFixed(4)
      })
    }
    ElMessage.success('费率配置已保存')
    rateVisible.value = false
  } finally {
    rateSaving.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  query.keyword = ''
  query.status = undefined
  query.pageNum = 1
  loadList()
}

onMounted(loadList)
</script>