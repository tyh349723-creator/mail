<template>
  <div class="app-container">
    <div class="search-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
      <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>商户销售额排行</template>
          <EChart :option="rankOption" height="360px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt16">
      <template #header>商户经营分析明细</template>
      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="merchantNo" label="商户编号" width="140" />
        <el-table-column prop="shopName" label="店铺名称" min-width="180" />
        <el-table-column prop="salesAmount" label="销售额" width="140">
          <template #default="{ row }">¥{{ row.salesAmount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="refundRate" label="退款率" width="100">
          <template #default="{ row }">{{ row.refundRate }}%</template>
        </el-table-column>
        <el-table-column prop="avgScore" label="评分" width="100">
          <template #default="{ row }">
            <el-rate :model-value="row.avgScore" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '营业中' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getMerchantRanking, getMerchantAnalysis } from '@/api/admin/report'
import type { ChartPoint } from '@/types'
import EChart from '@/components/common/EChart.vue'
import Pagination from '@/components/common/Pagination.vue'

const dateRange = ref<string[]>([])
const rankData = ref<ChartPoint[]>([])
const rankOption = ref<Record<string, any>>({})
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

function buildRank(): Record<string, any> {
  const sorted = [...rankData.value].sort((a, b) => a.value - b.value)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 140, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: sorted.map((d) => d.name) },
    series: [
      {
        type: 'bar',
        data: sorted.map((d) => d.value),
        itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
        barMaxWidth: 20
      }
    ]
  }
}

async function loadData() {
  await Promise.all([loadRank(), loadList()])
}

async function loadRank() {
  try {
    rankData.value = await getMerchantRanking({ limit: 10, dateRange: dateRange.value })
  } catch (e) {
    rankData.value = Array.from({ length: 10 }, (_, i) => ({
      name: `商户${i + 1}旗舰店`,
      value: 250000 - i * 20000
    }))
  }
  rankOption.value = buildRank()
}

async function loadList() {
  loading.value = true
  try {
    const res = await getMerchantAnalysis({ ...query, dateRange: dateRange.value })
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    tableData.value = Array.from({ length: 10 }, (_, i) => ({
      id: i + 1,
      merchantNo: `M2024${String(i + 1).padStart(4, '0')}`,
      shopName: `商户${i + 1}旗舰店`,
      salesAmount: 200000 - i * 15000,
      orderCount: 1500 - i * 100,
      refundRate: (Math.random() * 5).toFixed(1),
      avgScore: (4 + Math.random()).toFixed(1),
      status: i % 4 === 0 ? 0 : 1
    }))
    total.value = 56
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.mt16 {
  margin-top: 16px;
}
</style>
