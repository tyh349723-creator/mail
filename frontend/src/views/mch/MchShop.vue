<template>
  <div class="page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">店铺信息</span></div>

    <div class="card form">
      <div class="row"><label>店铺编号</label><em class="ro">{{ shop.shopNum }}</em></div>
      <div class="row"><label>店铺名称</label><input v-model="form.shopName" /></div>
      <div class="row"><label>联系电话</label><input v-model="form.shopPhone" /></div>
      <div class="row"><label>经营地址</label><input v-model="form.shopAddress" /></div>
      <div class="row"><label>联系人</label><input v-model="form.contactName" /></div>
      <div class="row"><label>经营类目</label><input v-model="form.categoryName" placeholder="如：食品生鲜" /></div>
      <div class="row"><label>修改密码</label><input v-model="form.password" type="password" placeholder="留空则不修改" /></div>
    </div>

    <div class="btn-wrap"><button class="btn-primary" :disabled="loading" @click="save">{{ loading ? '保存中...' : '保存' }}</button></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mchShopInfo, mchShopUpdate } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()
const shop = ref<any>({})
const loading = ref(false)
const form = ref({ shopName: '', shopPhone: '', shopAddress: '', contactName: '', categoryName: '', password: '' })

onMounted(async () => {
  const res = await mchShopInfo()
  shop.value = res.shop || {}
  form.value.shopName = shop.value.shopName
  form.value.shopPhone = shop.value.shopPhone
  form.value.shopAddress = shop.value.shopAddress
  form.value.contactName = shop.value.contactName
  form.value.categoryName = shop.value.categoryName || ''
})

async function save() {
  if (!form.value.shopName || !form.value.shopPhone) return alert('请填写店铺名称与联系电话')
  loading.value = true
  try {
    const res = await mchShopUpdate(form.value)
    user.setMerchantInfo(res.shop?.shopName || form.value.shopName, user.mchState)
    alert('保存成功')
    router.back()
  } catch (e: any) {
    alert(e.message || '保存失败')
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
.ro { font-style: normal; color: #bbb; }
</style>