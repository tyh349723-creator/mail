<template>
  <div class="page">
    <div class="navbar"><span class="back" @click="$router.back()">‹</span><span class="title">{{ isEdit ? '编辑商品' : '发布商品' }}</span></div>

    <div class="card form">
      <div class="row"><label>商品标题</label><input v-model="form.title" placeholder="商品标题" /></div>
      <div class="row"><label>副标题</label><input v-model="form.subTitle" placeholder="副标题（可选）" /></div>
      <div class="row"><label>商品类目</label>
        <select v-model="form.categoryId">
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div class="row"><label>价格</label><input v-model="form.price" type="number" placeholder="0.00" /></div>
      <div class="row"><label>原价</label><input v-model="form.originalPrice" type="number" placeholder="0.00" /></div>
      <div class="row"><label>主图</label><input v-model="form.mainImage" placeholder="图片 URL，留空使用默认图" /></div>
      <div class="row"><label>推荐</label><input type="checkbox" v-model="form.isRecommend" style="width:auto" /> 首页推荐位</div>
    </div>

    <div class="card-section">商品规格（SKU）</div>
    <div class="card">
      <div v-for="(s, i) in form.skus" :key="i" class="sku">
        <input v-model="s.specName" placeholder="规格名，如：容量" />
        <input v-model="s.specValue" placeholder="规格值，如：256G" />
        <input v-model="s.price" type="number" placeholder="价格" />
        <input v-model="s.stock" type="number" placeholder="库存" />
        <span class="del" @click="form.skus.splice(i, 1)">✕</span>
      </div>
      <div class="add-sku" @click="form.skus.push({ specName: '', specValue: '', price: form.price || 0, stock: 100 })">＋ 添加规格</div>
    </div>

    <!-- 库存设置：编辑已有商品时按 SKU 实时更新库存 -->
    <template v-if="isEdit && hasExistingSku">
      <div class="card-section">库存设置（修改后点击更新，立即生效）</div>
      <div class="card">
        <template v-for="(s, i) in form.skus" :key="s.id || i">
          <div v-if="s.id" class="stock-item">
            <span class="stock-name">{{ s.specValue || s.specName || '默认规格' }}</span>
            <div class="stock-stepper">
              <button @click="stepStock(s, -1)">−</button>
              <input v-model.number="s.stock" type="number" min="0" />
              <button @click="stepStock(s, 1)">＋</button>
            </div>
            <button class="stock-update" :disabled="stockSaving === s.id" @click="saveStock(s)">
              {{ stockSaving === s.id ? '保存中' : '更新' }}
            </button>
          </div>
        </template>
      </div>
    </template>

    <div class="card textarea-card">
      <div class="lab">商品详情</div>
      <textarea v-model="form.detail" rows="4" placeholder="商品详情描述"></textarea>
    </div>

    <div class="btn-wrap"><button class="btn-primary" :disabled="loading" @click="save">{{ loading ? '保存中...' : '保 存' }}</button></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { mchProductCreate, mchProductUpdate, mchProductStock, productDetail, categoryTree } from '@/api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const stockSaving = ref<any>(null)
const categories = ref<any[]>([])
const form = ref({ title: '', subTitle: '', categoryId: '', price: '', originalPrice: '', mainImage: '', detail: '', isRecommend: 0, skus: [{ specName: '', specValue: '', price: '', stock: '' }] as Array<{ id?: any; specName: string; specValue: string; price: string | number; stock: string | number }> })
const hasExistingSku = computed(() => form.value.skus.some((s) => s.id))

