<template>
  <div class="page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">入驻资料</span></div>

    <div class="card form">
      <div class="row"><label>店铺名称</label><input v-model="form.shopName" placeholder="店铺名称" /></div>
      <div class="row"><label>联系电话</label><input v-model="form.shopPhone" placeholder="联系电话" /></div>
      <div class="row"><label>经营地址</label><input v-model="form.shopAddress" placeholder="经营地址" /></div>
      <div class="row"><label>联系人</label><input v-model="form.contactName" placeholder="联系人姓名" /></div>
      <div class="row"><label>经营类目</label><input v-model="form.categoryName" placeholder="如：食品生鲜 / 数码家电" /></div>
    </div>

    <div class="card-section">资质文件（营业执照 / 身份证等）</div>
    <div class="card">
      <div v-for="(q, i) in form.qualifications" :key="i" class="qual">
        <input v-model="q.type" class="q-type" placeholder="类型：business_license" />
        <input v-model="q.url" class="q-url" placeholder="图片 URL（演示可填任意链接）" />
        <span class="q-del" @click="form.qualifications.splice(i, 1)">✕</span>
      </div>
      <div class="add-qual" @click="form.qualifications.push({ type: 'business_license', name: '资质文件', url: '' })">＋ 添加资质</div>
    </div>

    <div class="btn-wrap"><button class="btn-primary" :disabled="loading" @click="submit">{{ loading ? '提交中...' : '提交审核' }}</button></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mchApply, mchShopInfo } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const form = ref({ shopName: '', shopPhone: '', shopAddress: '', contactName: '', categoryName: '', qualifications: [] as any[] })

onMounted(async () => {
  try {
    const res = await mchShopInfo()
    const s = res.shop || {}
    form.value.shopName = s.shopName || ''
    form.value.shopPhone = s.shopPhone || ''
    form.value.shopAddress = s.shopAddress || ''
    form.value.contactName = s.contactName || ''
    form.value.categoryName = s.categoryName || ''
  } catch (e) { /* 已注册但无资料时忽略 */ }
})

async function submit() {
  const f = form.value
  if (!f.shopName || !f.shopPhone || !f.shopAddress) return alert('请完整填写店铺资料')
  loading.value = true
  try {
    const res = await mchApply(f)
    user.setMerchantInfo(f.shopName, res.shopState)
    alert('已提交，等待平台审核')
    router.replace('/mch/apply-status')
  } catch (e: any) {
    alert(e.message || '提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form { padding: 4px 16px; }
.row { display: flex; align-items: center; padding: 13px 0; border-bottom: 1px solid #f5f5f5; }
.row label { width: 80px; color: #666; font-size: 14px; }
.row input { flex: 1; border: none; outline: none; font-size: 14px; }
.card-section { color: #666; font-size: 13px; padding: 14px 16px 0 16px; }
.qual { display: flex; gap: 8px; padding: 8px 0; align-items: center; }
.qual .q-type { width: 40%; }
.qual .q-url { flex: 1; }
.qual input { padding: 9px 10px; border: 1px solid #eee; border-radius: 8px; font-size: 12px; background: #fafafa; }
.q-del { color: #ff3e3e; padding: 0 6px; }
.add-qual { text-align: center; color: var(--primary); font-size: 13px; padding: 10px 0 4px; }
</style>