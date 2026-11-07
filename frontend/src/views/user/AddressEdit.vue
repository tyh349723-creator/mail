<template>
  <div class="page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">{{ isEdit ? '编辑地址' : '新增地址' }}</span></div>

    <div class="card form">
      <div class="row"><label>收货人</label><input v-model="form.consignee" placeholder="姓名" /></div>
      <div class="row"><label>手机号</label><input v-model="form.phone" placeholder="手机号" /></div>
      <div class="row"><label>省份</label><input v-model="form.province" placeholder="如：福建省" /></div>
      <div class="row"><label>城市</label><input v-model="form.city" placeholder="如：福州市" /></div>
      <div class="row"><label>区县</label><input v-model="form.district" placeholder="如：台江区" /></div>
      <div class="row"><label>详细地址</label><input v-model="form.detail" placeholder="街道门牌号" /></div>
      <div class="row">
        <label>设为默认</label>
        <input type="checkbox" v-model="form.defaultFlag" style="width:auto" />
      </div>
    </div>

    <div class="btn-wrap"><button class="btn-primary" @click="save">保 存</button></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addressCreate, addressUpdate } from '@/api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const form = ref<any>({ consignee: '', phone: '', province: '', city: '', district: '', detail: '', defaultFlag: false })

onMounted(async () => {
  if (isEdit.value) {
    // 编辑场景从列表页缓存读取（简单演示）
    const cache = JSON.parse(localStorage.getItem('mall_addr_edit') || '{}')
    Object.assign(form.value, cache)
    form.value.id = route.params.id
  }
})

async function save() {
  if (!form.value.consignee || !form.value.phone || !form.value.detail) return alert('请填写收货人、手机号与详细地址')
  try {
    if (isEdit.value) {
      await addressUpdate(form.value)
    } else {
      await addressCreate(form.value)
    }
    alert('保存成功')
    router.back()
  } catch (e: any) {
    alert(e.message || '保存失败')
  }
}
</script>

<style scoped>
.form { padding: 4px 16px; }
.row { display: flex; align-items: center; padding: 13px 0; border-bottom: 1px solid #f5f5f5; }
.row label { width: 80px; color: #666; font-size: 14px; }
.row input { flex: 1; border: none; outline: none; font-size: 14px; }
.checkbox-row { display: flex; }
</style>