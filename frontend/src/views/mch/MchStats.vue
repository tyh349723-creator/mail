<template>
  <div class="page">
    <div class="navbar"><span class="title">经营统计</span></div>

    <!-- 自定义统计时间范围（5.2.5） -->
    <div class="range-bar">
      <el-date-picker
        v-model="range"
        type="daterange"
        size="small"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        :disabled-date="(d: Date) => d.getTime() > Date.now()"
        style="flex: 1"
        @change="onRangeChange"
      />
      <button class="mini" @click="resetRange">重置</button>
    </div>

    <!-- 指标卡 -->
    <div class="metric-grid">
      <div class="metric">
        <div class="v money">¥{{ fmtMoney(s.totalMoney) }}</div>
        <div class="k">交易额（{{ s.totalCount }} 笔）</div>
      </div>
      <div class="metric">
        <div class="v fee">¥{{ fmtMoney(s.feeMoney) }}</div>
        <div class="k">手续费</div>
      </div>
      <div class="metric">
        <div class="v net">¥{{ fmtMoney(s.netMoney) }}</div>
        <div class="k">净收益</div>
      </div>
      <div class="metric">
        <div class="v">¥{{ fmtMoney(s.refundMoney) }}</div>
        <div class="k">已退款金额</div>
      </div>
    </div>

    <!-- 近 7 日趋势 -->
    <div class="card">
      <div class="title">近 7 日交易趋势</div>
      <div class="days">
        <div v-for="d in s.sevenDayData" :key="d.date" class="day">
          <div class="bar" :style="{ height: barHeight(d) }"></div>
          <div class="money-sm" v-if="Number(d.money) > 0">¥{{ fmtMoney(d.money) }}</div>
          <div class="date">{{ d.date.slice(5) }}</div>
        </div>
      </div>
    </div>

    <!-- 渠道占比 -->
    <div class="card">
      <div class="title">支付渠道占比</div>
      <div v-for="c in s.channelRatio" :key="c.wayCode" class="chn">
        <span class="cn">{{ channelText(c.wayCode) }}</span>
        <div class="track"><div class="fill" :style="{ width: c.ratio + '%' }"></div></div>
        <span class="cr">{{ c.ratio }}%</span>
      </div>
      <div v-if="!s.channelRatio || !s.channelRatio.length" class="dim">暂无交易数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { statisMch } from '@/api'
import { fmtMoney } from '@/utils/const'

const s = reactive({ totalMoney: 0, totalCount: 0, feeMoney: 0, netMoney: 0, refundMoney: 0, channelRatio: [] as any[], sevenDayData: [] as any[] })
const range = ref<string[]>([])
const loading = ref(false)

onMounted(async () => {
  await load()
})

/** 按时间范围重查统计（begin/end 为空时返回全部） */
async function load() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (range.value && range.value.length === 2) {
      params.begin = range.value[0]
      params.end = range.value[1]
    }
    const res = await statisMch(params)
    Object.assign(s, res)
  } finally {
    loading.value = false
  }
}

function onRangeChange() {
  load()
}

function resetRange() {
  range.value = []
  load()
}

function barHeight(d: any) {
  const max = Math.max(...(s.sevenDayData || []).map((x) => Number(x.money)), 0.01)
  return Math.max(4, Math.round((Number(d.money) / max) * 70)) + 'px'
}
function channelText(c: string) {
  return ({ WECHAT: '微信支付', ALIPAY: '支付宝', UNIONPAY: '银联支付' } as Record<string, string>)[c] || c
}
</script>

<style scoped>
.range-bar { display: flex; align-items: center; gap: 8px; padding: 10px 12px 0; }
.mini { padding: 6px 14px; border-radius: 14px; border: 1px solid #e5e5e5; background: #fff; color: #666; font-size: 12px; font-weight: 600; }
.metric-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; padding: 12px; }
.metric { background: #fff; border-radius: 12px; padding: 16px; }
.v { font-size: 20px; font-weight: 700; }
.v.money { color: #3b7cff; }
.v.fee { color: #fa8c16; }
.v.net { color: #00b578; }
.k { color: #999; font-size: 12px; margin-top: 4px; }
.card { margin: 0 12px 12px; }
.title { font-size: 14px; font-weight: 600; padding-bottom: 12px; }
.days { display: flex; align-items: flex-end; gap: 6px; height: 120px; }
.day { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; }
.bar { width: 60%; background: linear-gradient(180deg, #5b8cff, #3b7cff); border-radius: 6px 6px 0 0; }
.money-sm { font-size: 10px; color: #666; margin-bottom: 4px; white-space: nowrap; }
.date { font-size: 10px; color: #999; margin-top: 4px; }
.chn { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
.cn { width: 70px; font-size: 13px; color: #333; }
.track { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.fill { height: 100%; background: linear-gradient(90deg, #5b8cff, #3b7cff); border-radius: 4px; }
.cr { width: 50px; text-align: right; font-size: 12px; color: #666; }
.dim { color: #bbb; font-size: 12px; text-align: center; padding: 16px 0; }
</style>