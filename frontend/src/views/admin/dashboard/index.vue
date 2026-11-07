<template>
  <div class="dashboard">
    <!-- 指标卡片 -->
    <el-row :gutter="16">
      <el-col v-for="m in metrics" :key="m.label" :xs="12" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">{{ m.label }}</div>
          <div class="stat-value">{{ formatNumber(m.value) }}</div>
          <div class="stat-trend" :class="m.trend >= 0 ? 'up' : 'down'">
            较上期
            <el-icon v-if="m.trend >= 0"><Top /></el-icon>
            <el-icon v-else><Bottom /></el-icon>
            {{ Math.abs(m.trend) }}%
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <template #header>交易趋势</template>
          <EChart :option="trendOption" height="340px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>品类销售分布</template>
          <EChart :option="pieOption" height="340px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 商户排行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>商户经营排行 TOP10</template>
          <EChart :option="rankOption" height="320px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardMetrics, getTradeTrend, getCategorySalesPie, getMerchantRanking } from '@/api/admin/report'
import type { StatMetric, ChartPoint } from '@/types'
import EChart from '@/components/common/EChart.vue'

const metrics = ref<StatMetric[]>([])
const trendData = ref<ChartPoint[]>([])
const pieData = ref<ChartPoint[]>([])
const rankData = ref<ChartPoint[]>([])

const trendOption = ref<Record<string, any>>({})
const pieOption = ref<Record<string, any>>({})
const rankOption = ref<Record<string, any>>({})

function formatNumber(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(2) + ' 万'
  return n.toLocaleString()
}

function buildTrendOption(): Record<string, any> {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: trendData.value.map((d) => d.name),
      boundaryGap: false
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '交易额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        data: trendData.value.map((d) => d.value),
        itemStyle: { color: '#409eff' }
      }
    ]
  }
}

function buildPieOption(): Record<string, any> {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        name: '品类',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        data: pieData.value.map((d) => ({ name: d.name, value: d.value })),
        label: { formatter: '{b}\n{d}%' }
      }
    ]
  }
}

function buildRankOption(): Record<string, any> {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 120, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: rankData.value.map((d) => d.name).reverse()
    },
    series: [
      {
        name: '销售额',
        type: 'bar',
        data: rankData.value.map((d) => d.value).reverse(),
        itemStyle: {
          color: '#409eff',
          borderRadius: [0, 4, 4, 0]
        },
        barMaxWidth: 20
      }
    ]
  }
}

async function loadData() {
  try {
    const [m, trend, pie, rank] = await Promise.all([
      getDashboardMetrics(),
      getTradeTrend({ type: 'week' }),
      getCategorySalesPie(),
      getMerchantRanking({ limit: 10 })
    ])
    metrics.value = m || []
    trendData.value = trend || []
    pieData.value = pie || []
    rankData.value = rank || []

    trendOption.value = buildTrendOption()
    pieOption.value = buildPieOption()
    rankOption.value = buildRankOption()
  } catch (e) {
    // 使用演示数据兜底
    loadMockData()
  }
}

// 后端未就绪时的演示数据
function loadMockData() {
  metrics.value = [
    { label: '今日交易额', value: 865420, trend: 12.5 },
    { label: '今日订单数', value: 3286, trend: 8.2 },
    { label: '入驻商户数', value: 1256, trend: 3.1 },
    { label: '待处理退款', value: 18, trend: -5.4 }
  ]
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  trendData.value = days.map((d, i) => ({ name: d, value: 50000 + Math.round(Math.random() * 60000) }))
  pieData.value = [
    { name: '数码家电', value: 320 },
    { name: '服饰鞋包', value: 280 },
    { name: '食品生鲜', value: 190 },
    { name: '美妆个护', value: 150 },
    { name: '家居日用', value: 120 }
  ]
  rankData.value = Array.from({ length: 10 }, (_, i) => ({
    name: `商户${i + 1}旗舰店`,
    value: 200000 - i * 15000
  }))
  trendOption.value = buildTrendOption()
  pieOption.value = buildPieOption()
  rankOption.value = buildRankOption()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.dashboard {
  .chart-row {
    margin-top: 16px;
  }
}

:deep(.stat-card) {
  .stat-value {
    font-size: 26px;
    font-weight: 600;
  }
  .stat-trend {
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 2px;
    margin-top: 8px;

    &.up {
      color: #f56c6c;
    }
    &.down {
      color: #67c23a;
    }
  }
}
</style>
