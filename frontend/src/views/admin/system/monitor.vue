<template>
  <div class="app-container">
    <div class="stat-bar">
      <div class="stat"><div class="v">{{ fmtUptime(m.uptimeSeconds) }}</div><div class="k">运行时长</div></div>
      <div class="stat"><div class="v sm">{{ m.startTime || '-' }}</div><div class="k">启动时间</div></div>
      <div class="stat"><div class="v">{{ m.threads }}</div><div class="k">活跃线程</div></div>
      <div class="stat"><div class="v">{{ m.totalCalls }}</div><div class="k">接口调用（累计）</div></div>
      <div class="stat"><div class="v">{{ heapPct }}%</div><div class="k">JVM 堆使用率</div></div>
    </div>

    <div class="table-card">
      <div class="card-head">
        <span class="title">JVM 内存</span>
        <span class="hint">{{ m.jvm?.heapUsedMb }} MB / {{ m.jvm?.heapTotalMb }} MB（最大 {{ m.jvm?.heapMaxMb }} MB）</span>
      </div>
      <el-progress
        :percentage="heapPct"
        :stroke-width="14"
        :color="heapColor"
        :format="() => `${heapPct}%`"
      />
    </div>

    <div class="table-card">
      <div class="card-head">
        <span class="title">接口调用 Top 10</span>
        <div class="actions">
          <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
          <el-checkbox v-model="autoFresh" @change="toggleAuto">10 秒自动刷新</el-checkbox>
        </div>
      </div>
      <el-table :data="topList" stripe border v-loading="loading" size="small">
        <el-table-column prop="path" label="接口路径" min-width="220" />
        <el-table-column label="调用次数" width="140" align="right">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.count }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getMonitorStatus } from '@/api/admin/system'

const m = reactive({ uptimeSeconds: 0, startTime: '', threads: 0, totalCalls: 0, jvm: { heapUsedMb: 0, heapTotalMb: 0, heapMaxMb: 0 }, top: [] as Array<{ path: string; count: number }> })
const loading = ref(false)
const autoFresh = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const topList = computed(() => m.top || [])
const heapPct = computed(() => {
  const max = Number(m.jvm?.heapMaxMb) || 1
  return Math.min(100, Math.round(((Number(m.jvm?.heapUsedMb) || 0) / max) * 100))
})
const heapColor = computed(() => (heapPct.value > 80 ? '#f56c6c' : heapPct.value > 60 ? '#e6a23c' : '#67c23a'))

function fmtUptime(sec?: number) {
  const s = Number(sec || 0)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const mi = Math.floor((s % 3600) / 60)
  const ss = s % 60
  if (d > 0) return `${d}天${h}小时`
  if (h > 0) return `${h}小时${mi}分`
  if (mi > 0) return `${mi}分${ss}秒`
  return `${ss}秒`
}

async function load() {
  loading.value = true
  try {
    Object.assign(m, await getMonitorStatus())
  } finally {
    loading.value = false
  }
}

function toggleAuto(on: boolean) {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  if (on) timer = setInterval(load, 10000)
}

onMounted(load)
onBeforeUnmount(() => timer && clearInterval(timer))
</script>

<style scoped>
.stat-bar { display: flex; gap: 12px; padding: 16px; margin-bottom: 12px; background: #fff; border-radius: 8px; border: 1px solid #ebeef5; }
.stat { flex: 1; text-align: center; }
.stat .v { font-size: 22px; font-weight: 700; color: #303133; }
.stat .v.sm { font-size: 14px; line-height: 32px; }
.stat .k { font-size: 12px; color: #909399; margin-top: 4px; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-head .title { font-weight: 600; }
.card-head .hint { color: #909399; font-size: 12px; }
.actions { display: flex; align-items: center; gap: 10px; }
</style>