onMounted(async () => {
  // 类目下拉：使用一级类目（保持简单）
  const catRes = await categoryTree()
  const flatten = (tree: any) => {
    const out = []
    for (const c of tree) {
      out.push(c)
      if (c.children) flatten(c.children).forEach((x) => out.push(x))
    }
    return out
  }
  categories.value = flatten(catRes.tree || [])
  if (categories.value.length) form.value.categoryId = categories.value[0].id

  if (isEdit.value) {
    const res = await productDetail(String(route.params.id))
    const p = res
    form.value.title = p.title || ''
    form.value.subTitle = p.subTitle || ''
    if (p.categoryId) form.value.categoryId = p.categoryId
    form.value.price = p.price
    form.value.originalPrice = p.originalPrice ?? p.price
    form.value.mainImage = p.mainImage || ''
    form.value.detail = p.detail || ''
    form.value.isRecommend = p.isRecommend || 0
    if (p.skus && p.skus.length) {
      form.value.skus = p.skus.map((s: any) => ({ specName: s.specName, specValue: s.specValue, price: s.price, stock: s.stock, id: s.id }))
    }
  }
})

/** 库存加减（仅调整页面输入值，点击“更新”后落库） */
function stepStock(s: any, delta: number) {
  const v = Number(s.stock) || 0
  s.stock = Math.max(0, v + delta)
}

/** 实时更新单个 SKU 库存到数据库 */
async function saveStock(s: any) {
  const stock = Number(s.stock)
  if (!Number.isFinite(stock) || stock < 0) {
    ElMessage.error('请输入正确的库存量')
    return
  }
  stockSaving.value = s.id
  try {
    await mchProductStock({ skuId: s.id, stock })
    ElMessage.success('库存已更新')
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    stockSaving.value = null
  }
}

async function save() {
  const f = form.value
  const skus = f.skus
    .filter((s) => s.specValue !== '' || s.specName !== '')
    .map(({ id, specName, specValue, price, stock }) => ({ specName, specValue, price: Number(price), stock: Number(stock) }))
  if (!f.title || !f.price) return alert('请填写商品标题与价格')
  if (!skus.length) return alert('至少填一个商品规格')
  const payload: any = { ...f, price: Number(f.price), originalPrice: Number(f.originalPrice), isRecommend: f.isRecommend ? 1 : 0, skus }
  if (isEdit.value) payload.id = route.params.id
  loading.value = true
  try {
    if (isEdit.value) {
      await mchProductUpdate(payload)
    } else {
      await mchProductCreate(payload)
    }
    alert('保存成功')
    router.replace('/mch/products')
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
.row label { width: 80px; color: #666; font-size: 14px; flex-shrink: 0; }
.row input, .row select { flex: 1; border: none; outline: none; font-size: 14px; background: transparent; }
.row select { color: #333; padding: 6px 0; }
.card-section { color: #666; font-size: 13px; padding: 14px 16px 0 16px; }
.sku { display: flex; gap: 6px; padding: 8px 0; align-items: center; }
.sku input { flex: 1; min-width: 0; padding: 8px; border: 1px solid #eee; border-radius: 8px; font-size: 12px; background: #fafafa; }
.sku .del { color: #ff3e3e; padding: 0 4px; }
.add-sku { text-align: center; color: var(--primary); font-size: 13px; padding: 8px 0 4px; }
.stock-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.stock-item:last-child { border-bottom: none; }
.stock-name { flex: 1; min-width: 0; font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stock-stepper { display: flex; align-items: center; gap: 6px; }
.stock-stepper button { width: 26px; height: 26px; border-radius: 6px; background: #f2f3f5; font-size: 14px; }
.stock-stepper input { width: 64px; padding: 6px 8px; border: 1px solid #eee; border-radius: 8px; font-size: 13px; text-align: center; }
.stock-update { padding: 6px 12px; border-radius: 8px; background: var(--primary); color: #fff; font-size: 12px; }
.stock-update:disabled { opacity: 0.6; }
.textarea-card { padding: 10px 16px; }
.lab { color: #666; font-size: 13px; padding: 4px 0 8px; }
textarea { width: 100%; border: 1px solid #f0f0f0; border-radius: 8px; font-size: 13px; padding: 10px; }
</style>