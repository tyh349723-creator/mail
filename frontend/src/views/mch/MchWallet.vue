<template>
  <div class="page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">钱包提现</span></div>

    <!-- 余额卡 -->
    <div class="balance-card">
      <div class="b1">可提现余额（元）</div>
      <div class="b2">¥{{ fmtMoney(w.balance) }}</div>
      <div class="b3">
        <span>累计入账 ¥{{ fmtMoney(w.totalIncome) }}</span>
        <span>累计手续费 ¥{{ fmtMoney(w.totalFee) }}</span>
      </div>
      <button class="b-btn" @click="withdraw">提 现</button>
    </div>

    <div class="card tab-switch">
      <span :class="{ active: tab === 'records' }" @click="switchTab('records')">资金流水</span>
      <span :class="{ active: tab === 'withdraws' }" @click="switchTab('withdraws')">提现记录</span>
    </div>

    <!-- 资金流水 -->
    <div v-if="tab === 'records'">
      <!-- 手续费明细汇总（营收 − 手续费 = 实收） -->
      <div class="card fee-summary">
        <div class="fs-item"><div class="fs-v">¥{{ fmtMoney(total.moneyTotal) }}</div><div class="fs-k">累计营收</div></div>
        <div class="fs-item"><div class="fs-v fee">- ¥{{ fmtMoney(total.feeTotal) }}</div><div class="fs-k">累计手续费</div></div>
        <div class="fs-item"><div class="fs-v net">¥{{ fmtMoney(netTotal) }}</div><div class="fs-k">实收</div></div>
      </div>

      <div v-for="r in records" :key="r.id" class="card rec">
        <div class="r1">
          <span class="tt" :class="r.tradeType">{{ r.tradeTypeText }}</span>
          <em :class="moneyClass(r.tradeType)">{{ moneyClass(r.tradeType) === 'in' ? '+' : '-' }}¥{{ fmtMoney(r.money) }}</em>
        </div>
        <div class="r2"><span>{{ r.remark }}</span><em>{{ r.createTime }}</em></div>
        <div class="r3" v-if="r.tradeType === 'pay_in' && Number(r.fee) > 0">
          <span>手续费 ¥{{ fmtMoney(r.fee) }}</span><span>实收 ¥{{ fmtMoney(Number(r.money) - Number(r.fee)) }}</span>
        </div>
      </div>
      <div v-if="!records.length" class="empty">暂无流水</div>
    </div>

    <!-- 提现记录 -->
    <div v-else>
      <div v-for="w in withdraws" :key="w.id" class="card rec">
        <div class="r1">
          <span>{{ w.withdrawNo }}</span>
          <span class="wst" :class="'s' + w.status">{{ w.statusText }}</span>
        </div>
        <div class="r2"><span>提现 ¥{{ fmtMoney(w.money) }} · {{ w.bankName }} {{ w.cardNo }}</span><em>{{ w.createTime }}</em></div>
      </div>
      <div v-if="!withdraws.length" class="empty">暂无提现记录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { walletBalance, walletRecords, walletWithdrawApply, walletWithdrawList } from '@/api'
import { fmtMoney } from '@/utils/const'

const w = ref({ balance: '0', totalIncome: '0', totalFee: '0' })
const tab = ref('records')
const records = ref<any[]>([])
const withdraws = ref<any[]>([])
/** 流水汇总（营收 / 手续费，来自 wallet/records 返回） */
const total = ref({ moneyTotal: '0', feeTotal: '0' })
const netTotal = computed(() => Number(total.value.moneyTotal) - Number(total.value.feeTotal))

onMounted(load)
async function load() {
  const b = await walletBalance()
  w.value = b
  tab.value = 'records'
  await loadRecords()
}
async function loadRecords() {
  const res = await walletRecords({ pageNum: 1, pageSize: 100 })
  records.value = res.records || []
  total.value = { moneyTotal: res.moneyTotal || 0, feeTotal: res.feeTotal || 0 }
}
async function loadWithdraws() {
  const res = await walletWithdrawList({ pageNum: 1, pageSize: 100 })
  withdraws.value = res.records || []
}
function switchTab(t: any) {
  tab.value = t
  if (t === 'records') loadRecords()
  else loadWithdraws()
}
function moneyClass(type: any) {
  return (type === 'pay_in' || type === 'withdraw_back') ? 'in' : 'out'
}
async function withdraw() {
  const money = prompt('提现金额（元）：', '')
  if (!money) return
  const bank = prompt('开户银行：', '招商银行')
  if (bank === null) return
  const card = prompt('收款卡号：', '')
  if (!card) return
  try {
    await walletWithdrawApply({ money: Number(money), bankName: bank, cardNo: card })
    alert('提现申请已提交，等待平台打款')
    load()
  } catch (e: any) {
    alert(e.message || '提现失败')
  }
}
</script>

<style scoped>
.balance-card { background: linear-gradient(135deg, #3b7cff, #8a5cff); color: #fff; margin: 12px; border-radius: 16px; padding: 22px 20px; position: relative; }
.b1 { font-size: 13px; opacity: .9; }
.b2 { font-size: 34px; font-weight: 700; margin: 6px 0 10px; }
.b3 { display: flex; gap: 16px; font-size: 12px; opacity: .9; }
.b-btn { position: absolute; right: 20px; top: 24px; padding: 9px 22px; border: none; border-radius: 18px; background: rgba(255,255,255,.95); color: #3b7cff; font-size: 14px; font-weight: 600; }
.tab-switch { display: flex; margin: 12px; }
.tab-switch span { flex: 1; text-align: center; padding: 10px 0; color: #666; font-size: 14px; }
.tab-switch span.active { color: var(--primary); font-weight: 600; border-bottom: 2px solid var(--primary); }
.rec { padding: 12px 16px; }
.r1 { display: flex; justify-content: space-between; align-items: center; }
.tt { font-size: 14px; font-weight: 600; }
.em { font-style: normal; }
.r1 em { font-style: normal; font-weight: 600; }
.r1 .in { color: #00b578; }
.r1 .out { color: #ff3e3e; }
.r2 { display: flex; justify-content: space-between; color: #999; font-size: 12px; margin-top: 5px; }
.r3 { display: flex; justify-content: space-between; color: #999; font-size: 11px; margin-top: 5px; padding-top: 5px; border-top: 1px dashed #f0f0f0; }
.fee-summary { display: flex; padding: 14px 8px; gap: 4px; }
.fs-item { flex: 1; text-align: center; }
.fs-v { font-size: 15px; font-weight: 700; color: #333; }
.fs-v.fee { color: #fa8c16; }
.fs-v.net { color: #00b578; }
.fs-k { color: #bbb; font-size: 11px; margin-top: 3px; }
.wst { font-size: 12px; padding: 2px 8px; border-radius: 8px; }
.wst.s0 { background: #fff7e6; color: #fa8c16; }
.wst.s1 { background: #e6f7f0; color: #00b578; }
.wst.s2 { background: #fff1f0; color: #ff4d4f; }
</style>