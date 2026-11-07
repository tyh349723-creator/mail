<template>
  <div class="page">
    <div class="navbar"><span class="title">我的</span></div>

    <!-- 店铺头卡 -->
    <div class="mine-head">
      <div class="m-ic">🏪</div>
      <div class="m-info">
        <div class="t">{{ user.shopName || '我的店铺' }}</div>
        <div class="s">{{ stateText }}</div>
      </div>
    </div>

    <!-- 功能入口：未通过入驻审核时，经营类功能（钱包/店铺信息）禁用 -->
    <div class="card menu">
      <div class="cell" :class="{ dim: !canBiz }" @click="go('/mch/wallet')"><span class="ic"><img src="@/assets/packege.png" style="width:28px;height:28px;margin-left:0px;margin-top:2px" /></span>钱包提现<span class="arrow">›</span></div>
      <div class="cell" :class="{ dim: !canBiz }" @click="go('/mch/shop')"><span class="ic"><img src="@/assets/dianpu.png" style="width:28px;height:28px;margin-left:0px;margin-top:2px" /></span>店铺信息<span class="arrow">›</span></div>
      <div class="cell" @click="$router.push('/mch/apply-status')"><span class="ic"><img src="@/assets/ruzhu.png" style="width:28px;height:28px;margin-left:0px;margin-top:2px" /></span>入驻状态<span class="arrow">›</span></div>
      <div class="cell" @click="switchUser"><span class="ic"><img src="@/assets/bug.png" style="width:28px;height:28px;margin-left:0px;margin-top:2px" /></span>切换买家端<span class="arrow">›</span></div>
      <div class="cell logout" @click="logout">退出登录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { mchApplyStatus } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()
const state = ref<number>(user.mchState)

const stateText = computed(() => ({ '-1': '未入驻', 0: '审核中', 1: '经营中', 2: '已驳回', 3: '已禁用' }[state.value] || ''))
/** 仅入驻审核通过（状态=1 经营中）可使用经营功能 */
const canBiz = computed(() => state.value === 1)

/** 经营类入口：未通过审核时拦截并提示 */
function go(path: string) {
  if (!canBiz.value) {
    ElMessage.warning('店铺尚未通过入驻审核，暂不可用，请前往「入驻状态」查看进度')
    return
  }
  router.push(path)
}

onMounted(async () => {
  try {
    const res = await mchApplyStatus()
    state.value = res.shopState
    user.setMerchantInfo(res.shop?.shopName, res.shopState)
  } catch (e) {
    /* 未登录由网关 401 兜底 */
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
.mine-head { background: linear-gradient(135deg, #3b7cff, #8a5cff); color: #fff; margin: 12px; border-radius: 16px; padding: 24px 20px; display: flex; align-items: center; gap: 14px; }
.m-ic { font-size: 42px; }
.m-info { min-width: 0; }
.mine-head .t { font-size: 18px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mine-head .s { font-size: 12px; opacity: .9; margin-top: 4px; }

.menu { margin: 12px; }
.menu .cell { border-radius: 12px; margin-bottom: 10px; padding: 14px 16px; display: flex; align-items: center; font-size: 14px; }
.menu .cell .ic { font-size: 18px; margin: 0 10px 0 0; }
.menu .cell .arrow { margin-left: auto; color: #ccc; }
.menu .cell.dim { opacity: .45; }
.logout { color: #ff3e3e; justify-content: center; border: none; }
</style>