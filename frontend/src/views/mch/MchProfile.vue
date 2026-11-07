<template>
  <div class="page">
    <!-- 店铺信息卡 -->
    <div class="mch-head">
      <div class="m-ic">🏪</div>
      <div class="m-info">
        <div class="t">{{ user.shopName || '我的店铺' }}</div>
        <div class="s">{{ stateText }} · {{ user.userNum }}</div>
      </div>
    </div>

    <!-- 未入驻 / 审核中 / 已驳回：提示去入驻流程 -->
    <div v-if="state !== 1" class="banner" @click="$router.push('/mch/apply-status')">
      <span>店铺尚未开始经营，查看入驻进度 ›</span>
    </div>

    <!-- 经营指标概览（复用经营统计接口） -->
    <template v-if="state === 1">
      <div class="section-title">📈 经营概览</div>
      <div class="metric-grid">
        <div class="metric">
          <div class="v money">¥{{ fmtMoney(s.totalMoney) }}</div>
          <div class="k">交易额（{{ s.totalCount }} 笔）</div>
        </div>
        <div class="metric">
          <div class="v net">¥{{ fmtMoney(s.netMoney) }}</div>
          <div class="k">净收益</div>
        </div>
        <div class="metric">
          <div class="v fee">¥{{ fmtMoney(s.feeMoney) }}</div>
          <div class="k">累计手续费</div>
        </div>
        <div class="metric">
          <div class="v">¥{{ fmtMoney(s.refundMoney) }}</div>
          <div class="k">已退款金额</div>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="section-title">⚡ 快捷入口</div>
      <div class="quick-grid">
        <div class="q-item" @click="$router.push('/mch/product-edit')"><span class="qi"><img src="@/assets/submit.png" style="width:28px;height:28px;margin-left:40px;margin-top:2px" /></span><span>发布商品</span></div>
        <div class="q-item" @click="$router.push('/mch/orders')"><span class="qi"><img src="@/assets/dingdang.png" style="width:28px;height:28px;margin-left:40px;margin-top:2px" /></span><span>订单处理</span></div>
        <div class="q-item" @click="$router.push('/mch/wallet')"><span class="qi"><img src="@/assets/packege.png" style="width:28px;height:28px;margin-left:40px;margin-top:2px" /></span><span>钱包提现</span></div>
        <div class="q-item" @click="$router.push('/mch/stats')"><span class="qi"><img src="@/assets/jingying.png" style="width:28px;height:28px;margin-left:40px;margin-top:2px" /></span><span>经营统计</span></div>
      </div>
    </template>

    <div class="card menu">
      <div class="cell" @click="switchUser"><span class="ic"><img src="@/assets/bug.png" style="width:28px;height:28px;margin-left:0px;margin-top:2px" /></span>切换至买家端<span class="arrow">›</span></div>
      <div class="cell logout" @click="logout">退出登录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mchApplyStatus, statisMch } from '@/api'
import { useUserStore } from '@/store/user'
import { fmtMoney } from '@/utils/const'

const router = useRouter()
const user = useUserStore()
const state = ref<number>(user.mchState)
const stateText = computed(() => ({ '-1': '未入驻', 0: '审核中', 1: '经营中', 2: '已驳回', 3: '已禁用' }[state.value] || ''))

const s = reactive({ totalMoney: 0, totalCount: 0, feeMoney: 0, netMoney: 0, refundMoney: 0 })

onMounted(async () => {
  try {
    const res = await mchApplyStatus()
    state.value = res.shopState
    user.setMerchantInfo(res.shop?.shopName, res.shopState)
  } catch (e) { /* 未登录由网关 401 兜底 */ }

  if (state.value === 1) {
    try {
      Object.assign(s, await statisMch({}))
    } catch (e) { /* 统计数据拉取失败静默 */ }
  }
})

function switchUser() {
  user.logout()
  router.push({ path: '/', query: { role: 'user' } })
}
function logout() {
  user.logout()
  router.replace('/')
}
</script>

<style scoped>
.mch-head { background: linear-gradient(135deg, #3b7cff, #8a5cff); color: #fff; padding: 26px 20px; display: flex; align-items: center; gap: 14px; }
.m-ic { font-size: 44px; }
.m-info { min-width: 0; }
.mch-head .t { font-size: 19px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mch-head .s { font-size: 12px; opacity: .9; margin-top: 4px; }
.banner { margin: 12px; background: #fff7e6; color: #fa8c16; border-radius: 12px; padding: 14px 16px; font-size: 13px; }

.metric-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; padding: 0 12px; }
.metric { background: #fff; border-radius: 12px; padding: 16px; }
.v { font-size: 19px; font-weight: 700; }
.v.money { color: #3b7cff; }
.v.fee { color: #fa8c16; }
.v.net { color: #00b578; }
.k { color: #999; font-size: 12px; margin-top: 4px; }

.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; padding: 0 12px; }
.q-item { background: #fff; border-radius: 12px; padding: 14px 0 12px; text-align: center; font-size: 12px; color: #333; }
.qi { display: block; font-size: 22px; margin-bottom: 6px; }

.menu { margin: 14px 12px; }
.menu .cell { border-radius: 12px; margin-bottom: 10px; padding: 14px 16px; display: flex; align-items: center; }
.menu .cell .ic { font-size: 18px; margin: 0 10px 0 0; }
.menu .cell .arrow { margin-left: auto; color: #ccc; }
.logout { color: #ff3e3e; justify-content: center; border: none; }
</style>