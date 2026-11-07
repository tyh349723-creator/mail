<template>
  <div class="page">
    <div class="navbar">
      <span class="back" @click="$router.back()">‹</span>
      <span class="title">入驻审核</span>
      <span class="nav-logout" @click="logout">退出登录</span>
    </div>

    <div class="status-hero" :class="heroClass">
      <div class="ic">{{ icon }}</div>
      <div class="t">{{ stateText }}</div>
      <div class="s" v-if="state === 0">平台正在审核你的入驻资料，请耐心等待，预计 1-2 个工作日</div>
      <div class="s" v-else-if="state === 2">驳回原因：{{ reason || '资料不完整' }}</div>
      <div class="s" v-else-if="state === 3">店铺已被平台禁用，如有疑问请联系平台客服</div>
    </div>

    <template v-if="state === 0 || state === 2">
      <div class="card-section">已提交资料</div>
      <div class="card info">
        <div class="i-row"><span>店铺名称</span><em>{{ shop.shopName }}</em></div>
        <div class="i-row"><span>联系电话</span><em>{{ shop.shopPhone }}</em></div>
        <div class="i-row"><span>经营地址</span><em>{{ shop.shopAddress }}</em></div>
        <div class="i-row"><span>经营类目</span><em>{{ shop.categoryName || '-' }}</em></div>
        <div class="i-row"><span>提交时间</span><em>{{ shop.createTime }}</em></div>
      </div>
      <div v-if="quals.length" class="card-section">资质文件</div>
      <div class="card">
        <div v-for="q in quals" :key="q.id" class="qual">
          <span>{{ q.name || q.type }}</span>
          <a v-if="q.url" :href="q.url" target="_blank">查看图片 ›</a>
        </div>
        <div v-if="!quals.length" class="dim">未上传资质文件</div>
      </div>
      <div class="btn-wrap" v-if="state === 2">
        <button class="btn-primary" @click="$router.push('/mch/apply')">重新提交资料</button>
      </div>
    </template>

    <div class="btn-wrap" v-if="state === 1">
      <button class="btn-primary" @click="$router.push('/mch/profile')">进入商家工作台</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mchApplyStatus } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()
const state = ref(-1)
const reason = ref('')
const shop = ref<any>({})
const quals = ref<any[]>([])

const heroClass = computed(() => ({ 0: 'c0', 1: 'c1', 2: 'c2', 3: 'c3' }[state.value] || ''))
const stateText = computed(() => ({ 0: '审核中', 1: '审核通过', 2: '已驳回', 3: '已禁用' }[state.value] || '未知状态'))
const icon = computed(() => ({ 0: '⏳', 1: '✅', 2: '⚠️', 3: '🚫' }[state.value] || '❓'))

onMounted(async () => {
  const res = await mchApplyStatus()
  state.value = res.shopState
  reason.value = res.rejectReason || ''
  shop.value = res.shop || {}
  quals.value = res.qualifications || []
  user.setMerchantInfo(res.shop?.shopName, res.shopState)
})

function logout() {
  user.logout()
  router.replace('/')
}
</script>

<style scoped>
.navbar { display: flex; align-items: center; }
.nav-logout { margin-left: auto; font-size: 13px; color: #888; padding: 0 16px 0 8px; }
.status-hero { background: #fff; margin: 12px; border-radius: 14px; padding: 30px 20px; text-align: center; }
.status-hero .ic { font-size: 52px; }
.status-hero .t { font-size: 20px; font-weight: 700; margin-top: 8px; }
.status-hero .s { color: #888; font-size: 13px; margin-top: 8px; line-height: 1.6; }
.status-hero.c0 .t { color: #fa8c16; }
.status-hero.c1 .t { color: #00b578; }
.status-hero.c2 .t { color: #ff3e3e; }
.status-hero.c3 .t { color: #666; }
.card-section { color: #666; font-size: 13px; padding: 14px 16px 0 16px; }
.info { padding: 4px 16px; }
.i-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px dashed #f0f0f0; font-size: 13px; color: #666; }
.i-row em { font-style: normal; color: #333; }
.qual { display: flex; justify-content: space-between; padding: 10px 0; font-size: 13px; color: #333; border-bottom: 1px dashed #f0f0f0; }
.qual a { color: var(--primary); text-decoration: none; }
.dim { color: #bbb; font-size: 12px; padding: 10px 0; }
</style>