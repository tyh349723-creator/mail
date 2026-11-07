<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <div class="search-bar">
      <el-radio-group v-model="trendType" @change="loadTrend">
        <el-radio-button label="day">按日</el-radio-button>
        <el-radio-button label="week">按周</el-radio-button>
        <el-radio-button label="month">按月</el-radio-button>
      </el-radio-group>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="loadAll"
      />
      <el-button type="primary" :icon="Search" @click="loadAll">查询</el-button>
    </div>

    <!-- 指标 -->
    <el-row :gutter="16">
      <el-col v-for="m in metrics" :key="m.label" :xs="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">{{ m.label }}</div>
          <div class="stat-value">{{ m.value.toLocaleString() }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>交易金额趋势</template>
          <EChart :option="trendOption" height="360px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>品类销售占比</template>
          <EChart :option="pieOption" height="340px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>支付渠道占比</template>
          <EChart :option="channelOption" height="340px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getDashboardMetrics, getTradeTrend, getCategorySalesPie } from '@/api/admin/report'
import type { StatMetric, ChartPoint } from '@/types'
import EChart from '@/components/common/EChart.vue'

const trendType = ref<'day' | 'week' | 'month'>('week')
const dateRange = ref<string[]>([])
const metrics = ref<StatMetric[]>([])
const trendData = ref<ChartPoint[]>([])
const pieData = ref<ChartPoint[]>([])
const channelData = ref<ChartPoint[]>([])

const trendOption = ref<Record<string, any>>({})
const pieOption = ref<Record<string, any>>({})
const channelOption = ref<Record<string, any>>({})

function buildTrend(): Record<string, any> {
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['交易额', '订单量'] },
    grid: { left: 70, right: 50, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: trendData.value.map((d) => d.name), boundaryGap: false },
    yAxis: [
      { type: 'value', name: '交易额' },
      { type: 'value', name: '订单量' }
    ],
    series: [
      {
        name: '交易额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.12 },
        data: trendData.value.map((d) => d.value),
        itemStyle: { color: '#409eff' }
      },
      {
        name: '订单量',
        type: 'bar',
        yAxisIndex: 1,
        data: trendData.value.map((d) => Math.round(d.value / 100)),
        itemStyle: { color: '#67c23a' },
        barMaxWidth: 24
      }
    ]
  }
}

function buildPie(): Record<string, any> {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        data: pieData.value.map((d) => ({ name: d.name, value: d.value }))
      }
    ]
  }
}

function buildChannel(): Record<string, any> {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: '65%',
        roseType: 'radius',
        data: channelData.value.map((d) => ({ name: d.name, value: d.value }))
      }
    ]
  }
}

async function loadTrend() {
  try {
    trendData.value = await getTradeTrend({ type: trendType.value, dateRange: dateRange.value })
  } catch (e) {
    trendData.value = genTrend()
  }
  trendOption.value = buildTrend()
}

async function loadAll() {
  await Promise.all([loadMetrics(), loadTrend(), loadChannel()])
}

async function loadChannel() {
  try {
    channelData.value = await getCategorySalesPie({ dateRange: dateRange.value })
  } catch (e) {
    channelData.value = []
  }
  channelOption.value = buildChannel()
}

async function loadMetrics() {
  try {
    metrics.value = await getDashboardMetrics({ dateRange: dateRange.value })
  } catch (e) {
    metrics.value = [
      { label: '交易总额', value: 8654200, trend: 10.5 },
      { label: '订单总量', value: 32860, trend: 8.2 },
      { label: '客单价', value: 263, trend: 2.1 },
      { label: '退款金额', value: 124800, trend: -3.4 }
    ]
  }
}

function genTrend(): ChartPoint[] {
  const arr: ChartPoint[] = []
  const n = trendType.value === 'day' ? 30 : trendType.value === 'week' ? 12 : 12
  for (let i = n; i > 0; i--) {
    arr.push({
      name: trendType.value === 'day' ? `D-${i}` : trendType.value === 'week' ? `第${i}周` : `${i}月`,
      value: 100000 + Math.round(Math.random() * 200000)
    })
  }
  return arr
}

onMounted(async () => {
  pieData.value = [
    { name: '数码家电', value: 3200 },
    { name: '服饰鞋包', value: 2800 },
    { name: '食品生鲜', value: 1900 },
    { name: '美妆个护', value: 1500 }
  ]
  pieOption.value = buildPie()
  channelOption.value = buildChannel()
  await loadAll()
})
</script>

<style scoped lang="scss">
.mt16 {
  margin-top: 16px;
}
</style>
